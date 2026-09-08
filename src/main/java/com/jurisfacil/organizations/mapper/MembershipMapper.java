package com.jurisfacil.organizations.mapper;

import org.springframework.stereotype.Component;

import com.jurisfacil.iam.model.entity.UserEntity;
import com.jurisfacil.organizations.controller.dto.response.MemberResponse;
import com.jurisfacil.organizations.model.entity.MembershipEntity;

@Component
public class MembershipMapper {

    public MemberResponse toResponse(MembershipEntity membership, UserEntity user) {
        return new MemberResponse(membership.getId(), user.getId(), user.getName(), user.getEmail(),
                membership.getRole().name(), membership.getStatus().name(), membership.getCreatedAt());
    }
}
