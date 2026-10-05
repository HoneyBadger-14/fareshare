package com.fareshare.service;

import com.fareshare.model.Model;
import com.fareshare.controller.ApiDtos;
import com.fareshare.controller.ApiError;
import com.fareshare.repository.Expenses;
import com.fareshare.repository.Shares;
import com.fareshare.repository.Activities;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {
    private final GroupService groups;
    private final Expenses expenses;
    private final Shares shares;
    private final Activities activities;

    public ExpenseService(GroupService groups, Expenses expenses, Shares shares, Activities activities) {
        this.groups = groups; this.expenses = expenses; this.shares = shares; this.activities = activities;
    }

    @Transactional
    public ApiDtos.ExpenseView create(UUID groupId, Model.User actor, ApiDtos.WriteExpense request, String key) {
        groups.requireMember(groupId, actor.getId());
        String requestKey = requireKey(key);
        ValidExpense valid = validate(groupId, request);
        var existing = expenses.findByGroupIdAndRequestKey(groupId, requestKey);
        if (existing.isPresent()) {
            Model.Expense previous = existing.get();
            if (!previous.getCreatorId().equals(actor.getId()) || !previous.getDescription().equals(valid.description())
                    || previous.getAmountMinor() != valid.amountMinor() || !previous.getPayerId().equals(valid.payerId())
                    || !previous.getSpentOn().equals(valid.spentOn())
                    || !shareMap(view(previous).shares()).equals(shareMap(valid.shares()))) {
                throw ApiError.conflict("Idempotency key was used for a different expense");
            }
            return view(previous);
        }
        Model.Expense expense = expenses.save(new Model.Expense(groupId, valid.payerId(), actor.getId(),
                valid.description(), valid.amountMinor(), valid.spentOn(), requestKey));
        for (var share : valid.shares()) shares.save(new Model.Share(expense.getId(), share.userId(), share.amountMinor()));
        activities.save(new Model.Activity(groupId, actor.getId(), "EXPENSE_CREATED", expense.getId(),
                "Amount: " + expense.getAmountMinor() + "; shares: " + valid.shares()));
        return view(expense);
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.ExpenseView> list(UUID groupId, Model.User actor) {
        groups.requireMember(groupId, actor.getId());
        return expenses.findByGroupIdOrderByCreatedAtDesc(groupId).stream().map(this::view).toList();
    }

    @Transactional
    public ApiDtos.ExpenseView edit(UUID groupId, UUID expenseId, Model.User actor, ApiDtos.EditExpense request) {
        groups.requireMember(groupId, actor.getId());
        Model.Expense expense = requireExpense(groupId, expenseId);
        requireCreator(expense, actor);
        if (expense.getReversed()) throw ApiError.conflict("Reversed expense cannot be edited");
        if (request == null || request.expectedVersion() == null || request.expectedVersion() != expense.getVersion()) {
            throw ApiError.conflict("Expense version is stale");
        }
        ValidExpense valid = validate(groupId, new ApiDtos.WriteExpense(request.description(), request.amountMinor(),
                request.payerId(), request.spentOn(), request.splitMethod(), request.participantIds(), request.shares()));
        String before = "Amount: " + expense.getAmountMinor() + "; payer: " + expense.getPayerId()
                + "; shares: " + shares.findByExpenseId(expense.getId()).stream()
                .map(s -> s.getUserId() + "=" + s.getAmountMinor()).toList();
        expense.update(valid.description(), valid.amountMinor(), valid.payerId(), valid.spentOn());
        shares.deleteByExpenseId(expense.getId());
        shares.flush();
        for (var share : valid.shares()) shares.save(new Model.Share(expense.getId(), share.userId(), share.amountMinor()));
        expenses.saveAndFlush(expense);
        activities.save(new Model.Activity(groupId, actor.getId(), "EXPENSE_EDITED", expense.getId(),
                before + " -> amount: " + expense.getAmountMinor() + "; payer: " + expense.getPayerId()
                        + "; shares: " + valid.shares()));
        return view(expense);
    }

    @Transactional
    public ApiDtos.ExpenseView reverse(UUID groupId, UUID expenseId, Model.User actor) {
        groups.requireMember(groupId, actor.getId());
        Model.Expense expense = requireExpense(groupId, expenseId);
        requireCreator(expense, actor);
        if (expense.getReversed()) throw ApiError.conflict("Expense is already reversed");
        expense.reverse();
        expenses.saveAndFlush(expense);
        activities.save(new Model.Activity(groupId, actor.getId(), "EXPENSE_REVERSED", expense.getId(),
                "Reversed expense of " + expense.getAmountMinor()));
        return view(expense);
    }

    private Model.Expense requireExpense(UUID groupId, UUID expenseId) {
        Model.Expense expense = expenses.findById(expenseId).orElseThrow(() -> ApiError.missing("Expense not found"));
        if (!expense.getGroupId().equals(groupId)) throw ApiError.missing("Expense not found");
        return expense;
    }

    private void requireCreator(Model.Expense expense, Model.User actor) {
        if (!expense.getCreatorId().equals(actor.getId())) throw ApiError.forbidden("Only the expense creator may change it");
    }

    private ValidExpense validate(UUID groupId, ApiDtos.WriteExpense request) {
        if (request == null || request.description() == null || request.description().isBlank()
                || request.description().length() > 240 || request.amountMinor() == null
                || request.amountMinor() <= 0 || request.payerId() == null || request.spentOn() == null) {
            throw ApiError.bad("Description, positive amount, payer and date are required");
        }
        if (request.spentOn().isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1))) {
            throw ApiError.bad("Expense date is too far in the future");
        }
        groups.requireMembers(groupId, List.of(request.payerId()));
        List<Accounting.ShareAmount> allocated;
        if ("EQUAL".equalsIgnoreCase(request.splitMethod())) {
            groups.requireMembers(groupId, request.participantIds());
            allocated = Accounting.splitEqually(request.amountMinor(), request.participantIds());
        } else if ("EXACT".equalsIgnoreCase(request.splitMethod())) {
            allocated = request.shares();
            Accounting.validateShares(request.amountMinor(), allocated);
            groups.requireMembers(groupId, allocated.stream().map(Accounting.ShareAmount::userId).toList());
        } else {
            throw ApiError.bad("splitMethod must be EQUAL or EXACT");
        }
        Accounting.validateShares(request.amountMinor(), allocated);
        return new ValidExpense(request.description().trim(), request.amountMinor(), request.payerId(),
                request.spentOn(), allocated);
    }

    private ApiDtos.ExpenseView view(Model.Expense expense) {
        List<Accounting.ShareAmount> allocated = shares.findByExpenseId(expense.getId()).stream()
                .map(s -> new Accounting.ShareAmount(s.getUserId(), s.getAmountMinor())).toList();
        return new ApiDtos.ExpenseView(expense.getId(), expense.getGroupId(), expense.getPayerId(), expense.getCreatorId(),
                expense.getDescription(), expense.getAmountMinor(), expense.getSpentOn(), allocated, expense.getReversed(),
                expense.getVersion(), expense.getCreatedAt());
    }

    static String requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) throw ApiError.bad("Idempotency-Key header is required");
        return key.trim();
    }

    private Map<UUID, Long> shareMap(List<Accounting.ShareAmount> allocated) {
        return allocated.stream().collect(Collectors.toMap(Accounting.ShareAmount::userId,
                Accounting.ShareAmount::amountMinor));
    }

    private record ValidExpense(String description, long amountMinor, UUID payerId, LocalDate spentOn,
                                List<Accounting.ShareAmount> shares) {}
}
