package com.diabetes.assistant.modules.profile.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.profile.contract.dto.PatientProfileDTO;
import com.diabetes.assistant.modules.profile.dto.AdminProfileListItem;
import com.diabetes.assistant.modules.profile.dto.SaveProfileRequest;
import com.diabetes.assistant.modules.profile.dto.SaveProfileResponse;
import com.diabetes.assistant.modules.profile.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/entry")
    public ApiResponse<PatientProfileDTO> entry() {
        return ApiResponse.success(profileService.getMyProfile());
    }

    @PostMapping
    public ApiResponse<SaveProfileResponse> saveProfile(@Valid @RequestBody SaveProfileRequest request) {
        return ApiResponse.success(profileService.saveProfile(request));
    }

    @GetMapping("/admin")
    public ApiResponse<PageResult<AdminProfileListItem>> adminListProfiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ApiResponse.success(profileService.adminListProfiles(
                keyword, gender, minAge, maxAge, page, pageSize));
    }

    @GetMapping("/admin/{user_id}")
    public ApiResponse<PatientProfileDTO> adminGetProfileDetail(@PathVariable("user_id") Integer userId) {
        return ApiResponse.success(profileService.adminGetProfileDetail(userId));
    }
}
