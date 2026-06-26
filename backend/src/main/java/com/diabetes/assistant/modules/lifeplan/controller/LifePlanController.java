package com.diabetes.assistant.modules.lifeplan.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.lifeplan.dto.GenerateLifePlanRequest;
import com.diabetes.assistant.modules.lifeplan.service.LifePlanService;
import com.diabetes.assistant.modules.lifeplan.vo.LifePlanResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class LifePlanController {

    private final LifePlanService lifePlanService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/api/life-plan/entry")
    public ApiResponse<String> entry() {
        return ApiResponse.success(lifePlanService.entry());
    }

    @PostMapping("/api/ai/life-plan/generate")
    public ApiResponse<LifePlanResponse> generate(HttpServletRequest request, @RequestBody GenerateLifePlanRequest generateRequest) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(lifePlanService.generate(userId, generateRequest));
    }

    @GetMapping("/api/life-plans")
    public ApiResponse<PageResult<LifePlanResponse>> listUserPlans(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(name = "call_status", required = false) String callStatus) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(lifePlanService.listUserPlans(userId, page, pageSize, status, callStatus));
    }

    @GetMapping("/api/life-plans/{planId}")
    public ApiResponse<LifePlanResponse> getUserPlanDetail(HttpServletRequest request, @PathVariable Integer planId) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(lifePlanService.getUserPlanDetail(userId, planId));
    }

    @GetMapping("/api/admin/life-plans")
    public ApiResponse<PageResult<LifePlanResponse>> listAdminPlans(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "plan_id", required = false) Integer planId,
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(name = "call_status", required = false) String callStatus,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer adminUserId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(lifePlanService.listAdminPlans(adminUserId, page, pageSize, planId, userId, keyword, status, callStatus, startDate, endDate));
    }
}
