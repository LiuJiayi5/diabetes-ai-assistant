package com.diabetes.assistant.modules.lifeplan.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.lifeplan.dto.GenerateLifePlanRequest;
import com.diabetes.assistant.modules.lifeplan.vo.LifePlanResponse;

import java.time.LocalDate;

public interface LifePlanService {

    String entry();

    LifePlanResponse generate(Integer userId, GenerateLifePlanRequest request);

    PageResult<LifePlanResponse> listUserPlans(Integer userId, Integer page, Integer pageSize, String status, String callStatus);

    LifePlanResponse getUserPlanDetail(Integer userId, Integer planId);

    PageResult<LifePlanResponse> listAdminPlans(Integer adminUserId, Integer page, Integer pageSize, Integer planId, Integer userId,
                                                String keyword, String status, String callStatus,
                                                LocalDate startDate, LocalDate endDate);
}
