package com.diabetes.assistant.modules.user.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.modules.user.dto.LoginRequest;
import com.diabetes.assistant.modules.user.dto.RegisterRequest;
import com.diabetes.assistant.modules.user.service.UserService;
import com.diabetes.assistant.modules.user.vo.LoginResponse;
import com.diabetes.assistant.modules.user.vo.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@RequestBody RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }
}
