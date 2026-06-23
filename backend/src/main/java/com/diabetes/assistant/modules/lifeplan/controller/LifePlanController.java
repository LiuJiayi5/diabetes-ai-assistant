package com.diabetes.assistant.modules.lifeplan.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.modules.lifeplan.service.LifePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/life-plan")
@RequiredArgsConstructor
public class LifePlanController {

    private final LifePlanService lifeplanService;

    @GetMapping("/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(lifeplanService.entry());
    }
}
