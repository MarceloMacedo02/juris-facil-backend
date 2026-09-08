package com.jurisfacil.organizations.service;

import com.jurisfacil.organizations.controller.dto.request.AcceptInviteRequest;
import com.jurisfacil.organizations.controller.dto.response.AcceptInviteResponse;

public interface InviteAcceptanceService {
    AcceptedInvite accept(AcceptInviteRequest request, String ipAddress, String userAgent);

    record AcceptedInvite(AcceptInviteResponse response, String refreshToken) {
    }
}
