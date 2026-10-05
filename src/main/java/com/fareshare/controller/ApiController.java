package com.fareshare.controller;

import com.fareshare.service.Identity;
import com.fareshare.service.GroupService;
import com.fareshare.service.ExpenseService;
import com.fareshare.service.SettlementService;
import com.fareshare.service.BalanceService;
import com.fareshare.service.InviteService;
import com.fareshare.model.Model;
import com.fareshare.model.CountryCatalog;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class ApiController {
    private final Identity identity;
    private final GroupService groups;
    private final ExpenseService expenses;
    private final SettlementService settlements;
    private final BalanceService balances;
    private final InviteService invites;

    ApiController(Identity identity, GroupService groups, ExpenseService expenses,
                  SettlementService settlements, BalanceService balances, InviteService invites) {
        this.identity = identity; this.groups = groups; this.expenses = expenses;
        this.settlements = settlements; this.balances = balances; this.invites = invites;
    }

    @GetMapping("/me")
    ApiDtos.MemberView me(HttpServletRequest http) {
        Model.User user = identity.current(http);
        return new ApiDtos.MemberView(user.getId(), user.getDisplayName(), user.getEmail());
    }

    @GetMapping("/catalog/countries")
    List<CountryCatalog.Country> countries() {
        return CountryCatalog.COUNTRIES;
    }

    @PostMapping("/groups") @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.GroupView createGroup(@RequestBody ApiDtos.CreateGroup body, HttpServletRequest http) {
        return groups.create(identity.current(http), body);
    }

    @GetMapping("/groups")
    List<ApiDtos.GroupView> listGroups(HttpServletRequest http) {
        return groups.list(identity.current(http));
    }

    @GetMapping("/groups/{groupId}")
    ApiDtos.GroupView group(@PathVariable UUID groupId, HttpServletRequest http) {
        return groups.get(groupId, identity.current(http));
    }

    @PostMapping("/groups/{groupId}/invites") @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.InviteView createInvite(@PathVariable UUID groupId, @RequestBody ApiDtos.CreateInvite body,
                                     HttpServletRequest http) {
        return invites.create(groupId, identity.current(http), body);
    }

    @PostMapping("/invites/accept")
    ApiDtos.GroupView acceptInvite(@RequestBody ApiDtos.AcceptInvite body, HttpServletRequest http) {
        return invites.accept(identity.current(http), body);
    }

    @PostMapping("/groups/{groupId}/expenses") @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.ExpenseView createExpense(@PathVariable UUID groupId, @RequestBody ApiDtos.WriteExpense body,
                                       @RequestHeader("Idempotency-Key") String key, HttpServletRequest http) {
        return expenses.create(groupId, identity.current(http), body, key);
    }

    @GetMapping("/groups/{groupId}/expenses")
    List<ApiDtos.ExpenseView> listExpenses(@PathVariable UUID groupId, HttpServletRequest http) {
        return expenses.list(groupId, identity.current(http));
    }

    @PatchMapping("/groups/{groupId}/expenses/{expenseId}")
    ApiDtos.ExpenseView editExpense(@PathVariable UUID groupId, @PathVariable UUID expenseId,
                                     @RequestBody ApiDtos.EditExpense body, HttpServletRequest http) {
        return expenses.edit(groupId, expenseId, identity.current(http), body);
    }

    @PostMapping("/groups/{groupId}/expenses/{expenseId}/reverse")
    ApiDtos.ExpenseView reverseExpense(@PathVariable UUID groupId, @PathVariable UUID expenseId,
                                        HttpServletRequest http) {
        return expenses.reverse(groupId, expenseId, identity.current(http));
    }

    @GetMapping("/groups/{groupId}/balances")
    ApiDtos.BalanceView balances(@PathVariable UUID groupId, HttpServletRequest http) {
        return balances.balances(groupId, identity.current(http));
    }

    @GetMapping("/groups/{groupId}/activity")
    List<ApiDtos.ActivityView> activity(@PathVariable UUID groupId, HttpServletRequest http) {
        return balances.activity(groupId, identity.current(http));
    }

    @PostMapping("/groups/{groupId}/settlements") @ResponseStatus(HttpStatus.CREATED)
    ApiDtos.SettlementView createSettlement(@PathVariable UUID groupId,
                                             @RequestBody ApiDtos.CreateSettlement body,
                                             @RequestHeader("Idempotency-Key") String key,
                                             HttpServletRequest http) {
        return settlements.create(groupId, identity.current(http), body, key);
    }

    @GetMapping("/groups/{groupId}/settlements")
    List<ApiDtos.SettlementView> settlements(@PathVariable UUID groupId, HttpServletRequest http) {
        return settlements.list(groupId, identity.current(http));
    }

    @PostMapping("/settlements/{settlementId}/confirm")
    ApiDtos.SettlementView confirm(@PathVariable UUID settlementId, HttpServletRequest http) {
        return settlements.decide(settlementId, identity.current(http), true);
    }

    @PostMapping("/settlements/{settlementId}/reject")
    ApiDtos.SettlementView reject(@PathVariable UUID settlementId, HttpServletRequest http) {
        return settlements.decide(settlementId, identity.current(http), false);
    }
}
