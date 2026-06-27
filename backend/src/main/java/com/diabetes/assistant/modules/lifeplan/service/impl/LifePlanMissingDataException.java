package com.diabetes.assistant.modules.lifeplan.service.impl;

import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.modules.lifeplan.vo.MissingLifePlanDataResponse;
import lombok.Getter;

@Getter
public class LifePlanMissingDataException extends BusinessException {

    private final MissingLifePlanDataResponse payload;

    public LifePlanMissingDataException(MissingLifePlanDataResponse payload) {
        super(400, "缺少健康档案、最新健康数据或最近一次风险评估，请先完成基础信息、健康数据录入和风险预测");
        this.payload = payload;
    }
}
