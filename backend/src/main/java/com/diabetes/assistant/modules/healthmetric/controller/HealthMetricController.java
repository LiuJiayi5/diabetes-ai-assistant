package com.diabetes.assistant.modules.healthmetric.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.modules.healthmetric.service.HealthMetricService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health-metric")
@RequiredArgsConstructor
public class HealthMetricController {

    private final HealthMetricService healthmetricService;

    @GetMapping("/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(healthmetricService.entry());
    }
}
