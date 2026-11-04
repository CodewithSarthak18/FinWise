package com.finwise.mapper;

import com.finwise.dto.auth.AuthResponse;
import com.finwise.dto.auth.UserProfileResponse;
import com.finwise.entity.Role;
import com.finwise.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserProfileResponse toProfileResponse(User user) {
        if (user == null) {
            return null;
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .currencyCode(user.getCurrencyCode())
                .active(user.isActive())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .build();
    }

    public AuthResponse toAuthResponse(User user, String accessToken, String refreshToken, Long expiresIn) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .currencyCode(user.getCurrencyCode())
                .roles(roleNames)
                .build();
    }
}
