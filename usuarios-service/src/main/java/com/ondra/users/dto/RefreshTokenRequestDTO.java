package com.ondra.users.dto;

import lombok.*;

/**
 * DTO para solicitar un nuevo access token usando un refresh token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequestDTO {
    private String refreshToken;
}