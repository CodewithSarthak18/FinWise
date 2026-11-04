package com.finwise.service.impl;

import com.finwise.dto.auth.AuthResponse;
import com.finwise.dto.auth.LoginRequest;
import com.finwise.dto.auth.RefreshTokenRequest;
import com.finwise.dto.auth.RegisterRequest;
import com.finwise.entity.RefreshToken;
import com.finwise.entity.Role;
import com.finwise.entity.User;
import com.finwise.exception.BadRequestException;
import com.finwise.exception.DuplicateResourceException;
import com.finwise.exception.UnauthorizedException;
import com.finwise.mapper.UserMapper;
import com.finwise.repository.RefreshTokenRepository;
import com.finwise.repository.RoleRepository;
import com.finwise.repository.UserRepository;
import com.finwise.security.JwtTokenProvider;
import com.finwise.security.UserPrincipal;
import com.finwise.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    @Value("${finwise.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_USER")
                        .description("Standard authenticated user")
                        .build()));

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(request.getPhoneNumber())
                .currencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR")
                .active(true)
                .roles(Collections.singleton(userRole))
                .build();

        User savedUser = userRepository.save(user);
        UserPrincipal principal = UserPrincipal.create(savedUser);

        String accessToken = tokenProvider.generateAccessToken(principal);
        RefreshToken refreshToken = createRefreshToken(savedUser);

        return userMapper.toAuthResponse(
                savedUser,
                accessToken,
                refreshToken.getToken(),
                tokenProvider.getAccessTokenExpirationMs() / 1000
        );
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User account not found"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive. Please contact support.");
        }

        String accessToken = tokenProvider.generateAccessToken(principal);
        RefreshToken refreshToken = createRefreshToken(user);

        return userMapper.toAuthResponse(
                user,
                accessToken,
                refreshToken.getToken(),
                tokenProvider.getAccessTokenExpirationMs() / 1000
        );
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            // Suspicious token reuse attempt - revoke all tokens for this user
            refreshTokenRepository.revokeAllUserTokens(storedToken.getUser());
            throw new UnauthorizedException("Compromised refresh token used. All sessions revoked.");
        }

        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        User user = storedToken.getUser();
        UserPrincipal principal = UserPrincipal.create(user);

        // Rotate token: revoke old token and create a new refresh token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        RefreshToken newRefreshToken = createRefreshToken(user);
        String newAccessToken = tokenProvider.generateAccessToken(principal);

        return userMapper.toAuthResponse(
                user,
                newAccessToken,
                newRefreshToken.getToken(),
                tokenProvider.getAccessTokenExpirationMs() / 1000
        );
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        refreshTokenRepository.revokeAllUserTokens(user);
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }
}
