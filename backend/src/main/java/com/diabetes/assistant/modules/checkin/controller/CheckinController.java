package com.diabetes.assistant.modules.checkin.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.modules.checkin.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @GetMapping("/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(checkinService.entry());
    }
}
