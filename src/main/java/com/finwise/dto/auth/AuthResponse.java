package com.finwise.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    @Builder.Default
    private String tokenType = "Bearer";

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String currencyCode;
    private Set<String> roles;
}
