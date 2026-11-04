package com.finwise.service;

import com.finwise.dto.auth.PasswordChangeRequest;
import com.finwise.dto.auth.UserProfileResponse;

public interface UserService {

    UserProfileResponse getCurrentUserProfile(Long userId);

    void changePassword(Long userId, PasswordChangeRequest request);
}
