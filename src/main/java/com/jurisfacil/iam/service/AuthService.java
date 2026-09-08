package com.jurisfacil.iam.service;

import com.jurisfacil.iam.model.entity.UserEntity;

public interface AuthService {

    UserEntity authenticate(String email, String password);

    AuthenticatedUser login(String email, String password, boolean rememberMe, String ipAddress, String userAgent);

    record AuthenticatedUser(UserEntity user, String accessToken, String refreshToken) {
    }
}
