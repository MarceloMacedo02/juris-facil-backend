package com.jurisfacil.organizations.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jurisfacil.organizations.controller.dto.request.AcceptInviteRequest;
import com.jurisfacil.organizations.controller.dto.response.AcceptInviteResponse;
import com.jurisfacil.organizations.service.InviteAcceptanceService;
import java.time.Duration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Workspace invitation")
public class AcceptInviteController {

    private final InviteAcceptanceService inviteAcceptanceService;

    @PostMapping("/accept-invite")
    @Operation(summary = "Accept a workspace invitation")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Invitation accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired invitation"),
            @ApiResponse(responseCode = "403", description = "Workspace suspended"),
            @ApiResponse(responseCode = "422", description = "Validation error"),
            @ApiResponse(responseCode = "500", description = "Internal error")})
    public ResponseEntity<AcceptInviteResponse> accept(@Valid @RequestBody AcceptInviteRequest request,
            HttpServletRequest httpRequest) {
        InviteAcceptanceService.AcceptedInvite accepted = inviteAcceptanceService.accept(request,
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        ResponseCookie cookie = ResponseCookie.from("JF_REFRESH", accepted.refreshToken())
                .httpOnly(true).secure(true).sameSite("Lax").path("/api").maxAge(Duration.ofDays(7)).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(accepted.response());
    }
}
