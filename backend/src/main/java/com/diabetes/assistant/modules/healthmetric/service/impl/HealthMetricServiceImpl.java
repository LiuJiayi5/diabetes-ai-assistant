package com.diabetes.assistant.modules.healthmetric.service.impl;

import com.diabetes.assistant.modules.healthmetric.service.HealthMetricService;
import org.springframework.stereotype.Service;

@Service
public class HealthMetricServiceImpl implements HealthMetricService {

    @Override
    public String entry() {
        return "健康数据模块功能开发中";
    }
}
