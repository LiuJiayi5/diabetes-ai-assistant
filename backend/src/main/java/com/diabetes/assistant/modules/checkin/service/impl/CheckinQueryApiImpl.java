package com.diabetes.assistant.modules.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diabetes.assistant.modules.checkin.contract.CheckinQueryApi;
import com.diabetes.assistant.modules.checkin.contract.dto.CheckinAnalysisDTO;
import com.diabetes.assistant.modules.checkin.contract.dto.CheckinRecordDTO;
import com.diabetes.assistant.modules.checkin.dto.CheckinTaskResponse;
import com.diabetes.assistant.modules.checkin.entity.CheckinAnalysis;
import com.diabetes.assistant.modules.checkin.mapper.CheckinAnalysisMapper;
import com.diabetes.assistant.modules.checkin.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckinQueryApiImpl implements CheckinQueryApi {

    private final CheckinService checkinService;
    private final CheckinAnalysisMapper checkinAnalysisMapper;

    @Override
    public List<CheckinRecordDTO> listRecentCheckins(Integer userId, Integer period) {
        return checkinService.listRecentCheckins(userId, period).stream()
                .map(this::toRecordDTO)
                .toList();
    }

    @Override
    public BigDecimal getRecentCompletionRate(Integer userId, Integer period) {
        return checkinService.getRecentCompletionRate(userId, period);
    }

    @Override
    public String getLatestCheckinSummaryByUserId(Integer userId, Integer period) {
        return checkinService.getLatestCheckinSummaryByUserId(userId, period);
    }

    @Override
    public CheckinAnalysisDTO getLatestAnalysisByUserId(Integer userId) {
        CheckinAnalysis analysis = checkinAnalysisMapper.selectOne(new LambdaQueryWrapper<CheckinAnalysis>()
                .eq(CheckinAnalysis::getUserId, userId)
                .orderByDesc(CheckinAnalysis::getCreateTime)
                .last("LIMIT 1"));
        return analysis == null ? null : toAnalysisDTO(analysis);
    }

    @Override
    public String getLatestCheckinAnalysisSummaryByUserId(Integer userId) {
        CheckinAnalysisDTO latest = getLatestAnalysisByUserId(userId);
        return latest == null ? "" : latest.getSummary();
    }

    private CheckinRecordDTO toRecordDTO(CheckinTaskResponse response) {
        CheckinRecordDTO dto = new CheckinRecordDTO();
        dto.setCheckinId(response.getCheckinId());
        dto.setUserId(response.getUserId());
        dto.setPlanId(response.getPlanId());
        dto.setTaskType(response.getTaskType());
        dto.setTaskName(response.getTaskName());
        dto.setStatus(response.getStatus());
        dto.setNote(response.getNote());
        dto.setCheckinDate(response.getCheckinDate());
        dto.setCompletedTime(response.getCompletedTime());
        return dto;
    }

    private CheckinAnalysisDTO toAnalysisDTO(CheckinAnalysis analysis) {
        CheckinAnalysisDTO dto = new CheckinAnalysisDTO();
        dto.setAnalysisId(analysis.getAnalysisId());
        dto.setUserId(analysis.getUserId());
        dto.setPlanId(analysis.getPlanId());
        dto.setStartDate(analysis.getStartDate());
        dto.setEndDate(analysis.getEndDate());
        dto.setTotalDays(analysis.getTotalDays());
        dto.setDietCompletionCount(analysis.getDietCompletionCount());
        dto.setExerciseCompletionCount(analysis.getExerciseCompletionCount());
        dto.setCompletionRate(analysis.getCompletionRate());
        dto.setHabitScore(analysis.getHabitScore());
        dto.setDietSummary(analysis.getDietSummary());
        dto.setExerciseSummary(analysis.getExerciseSummary());
        dto.setLifeEvaluation(analysis.getLifeEvaluation());
        dto.setMainProblems(analysis.getMainProblems());
        dto.setImprovementSuggestions(analysis.getImprovementSuggestions());
        dto.setNextFocus(analysis.getNextFocus());
        dto.setSummary(analysis.getSummary());
        dto.setInputSummary(analysis.getInputSummary());
        dto.setCallStatus(analysis.getCallStatus());
        dto.setErrorMessage(analysis.getErrorMessage());
        dto.setCreateTime(analysis.getCreateTime());
        return dto;
    }
}
