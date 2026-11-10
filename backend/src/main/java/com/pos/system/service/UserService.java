package com.pos.system.service;

import com.pos.system.dto.response.UserResponse;

import java.util.List;
import java.util.Set;

public interface UserService {
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    Set<String> getUserPermissions(Long userId);
    java.util.Map<String, Object> exportUserData(Long userId);
    void requestAccountDeletion(Long userId);
}
