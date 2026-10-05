package com.fareshare.service;

import com.fareshare.model.Model;
import com.fareshare.controller.ApiDtos;
import com.fareshare.repository.Members;
import com.fareshare.repository.Expenses;
import com.fareshare.repository.Shares;
import com.fareshare.repository.Settlements;
import com.fareshare.repository.Activities;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BalanceService {
    private final GroupService groups;
    private final Members members;
    private final Expenses expenses;
    private final Shares shares;
    private final Settlements settlements;
    private final Activities activities;

    public BalanceService(GroupService groups, Members members, Expenses expenses, Shares shares,
                   Settlements settlements, Activities activities) {
        this.groups = groups; this.members = members; this.expenses = expenses;
        this.shares = shares; this.settlements = settlements; this.activities = activities;
    }

    @Transactional(readOnly = true)
    public ApiDtos.BalanceView balances(UUID groupId, Model.User actor) {
        Model.Group group = groups.requireMember(groupId, actor.getId());
        Map<UUID, Long> balances = Accounting.zeroBalances(
                members.findByGroupId(groupId).stream().map(m -> m.getUserId()).toList());
        for (Model.Expense expense : expenses.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            if (expense.getReversed()) continue;
            add(balances, expense.getPayerId(), expense.getAmountMinor());
            for (Model.Share share : shares.findByExpenseId(expense.getId())) {
                add(balances, share.getUserId(), Math.negateExact(share.getAmountMinor()));
            }
        }
        for (Model.Settlement settlement : settlements.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            if (!"CONFIRMED".equals(settlement.getStatus())) continue;
            add(balances, settlement.getSenderId(), settlement.getAmountMinor());
            add(balances, settlement.getRecipientId(), Math.negateExact(settlement.getAmountMinor()));
        }
        return new ApiDtos.BalanceView(groupId, group.getCurrencyCode(), balances, Accounting.suggestions(balances));
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.ActivityView> activity(UUID groupId, Model.User actor) {
        groups.requireMember(groupId, actor.getId());
        return activities.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(e -> new ApiDtos.ActivityView(e.getId(), e.getActorId(), e.getEventType(),
                        e.getSubjectId(), e.getDetail(), e.getCreatedAt())).toList();
    }

    private void add(Map<UUID, Long> balances, UUID userId, long amount) {
        Long current = balances.get(userId);
        if (current == null) throw new IllegalStateException("Ledger references a non-member");
        balances.put(userId, Math.addExact(current, amount));
    }
}
