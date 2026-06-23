package com.diabetes.assistant.modules.risk.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.modules.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping("/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(riskService.entry());
    }
}
