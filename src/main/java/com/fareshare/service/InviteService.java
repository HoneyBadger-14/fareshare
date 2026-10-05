package com.fareshare.service;

import com.fareshare.model.Model;
import com.fareshare.controller.ApiDtos;
import com.fareshare.controller.ApiError;
import com.fareshare.repository.Members;
import com.fareshare.repository.Activities;
import com.fareshare.repository.Invites;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

interface InviteDelivery {
    String send(String email, String token);
}

@Component @Profile("local")
class LocalInviteDelivery implements InviteDelivery {
    @Override public String send(String email, String token) {
        return "fareshare://invite?token=" + token;
    }
}

@Component @Profile("!local")
class PendingInviteDelivery implements InviteDelivery {
    @Override public String send(String email, String token) {
        throw new ApiError(HttpStatus.SERVICE_UNAVAILABLE, "Invitation email provider is not configured");
    }
}

@Service
public class InviteService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final GroupService groups;
    private final Invites invites;
    private final Members members;
    private final Activities activities;
    private final InviteDelivery delivery;

    public InviteService(GroupService groups, Invites invites, Members members, Activities activities,
                  InviteDelivery delivery) {
        this.groups = groups; this.invites = invites; this.members = members;
        this.activities = activities; this.delivery = delivery;
    }

    @Transactional
    public ApiDtos.InviteView create(UUID groupId, Model.User actor, ApiDtos.CreateInvite request) {
        groups.requireMember(groupId, actor.getId());
        if (request == null || request.email() == null
                || !request.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw ApiError.bad("A valid invitation email is required");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (email.length() > 320) throw ApiError.bad("Invitation email is too long");
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        Model.Invite invite = invites.save(new Model.Invite(groupId, email, hash(token), actor.getId()));
        String localJoinLink = delivery.send(email, token);
        activities.save(new Model.Activity(groupId, actor.getId(), "MEMBER_INVITED", invite.getId(),
                "Invited " + email));
        return new ApiDtos.InviteView(invite.getId(), groupId, email, invite.getExpiresAt(), localJoinLink);
    }

    @Transactional
    public ApiDtos.GroupView accept(Model.User actor, ApiDtos.AcceptInvite request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            throw ApiError.bad("Invitation token is required");
        }
        Model.Invite invite = invites.findByTokenHash(hash(request.token()))
                .orElseThrow(() -> ApiError.missing("Invitation not found"));
        if (invite.getAcceptedAt() != null || !invite.getExpiresAt().isAfter(Instant.now())) {
            throw ApiError.conflict("Invitation has expired or was already used");
        }
        if (!invite.getInvitedEmail().equals(actor.getEmail())) {
            throw ApiError.forbidden("Sign in with the invited email address");
        }
        if (members.existsByGroupIdAndUserId(invite.getGroupId(), actor.getId())) {
            throw ApiError.conflict("Already a group member");
        }
        members.save(new Model.Member(invite.getGroupId(), actor.getId()));
        invite.accept(actor.getId());
        invites.save(invite);
        activities.save(new Model.Activity(invite.getGroupId(), actor.getId(), "MEMBER_JOINED", invite.getId(),
                "Joined by invitation"));
        return groups.get(invite.getGroupId(), actor);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
