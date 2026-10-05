package com.fareshare.service;

import com.fareshare.model.Model;
import com.fareshare.controller.ApiDtos;
import com.fareshare.controller.ApiError;
import com.fareshare.repository.Users;
import com.fareshare.repository.Groups;
import com.fareshare.repository.Members;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {
    private final Groups groups;
    private final Members members;
    private final Users users;

    public GroupService(Groups groups, Members members, Users users) {
        this.groups = groups; this.members = members; this.users = users;
    }

    @Transactional
    public ApiDtos.GroupView create(Model.User actor, ApiDtos.CreateGroup request) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.name().length() > 120) throw ApiError.bad("Group name is required and must be at most 120 characters");
        String currency = CurrencyRules.requireSupported(request.currencyCode());
        Model.Group group = groups.save(new Model.Group(UUID.randomUUID(), request.name().trim(), currency, actor.getId()));
        members.save(new Model.Member(group.getId(), actor.getId()));
        return view(group);
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.GroupView> list(Model.User actor) {
        List<UUID> ids = members.findByUserId(actor.getId()).stream().map(m -> m.getGroupId()).toList();
        return groups.findByIdIn(ids).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public ApiDtos.GroupView get(UUID groupId, Model.User actor) {
        return view(requireMember(groupId, actor.getId()));
    }

    public Model.Group requireMember(UUID groupId, UUID userId) {
        Model.Group group = groups.findById(groupId).orElseThrow(() -> ApiError.missing("Group not found"));
        if (!members.existsByGroupIdAndUserId(groupId, userId)) throw ApiError.missing("Group not found");
        return group;
    }

    public void requireMembers(UUID groupId, List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) throw ApiError.bad("At least one participant is required");
        for (UUID userId : userIds) {
            if (userId == null || !members.existsByGroupIdAndUserId(groupId, userId)) {
                throw ApiError.bad("Every user must belong to the group");
            }
        }
    }

    private ApiDtos.GroupView view(Model.Group group) {
        List<ApiDtos.MemberView> memberViews = members.findByGroupId(group.getId()).stream().map(member -> {
            Model.User user = users.findById(member.getUserId()).orElseThrow();
            return new ApiDtos.MemberView(user.getId(), user.getDisplayName(), user.getEmail());
        }).toList();
        return new ApiDtos.GroupView(group.getId(), group.getName(), group.getCurrencyCode(), memberViews);
    }
}
