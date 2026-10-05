package com.fareshare.service;

import com.fareshare.model.Model;
import com.fareshare.controller.ApiDtos;
import com.fareshare.controller.ApiError;
import com.fareshare.repository.Settlements;
import com.fareshare.repository.Activities;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {
    private final GroupService groups;
    private final Settlements settlements;
    private final Activities activities;

    public SettlementService(GroupService groups, Settlements settlements, Activities activities) {
        this.groups = groups; this.settlements = settlements; this.activities = activities;
    }

    @Transactional
    public ApiDtos.SettlementView create(UUID groupId, Model.User actor, ApiDtos.CreateSettlement request, String key) {
        groups.requireMember(groupId, actor.getId());
        String requestKey = ExpenseService.requireKey(key);
        if (request == null || request.senderId() == null || request.recipientId() == null
                || request.amountMinor() == null || request.amountMinor() <= 0
                || request.senderId().equals(request.recipientId())) {
            throw ApiError.bad("A positive settlement between two members is required");
        }
        if (!actor.getId().equals(request.senderId())) throw ApiError.forbidden("Only the sender can record a settlement");
        groups.requireMembers(groupId, List.of(request.senderId(), request.recipientId()));
        var existing = settlements.findByGroupIdAndRequestKey(groupId, requestKey);
        if (existing.isPresent()) {
            Model.Settlement previous = existing.get();
            if (!previous.getSenderId().equals(request.senderId())
                    || !previous.getRecipientId().equals(request.recipientId())
                    || previous.getAmountMinor() != request.amountMinor()) {
                throw ApiError.conflict("Idempotency key was used for a different settlement");
            }
            return view(previous);
        }
        Model.Settlement settlement = settlements.save(new Model.Settlement(groupId, request.senderId(),
                request.recipientId(), request.amountMinor(), requestKey));
        activities.save(new Model.Activity(groupId, actor.getId(), "SETTLEMENT_REQUESTED", settlement.getId(),
                "Amount: " + settlement.getAmountMinor() + "; recipient: " + settlement.getRecipientId()));
        return view(settlement);
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.SettlementView> list(UUID groupId, Model.User actor) {
        groups.requireMember(groupId, actor.getId());
        return settlements.findByGroupIdOrderByCreatedAtDesc(groupId).stream().map(this::view).toList();
    }

    @Transactional
    public ApiDtos.SettlementView decide(UUID settlementId, Model.User actor, boolean confirm) {
        Model.Settlement settlement = settlements.findById(settlementId)
                .orElseThrow(() -> ApiError.missing("Settlement not found"));
        groups.requireMember(settlement.getGroupId(), actor.getId());
        if (!settlement.getRecipientId().equals(actor.getId())) {
            throw ApiError.forbidden("Only the recipient can decide this settlement");
        }
        if (!"PENDING".equals(settlement.getStatus())) throw ApiError.conflict("Settlement is already decided");
        settlement.decide(confirm);
        settlements.save(settlement);
        activities.save(new Model.Activity(settlement.getGroupId(), actor.getId(),
                confirm ? "SETTLEMENT_CONFIRMED" : "SETTLEMENT_REJECTED", settlement.getId(),
                "Amount: " + settlement.getAmountMinor()));
        return view(settlement);
    }

    private ApiDtos.SettlementView view(Model.Settlement settlement) {
        return new ApiDtos.SettlementView(settlement.getId(), settlement.getGroupId(), settlement.getSenderId(),
                settlement.getRecipientId(), settlement.getAmountMinor(), settlement.getStatus(),
                settlement.getCreatedAt(), settlement.getDecidedAt());
    }
}
