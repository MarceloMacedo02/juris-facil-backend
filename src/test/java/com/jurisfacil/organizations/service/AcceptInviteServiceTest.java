package com.jurisfacil.organizations.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.jurisfacil.iam.repository.UserRepository;
import com.jurisfacil.iam.security.JwtService;
import com.jurisfacil.iam.service.RefreshService;
import com.jurisfacil.organizations.controller.dto.request.AcceptInviteRequest;
import com.jurisfacil.organizations.mapper.MembershipMapper;
import com.jurisfacil.organizations.repository.MembershipRepository;
import com.jurisfacil.organizations.repository.OrganizationRepository;
import com.jurisfacil.organizations.service.impl.InviteAcceptanceServiceImpl;

class AcceptInviteServiceTest {

    @Test
    void rejectsUnknownInviteWithoutTouchingUserOrOrganization() {
        MembershipRepository memberships = mock(MembershipRepository.class);
        when(memberships.findByInviteTokenHash(anyString())).thenReturn(Optional.empty());
        InviteAcceptanceServiceImpl service = new InviteAcceptanceServiceImpl(memberships,
                mock(OrganizationRepository.class), mock(UserRepository.class), new BCryptPasswordEncoder(),
                mock(JwtService.class), mock(RefreshService.class), new MembershipMapper());

        assertThatThrownBy(() -> service.accept(new AcceptInviteRequest("unknown", "Name", "password123"),
                "127.0.0.1", "test-agent"))
                .isInstanceOf(InviteAcceptanceServiceImpl.InviteAcceptanceException.class)
                .extracting("code").isEqualTo("INVITE_INVALID_OR_EXPIRED");
    }
}
