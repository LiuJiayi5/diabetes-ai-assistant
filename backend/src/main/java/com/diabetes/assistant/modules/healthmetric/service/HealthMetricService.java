package com.diabetes.assistant.modules.healthmetric.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import com.diabetes.assistant.modules.healthmetric.dto.AdminMetricListItem;
import com.diabetes.assistant.modules.healthmetric.dto.SaveMetricRequest;
import com.diabetes.assistant.modules.healthmetric.dto.SaveMetricResponse;

import java.time.LocalDate;

public interface HealthMetricService {

    HealthMetricDTO getEntry();

    SaveMetricResponse saveMetric(SaveMetricRequest request);

    HealthMetricDTO getLatestMetric();

    PageResult<HealthMetricDTO> getHistory(Integer page, Integer pageSize,
                                           LocalDate startDate, LocalDate endDate);

    PageResult<AdminMetricListItem> adminListMetrics(Integer userId, LocalDate startDate, LocalDate endDate,
                                                     String abnormalOnly, Integer page, Integer pageSize);

    PageResult<AdminMetricListItem> adminListAbnormalMetrics(Integer userId, Integer page, Integer pageSize);
}
