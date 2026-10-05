package com.fareshare.controller;

import com.fareshare.service.Accounting;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() {}

    public record CreateGroup(String name, String currencyCode) {}
    public record GroupView(UUID id, String name, String currencyCode, List<MemberView> members) {}
    public record MemberView(UUID userId, String displayName, String email) {}
    public record WriteExpense(String description, Long amountMinor, UUID payerId, LocalDate spentOn,
                        String splitMethod, List<UUID> participantIds, List<Accounting.ShareAmount> shares) {}
    public record EditExpense(Long expectedVersion, String description, Long amountMinor, UUID payerId,
                       LocalDate spentOn, String splitMethod, List<UUID> participantIds,
                       List<Accounting.ShareAmount> shares) {}
    public record ExpenseView(UUID id, UUID groupId, UUID payerId, UUID creatorId, String description,
                       long amountMinor, LocalDate spentOn, List<Accounting.ShareAmount> shares,
                       boolean reversed, long version, Instant createdAt) {}
    public record CreateSettlement(UUID senderId, UUID recipientId, Long amountMinor) {}
    public record SettlementView(UUID id, UUID groupId, UUID senderId, UUID recipientId, long amountMinor,
                          String status, Instant createdAt, Instant decidedAt) {}
    public record BalanceView(UUID groupId, String currencyCode, Map<UUID, Long> balances,
                       List<Accounting.Payment> suggestedPayments) {}
    public record ActivityView(UUID id, UUID actorId, String eventType, UUID subjectId,
                        String detail, Instant createdAt) {}
    public record CreateInvite(String email) {}
    public record AcceptInvite(String token) {}
    public record InviteView(UUID id, UUID groupId, String invitedEmail, Instant expiresAt, String localJoinLink) {}
}
