package com.diabetes.assistant.modules.user.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.user.dto.LoginRequest;
import com.diabetes.assistant.modules.user.dto.RegisterRequest;
import com.diabetes.assistant.modules.user.dto.UpdateUserRequest;
import com.diabetes.assistant.modules.user.dto.UpdateUserStatusRequest;
import com.diabetes.assistant.modules.user.vo.LoginResponse;
import com.diabetes.assistant.modules.user.vo.UserResponse;
import com.diabetes.assistant.modules.user.vo.UserStatusResponse;

public interface UserService {

    String entry();

    UserResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserResponse getCurrentUser(Integer userId);

    UserResponse updateCurrentUser(Integer userId, UpdateUserRequest request);

    PageResult<UserResponse> listUsers(Integer adminUserId, Integer page, Integer pageSize, String keyword, String role, String status);

    UserStatusResponse updateUserStatus(Integer adminUserId, Integer userId, UpdateUserStatusRequest request);
}
