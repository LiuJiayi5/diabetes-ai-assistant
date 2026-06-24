package com.diabetes.assistant.modules.lifeplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.modules.lifeplan.contract.LifePlanQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.dto.LifePlanDTO;
import com.diabetes.assistant.modules.lifeplan.entity.LifePlan;
import com.diabetes.assistant.modules.lifeplan.mapper.LifePlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LifePlanQueryApiImpl implements LifePlanQueryApi {

    private final LifePlanMapper lifePlanMapper;

    @Override
    public LifePlanDTO getCurrentPlanByUserId(Integer userId) {
        LifePlan lifePlan = lifePlanMapper.selectOne(new LambdaQueryWrapper<LifePlan>()
                .eq(LifePlan::getUserId, userId)
                .eq(LifePlan::getStatus, "active")
                .orderByDesc(LifePlan::getCreateTime)
                .last("LIMIT 1"));
        return lifePlan == null ? null : toDTO(lifePlan);
    }

    @Override
    public Integer getCurrentPlanIdByUserId(Integer userId) {
        LifePlanDTO lifePlan = getCurrentPlanByUserId(userId);
        return lifePlan == null ? null : lifePlan.getPlanId();
    }

    @Override
    public String getCurrentLifePlanSummaryByUserId(Integer userId) {
        LifePlanDTO lifePlan = getCurrentPlanByUserId(userId);
        return lifePlan == null ? "" : lifePlan.getSummary();
    }

    @Override
    public String getCurrentCheckinTasksJsonByUserId(Integer userId) {
        LifePlanDTO lifePlan = getCurrentPlanByUserId(userId);
        return lifePlan == null ? "" : lifePlan.getCheckinTasksJson();
    }

    private LifePlanDTO toDTO(LifePlan lifePlan) {
        LifePlanDTO dto = new LifePlanDTO();
        dto.setPlanId(lifePlan.getPlanId());
        dto.setUserId(lifePlan.getUserId());
        dto.setAssessmentId(lifePlan.getAssessmentId());
        dto.setPlanTitle(lifePlan.getPlanTitle());
        dto.setPlanGoal(lifePlan.getPlanGoal());
        dto.setCheckinTasksJson(lifePlan.getCheckinTasksJson());
        dto.setSummary(lifePlan.getSummary());
        dto.setStatus(lifePlan.getStatus());
        dto.setCallStatus(lifePlan.getCallStatus());
        dto.setErrorMessage(lifePlan.getErrorMessage());
        dto.setCreateTime(lifePlan.getCreateTime());
        return dto;
    }
}
