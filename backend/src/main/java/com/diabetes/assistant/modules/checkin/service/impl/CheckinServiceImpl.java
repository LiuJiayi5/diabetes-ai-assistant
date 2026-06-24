package com.diabetes.assistant.modules.checkin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.checkin.dto.CheckinHistoryQuery;
import com.diabetes.assistant.modules.checkin.dto.CheckinStatisticsResponse;
import com.diabetes.assistant.modules.checkin.dto.CheckinSubmitRequest;
import com.diabetes.assistant.modules.checkin.dto.CheckinTaskResponse;
import com.diabetes.assistant.modules.checkin.dto.CheckinTodayResponse;
import com.diabetes.assistant.modules.checkin.entity.CheckinRecord;
import com.diabetes.assistant.modules.checkin.mapper.CheckinRecordMapper;
import com.diabetes.assistant.modules.checkin.service.CheckinService;
import com.diabetes.assistant.modules.lifeplan.contract.LifePlanQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.dto.LifePlanDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private static final String TASK_TYPE_DIET = "diet";
    private static final String TASK_TYPE_EXERCISE = "exercise";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_MISSED = "missed";

    private final CheckinRecordMapper checkinRecordMapper;
    private final LifePlanQueryApi lifePlanQueryApi;
    private final ObjectMapper objectMapper;

    @Override
    public String entry() {
        return "打卡模块功能开发中";
    }

    @Override
    @Transactional
    public CheckinTodayResponse getTodayTasks(Integer userId, LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LifePlanDTO currentPlan = lifePlanQueryApi.getCurrentPlanByUserId(userId);
        if (currentPlan == null || currentPlan.getPlanId() == null) {
            return new CheckinTodayResponse(List.of(), "当前暂无有效生活方案，请先生成生活方案后再进行打卡");
        }

        List<CheckinRecord> existingRecords = listRecordsForDate(userId, currentPlan.getPlanId(), targetDate);
        if (existingRecords.isEmpty()) {
            List<TaskDefinition> taskDefinitions = parseTaskDefinitions(currentPlan.getCheckinTasksJson());
            for (TaskDefinition taskDefinition : taskDefinitions) {
                if (!existsRecord(userId, currentPlan.getPlanId(), targetDate, taskDefinition.taskType())) {
                    CheckinRecord record = new CheckinRecord();
                    record.setUserId(userId);
                    record.setPlanId(currentPlan.getPlanId());
                    record.setTaskType(taskDefinition.taskType());
                    record.setTaskName(taskDefinition.taskName());
                    record.setStatus(STATUS_PENDING);
                    record.setCheckinDate(targetDate);
                    checkinRecordMapper.insert(record);
                }
            }
            existingRecords = listRecordsForDate(userId, currentPlan.getPlanId(), targetDate);
        }

        return new CheckinTodayResponse(existingRecords.stream().map(this::toTaskResponse).toList(), "success");
    }

    @Override
    @Transactional
    public CheckinTaskResponse submitCheckin(Integer userId, CheckinSubmitRequest request) {
        if (!STATUS_COMPLETED.equals(request.getStatus()) && !STATUS_MISSED.equals(request.getStatus())
                && !STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException("打卡状态只能是 pending、completed 或 missed");
        }

        CheckinRecord record = checkinRecordMapper.selectById(request.getCheckinId());
        if (record == null) {
            throw new BusinessException(404, "打卡记录不存在");
        }
        if (!Objects.equals(record.getUserId(), userId)) {
            throw new BusinessException(403, "不能更新非本人的打卡记录");
        }

        record.setStatus(request.getStatus());
        record.setNote(request.getNote());
        record.setCompletedTime(STATUS_COMPLETED.equals(request.getStatus()) ? LocalDateTime.now() : null);
        record.setUpdateTime(LocalDateTime.now());
        checkinRecordMapper.updateById(record);
        return toTaskResponse(record);
    }

    @Override
    public PageResult<CheckinTaskResponse> listHistory(Integer userId, CheckinHistoryQuery query) {
        int page = normalizePage(query.getPage());
        int pageSize = normalizePageSize(query.getPageSize());
        LambdaQueryWrapper<CheckinRecord> wrapper = buildHistoryWrapper(userId, query)
                .orderByDesc(CheckinRecord::getCheckinDate)
                .orderByAsc(CheckinRecord::getTaskType);
        Page<CheckinRecord> result = checkinRecordMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<CheckinTaskResponse> records = result.getRecords().stream().map(this::toTaskResponse).toList();
        return new PageResult<>(records, result.getTotal(), page, pageSize);
    }

    @Override
    public CheckinStatisticsResponse getStatistics(Integer userId, Integer period) {
        int normalizedPeriod = normalizePeriod(period);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(normalizedPeriod - 1L);
        List<CheckinRecord> records = listRecordsInRange(userId, startDate, endDate);

        long dietCompletedDays = records.stream()
                .filter(record -> TASK_TYPE_DIET.equals(record.getTaskType()))
                .filter(record -> STATUS_COMPLETED.equals(record.getStatus()))
                .map(CheckinRecord::getCheckinDate)
                .distinct()
                .count();
        long exerciseCompletedDays = records.stream()
                .filter(record -> TASK_TYPE_EXERCISE.equals(record.getTaskType()))
                .filter(record -> STATUS_COMPLETED.equals(record.getStatus()))
                .map(CheckinRecord::getCheckinDate)
                .distinct()
                .count();
        int totalTaskCount = records.size();
        int completedTaskCount = (int) records.stream()
                .filter(record -> STATUS_COMPLETED.equals(record.getStatus()))
                .count();

        CheckinStatisticsResponse response = new CheckinStatisticsResponse();
        response.setTotalDays(normalizedPeriod);
        response.setDietCompletionCount((int) dietCompletedDays);
        response.setExerciseCompletionCount((int) exerciseCompletedDays);
        response.setTotalTaskCount(totalTaskCount);
        response.setCompletedTaskCount(completedTaskCount);
        response.setCompletionRate(calculateCompletionRate(completedTaskCount, totalTaskCount));
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        return response;
    }

    @Override
    public List<CheckinTaskResponse> listRecentCheckins(Integer userId, Integer period) {
        int normalizedPeriod = normalizePeriod(period);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(normalizedPeriod - 1L);
        return listRecordsInRange(userId, startDate, endDate).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    @Override
    public BigDecimal getRecentCompletionRate(Integer userId, Integer period) {
        return getStatistics(userId, period).getCompletionRate();
    }

    @Override
    public String getLatestCheckinSummaryByUserId(Integer userId, Integer period) {
        CheckinStatisticsResponse statistics = getStatistics(userId, period);
        return String.format("近%d天饮食完成%d天，运动完成%d天，总任务%d项，已完成%d项，完成率%s%%",
                statistics.getTotalDays(),
                statistics.getDietCompletionCount(),
                statistics.getExerciseCompletionCount(),
                statistics.getTotalTaskCount(),
                statistics.getCompletedTaskCount(),
                statistics.getCompletionRate());
    }

    private List<CheckinRecord> listRecordsForDate(Integer userId, Integer planId, LocalDate date) {
        return checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getPlanId, planId)
                .eq(CheckinRecord::getCheckinDate, date)
                .orderByAsc(CheckinRecord::getTaskType));
    }

    private LambdaQueryWrapper<CheckinRecord> buildHistoryWrapper(Integer userId, CheckinHistoryQuery query) {
        return new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .ge(query.getStartDate() != null, CheckinRecord::getCheckinDate, query.getStartDate())
                .le(query.getEndDate() != null, CheckinRecord::getCheckinDate, query.getEndDate())
                .eq(StringUtils.hasText(query.getTaskType()), CheckinRecord::getTaskType, query.getTaskType())
                .eq(StringUtils.hasText(query.getStatus()), CheckinRecord::getStatus, query.getStatus());
    }

    private boolean existsRecord(Integer userId, Integer planId, LocalDate date, String taskType) {
        Long count = checkinRecordMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getPlanId, planId)
                .eq(CheckinRecord::getCheckinDate, date)
                .eq(CheckinRecord::getTaskType, taskType));
        return count != null && count > 0;
    }

    private List<CheckinRecord> listRecordsInRange(Integer userId, LocalDate startDate, LocalDate endDate) {
        return checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .ge(CheckinRecord::getCheckinDate, startDate)
                .le(CheckinRecord::getCheckinDate, endDate)
                .orderByDesc(CheckinRecord::getCheckinDate)
                .orderByAsc(CheckinRecord::getTaskType));
    }

    private List<TaskDefinition> parseTaskDefinitions(String checkinTasksJson) {
        List<TaskDefinition> defaults = List.of(
                new TaskDefinition(TASK_TYPE_DIET, "饮食打卡"),
                new TaskDefinition(TASK_TYPE_EXERCISE, "运动打卡"));
        if (!StringUtils.hasText(checkinTasksJson)) {
            return defaults;
        }

        try {
            JsonNode root = objectMapper.readTree(checkinTasksJson);
            JsonNode tasksNode = root.isArray() ? root : root.get("checkin_tasks");
            if (tasksNode == null || !tasksNode.isArray()) {
                return defaults;
            }

            Map<String, TaskDefinition> taskMap = new LinkedHashMap<>();
            for (JsonNode node : tasksNode) {
                String taskType = node.path("task_type").asText("");
                String taskName = node.path("task_name").asText("");
                if ((TASK_TYPE_DIET.equals(taskType) || TASK_TYPE_EXERCISE.equals(taskType))
                        && StringUtils.hasText(taskName)) {
                    taskMap.putIfAbsent(taskType, new TaskDefinition(taskType, taskName));
                }
            }
            return taskMap.isEmpty() ? defaults : new ArrayList<>(taskMap.values());
        } catch (JsonProcessingException exception) {
            return defaults;
        }
    }

    private CheckinTaskResponse toTaskResponse(CheckinRecord record) {
        CheckinTaskResponse response = new CheckinTaskResponse();
        response.setCheckinId(record.getCheckinId());
        response.setUserId(record.getUserId());
        response.setPlanId(record.getPlanId());
        response.setTaskType(record.getTaskType());
        response.setTaskName(record.getTaskName());
        response.setStatus(record.getStatus());
        response.setNote(record.getNote());
        response.setCheckinDate(record.getCheckinDate());
        response.setCompletedTime(record.getCompletedTime());
        return response;
    }

    private BigDecimal calculateCompletionRate(int completedTaskCount, int totalTaskCount) {
        if (totalTaskCount == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(completedTaskCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalTaskCount), 2, RoundingMode.HALF_UP);
    }

    private int normalizePeriod(Integer period) {
        return period != null && period == 30 ? 30 : 7;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private record TaskDefinition(String taskType, String taskName) {
    }
}
