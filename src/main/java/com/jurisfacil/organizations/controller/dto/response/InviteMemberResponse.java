package com.jurisfacil.organizations.controller.dto.response;

import java.util.UUID;

public record InviteMemberResponse(UUID membershipId, String status, String inviteToken) {
}
