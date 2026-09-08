package com.jurisfacil.organizations.controller.dto.response;

public record AcceptInviteResponse(String accessToken, String tokenType, long expiresIn, MemberResponse user) {
}
