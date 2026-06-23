package com.diabetes.assistant.modules.risk.service.impl;

import com.diabetes.assistant.modules.risk.service.RiskService;
import org.springframework.stereotype.Service;

@Service
public class RiskServiceImpl implements RiskService {

    @Override
    public String entry() {
        return "风险预测模块功能开发中";
    }
}
