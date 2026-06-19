package com.maf.exchangeapiv1.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;

    public String refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String userId = jwtService.getUserIdFromToken(refreshToken);
        String email = jwtService.getEmailFromToken(refreshToken);

        if (email == null || userId == null) {
            throw new IllegalArgumentException("Invalid token: email not found");
        }

        return jwtService.generateAccessToken(userId, email);
    }
}
