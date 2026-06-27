package com.diabetes.assistant.modules.user.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.user.dto.UpdateUserRequest;
import com.diabetes.assistant.modules.user.service.UserService;
import com.diabetes.assistant.modules.user.vo.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(userService.entry());
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(userService.getCurrentUser(userId));
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateCurrentUser(
            HttpServletRequest request,
            @RequestBody UpdateUserRequest updateRequest) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(userService.updateCurrentUser(userId, updateRequest));
    }
}
