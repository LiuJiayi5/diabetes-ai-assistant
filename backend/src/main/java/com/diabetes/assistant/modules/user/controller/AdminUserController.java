package com.diabetes.assistant.modules.user.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.user.dto.UpdateUserStatusRequest;
import com.diabetes.assistant.modules.user.service.UserService;
import com.diabetes.assistant.modules.user.vo.UserResponse;
import com.diabetes.assistant.modules.user.vo.UserStatusResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping
    public ApiResponse<PageResult<UserResponse>> listUsers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        Integer adminUserId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(userService.listUsers(adminUserId, page, pageSize, keyword, role, status));
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<UserStatusResponse> updateStatus(
            HttpServletRequest request,
            @PathVariable("userId") Integer userId,
            @RequestBody UpdateUserStatusRequest updateRequest) {
        Integer adminUserId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(userService.updateUserStatus(adminUserId, userId, updateRequest));
    }
}
