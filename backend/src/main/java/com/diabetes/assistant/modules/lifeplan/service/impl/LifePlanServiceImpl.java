package com.diabetes.assistant.modules.lifeplan.service.impl;

import com.diabetes.assistant.modules.lifeplan.service.LifePlanService;
import org.springframework.stereotype.Service;

@Service
public class LifePlanServiceImpl implements LifePlanService {

    @Override
    public String entry() {
        return "生活方案模块功能开发中";
    }
}
