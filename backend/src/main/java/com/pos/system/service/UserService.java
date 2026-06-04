package com.pos.system.service;

import com.pos.system.dto.response.UserResponse;

import java.util.List;
import java.util.Set;

public interface UserService {
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    Set<String> getUserPermissions(Long userId);
}
