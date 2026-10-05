package com.fareshare.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class Model {
    private Model() {}

    @Entity @Table(name = "app_user")
    public static class User {
        @Id private UUID id;
        @Column(nullable = false) private String email;
        @Column(name = "display_name", nullable = false) private String displayName;
        @Column(name = "created_at", nullable = false) private Instant createdAt;
        protected User() {}
        public User(UUID id, String email, String displayName) {
            this.id = id; this.email = email; this.displayName = displayName; this.createdAt = Instant.now();
        }

        public UUID getId() { return id; }
        public String getEmail() { return email; }
        public String getDisplayName() { return displayName; }
        public Instant getCreatedAt() { return createdAt; }
    }

    @Entity @Table(name = "expense_group")
    public static class Group {
        @Id private UUID id;
        @Column(nullable = false) private String name;
        @Column(name = "currency_code", nullable = false) private String currencyCode;
        @Column(name = "created_by", nullable = false) private UUID createdBy;
        @Column(name = "created_at", nullable = false) private Instant createdAt;
        protected Group() {}
        public Group(UUID id, String name, String currencyCode, UUID createdBy) {
            this.id = id; this.name = name; this.currencyCode = currencyCode;
            this.createdBy = createdBy; this.createdAt = Instant.now();
        }

        public UUID getId() { return id; }
        public String getName() { return name; }
        public String getCurrencyCode() { return currencyCode; }
        public UUID getCreatedBy() { return createdBy; }
        public Instant getCreatedAt() { return createdAt; }
    }

    @Entity @Table(name = "group_member")
    public static class Member {
        @Id private UUID id;
        @Column(name = "group_id", nullable = false) private UUID groupId;
        @Column(name = "user_id", nullable = false) private UUID userId;
        @Column(name = "joined_at", nullable = false) private Instant joinedAt;
        protected Member() {}
        public Member(UUID groupId, UUID userId) {
            this.id = UUID.randomUUID(); this.groupId = groupId; this.userId = userId; this.joinedAt = Instant.now();
        }

        public UUID getId() { return id; }
        public UUID getGroupId() { return groupId; }
        public UUID getUserId() { return userId; }
        public Instant getJoinedAt() { return joinedAt; }
    }

    @Entity @Table(name = "expense")
    public static class Expense {
        @Id private UUID id;
        @Column(name = "group_id", nullable = false) private UUID groupId;
        @Column(name = "payer_id", nullable = false) private UUID payerId;
        @Column(name = "creator_id", nullable = false) private UUID creatorId;
        @Column(nullable = false) private String description;
        @Column(name = "amount_minor", nullable = false) private long amountMinor;
        @Column(name = "spent_on", nullable = false) private LocalDate spentOn;
        @Column(nullable = false) private boolean reversed;
        @Version private long version;
        @Column(name = "request_key") private String requestKey;
        @Column(name = "created_at", nullable = false) private Instant createdAt;
        @Column(name = "updated_at", nullable = false) private Instant updatedAt;
        protected Expense() {}
        public Expense(UUID groupId, UUID payerId, UUID creatorId, String description, long amountMinor,
                LocalDate spentOn, String requestKey) {
            this.id = UUID.randomUUID(); this.groupId = groupId; this.payerId = payerId;
            this.creatorId = creatorId; this.description = description; this.amountMinor = amountMinor;
            this.spentOn = spentOn; this.requestKey = requestKey;
            this.createdAt = Instant.now(); this.updatedAt = createdAt;
        }

        public UUID getId() { return id; }
        public UUID getGroupId() { return groupId; }
        public UUID getPayerId() { return payerId; }
        public UUID getCreatorId() { return creatorId; }
        public String getDescription() { return description; }
        public long getAmountMinor() { return amountMinor; }
        public LocalDate getSpentOn() { return spentOn; }
        public boolean getReversed() { return reversed; }
        public long getVersion() { return version; }
        public String getRequestKey() { return requestKey; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }

        public void update(String description, long amountMinor, UUID payerId, LocalDate spentOn) {
            this.description = description; this.amountMinor = amountMinor;
            this.payerId = payerId; this.spentOn = spentOn; this.updatedAt = Instant.now();
        }

        public void reverse() {
            this.reversed = true; this.updatedAt = Instant.now();
        }
    }

    @Entity @Table(name = "expense_share")
    public static class Share {
        @Id private UUID id;
        @Column(name = "expense_id", nullable = false) private UUID expenseId;
        @Column(name = "user_id", nullable = false) private UUID userId;
        @Column(name = "amount_minor", nullable = false) private long amountMinor;
        protected Share() {}
        public Share(UUID expenseId, UUID userId, long amountMinor) {
            this.id = UUID.randomUUID(); this.expenseId = expenseId;
            this.userId = userId; this.amountMinor = amountMinor;
        }

        public UUID getId() { return id; }
        public UUID getExpenseId() { return expenseId; }
        public UUID getUserId() { return userId; }
        public long getAmountMinor() { return amountMinor; }
    }

    @Entity @Table(name = "settlement")
    public static class Settlement {
        @Id private UUID id;
        @Column(name = "group_id", nullable = false) private UUID groupId;
        @Column(name = "sender_id", nullable = false) private UUID senderId;
        @Column(name = "recipient_id", nullable = false) private UUID recipientId;
        @Column(name = "amount_minor", nullable = false) private long amountMinor;
        @Column(nullable = false) private String status;
        @Column(name = "request_key") private String requestKey;
        @Column(name = "created_at", nullable = false) private Instant createdAt;
        @Column(name = "decided_at") private Instant decidedAt;
        protected Settlement() {}
        public Settlement(UUID groupId, UUID senderId, UUID recipientId, long amountMinor, String requestKey) {
            this.id = UUID.randomUUID(); this.groupId = groupId; this.senderId = senderId;
            this.recipientId = recipientId; this.amountMinor = amountMinor;
            this.status = "PENDING"; this.requestKey = requestKey; this.createdAt = Instant.now();
        }

        public UUID getId() { return id; }
        public UUID getGroupId() { return groupId; }
        public UUID getSenderId() { return senderId; }
        public UUID getRecipientId() { return recipientId; }
        public long getAmountMinor() { return amountMinor; }
        public String getStatus() { return status; }
        public String getRequestKey() { return requestKey; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getDecidedAt() { return decidedAt; }

        public void decide(boolean confirm) {
            this.status = confirm ? "CONFIRMED" : "REJECTED";
            this.decidedAt = Instant.now();
        }
    }

    @Entity @Table(name = "activity_event")
    public static class Activity {
        @Id private UUID id;
        @Column(name = "group_id", nullable = false) private UUID groupId;
        @Column(name = "actor_id", nullable = false) private UUID actorId;
        @Column(name = "event_type", nullable = false) private String eventType;
        @Column(name = "subject_id", nullable = false) private UUID subjectId;
        @Column(nullable = false) private String detail;
        @Column(name = "created_at", nullable = false) private Instant createdAt;
        protected Activity() {}
        public Activity(UUID groupId, UUID actorId, String eventType, UUID subjectId, String detail) {
            this.id = UUID.randomUUID(); this.groupId = groupId; this.actorId = actorId;
            this.eventType = eventType; this.subjectId = subjectId;
            this.detail = detail; this.createdAt = Instant.now();
        }

        public UUID getId() { return id; }
        public UUID getGroupId() { return groupId; }
        public UUID getActorId() { return actorId; }
        public String getEventType() { return eventType; }
        public UUID getSubjectId() { return subjectId; }
        public String getDetail() { return detail; }
        public Instant getCreatedAt() { return createdAt; }
    }

    @Entity @Table(name = "group_invite")
    public static class Invite {
        @Id private UUID id;
        @Column(name = "group_id", nullable = false) private UUID groupId;
        @Column(name = "invited_email", nullable = false) private String invitedEmail;
        @Column(name = "token_hash", nullable = false) private String tokenHash;
        @Column(name = "invited_by", nullable = false) private UUID invitedBy;
        @Column(name = "expires_at", nullable = false) private Instant expiresAt;
        @Column(name = "accepted_by") private UUID acceptedBy;
        @Column(name = "accepted_at") private Instant acceptedAt;
        @Column(name = "created_at", nullable = false) private Instant createdAt;
        protected Invite() {}
        public Invite(UUID groupId, String invitedEmail, String tokenHash, UUID invitedBy) {
            this.id = UUID.randomUUID(); this.groupId = groupId; this.invitedEmail = invitedEmail;
            this.tokenHash = tokenHash; this.invitedBy = invitedBy;
            this.createdAt = Instant.now(); this.expiresAt = createdAt.plusSeconds(7 * 24 * 3600);
        }

        public UUID getId() { return id; }
        public UUID getGroupId() { return groupId; }
        public String getInvitedEmail() { return invitedEmail; }
        public String getTokenHash() { return tokenHash; }
        public UUID getInvitedBy() { return invitedBy; }
        public Instant getExpiresAt() { return expiresAt; }
        public UUID getAcceptedBy() { return acceptedBy; }
        public Instant getAcceptedAt() { return acceptedAt; }
        public Instant getCreatedAt() { return createdAt; }

        public void accept(UUID userId) {
            this.acceptedBy = userId; this.acceptedAt = Instant.now();
        }
    }
}
