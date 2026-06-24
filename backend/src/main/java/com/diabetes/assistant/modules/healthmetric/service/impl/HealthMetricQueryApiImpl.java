package com.diabetes.assistant.modules.healthmetric.service.impl;

import com.diabetes.assistant.modules.healthmetric.contract.HealthMetricQueryApi;
import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class HealthMetricQueryApiImpl implements HealthMetricQueryApi {

    @Override
    public HealthMetricDTO getLatestMetricByUserId(Integer userId) {
        return null;
    }

    @Override
    public List<HealthMetricDTO> listMetricsByUserId(Integer userId, LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public String getLatestMetricSummaryByUserId(Integer userId) {
        return "No latest health data";
    }
}
