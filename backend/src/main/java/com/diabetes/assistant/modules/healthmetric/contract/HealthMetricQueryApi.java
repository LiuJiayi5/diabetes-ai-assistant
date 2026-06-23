package com.diabetes.assistant.modules.healthmetric.contract;

import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;

import java.time.LocalDate;
import java.util.List;

public interface HealthMetricQueryApi {

    HealthMetricDTO getLatestMetricByUserId(Integer userId);

    List<HealthMetricDTO> listMetricsByUserId(Integer userId, LocalDate startDate, LocalDate endDate);

    String getLatestMetricSummaryByUserId(Integer userId);
}
