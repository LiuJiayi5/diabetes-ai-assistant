package com.diabetes.assistant.modules.lifeplan.contract;

import com.diabetes.assistant.modules.lifeplan.contract.dto.LifePlanDTO;

public interface LifePlanQueryApi {

    LifePlanDTO getCurrentPlanByUserId(Integer userId);

    Integer getCurrentPlanIdByUserId(Integer userId);

    String getCurrentLifePlanSummaryByUserId(Integer userId);

    String getCurrentCheckinTasksJsonByUserId(Integer userId);
}
