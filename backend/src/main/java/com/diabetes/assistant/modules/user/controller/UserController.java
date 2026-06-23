package com.diabetes.assistant.modules.user.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(userService.entry());
    }
}
