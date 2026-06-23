package com.diabetes.assistant.modules.profile.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.modules.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(profileService.entry());
    }
}
