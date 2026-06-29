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
import com.diabetes.assistant.modules.interventionreview.service.InterventionReviewTriggerService;
import com.diabetes.assistant.modules.lifeplan.contract.LifePlanQueryApi;
import com.diabetes.assistant.modules.lifeplan.contract.dto.LifePlanDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.time.temporal.ChronoUnit;
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
    private final InterventionReviewTriggerService interventionReviewTriggerService;

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
            CheckinTodayResponse response = new CheckinTodayResponse();
            response.setList(List.of());
            response.setMessage("当前暂无有效生活方案，请先生成生活方案后再进行打卡");
            return response;
        }

        PlanExecutionInfo executionInfo = buildPlanExecutionInfo(currentPlan, targetDate);
        List<TaskDefinition> taskDefinitions = buildTaskDefinitions(currentPlan.getCheckinTasksJson(), executionInfo.todaySchedule());
        List<CheckinRecord> existingRecords = selectDailyTaskRecords(listRecordsForDate(userId, targetDate));
        if (!existingRecords.isEmpty()) {
            syncRecordsToCurrentPlan(existingRecords, currentPlan.getPlanId(), taskDefinitions);
            existingRecords = selectDailyTaskRecords(listRecordsForDate(userId, targetDate));
        }
        if (!executionInfo.expired()) {
            for (TaskDefinition taskDefinition : taskDefinitions) {
                if (!existsRecord(userId, targetDate, taskDefinition.taskType())) {
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
            existingRecords = selectDailyTaskRecords(listRecordsForDate(userId, targetDate));
        }

        CheckinTodayResponse response = buildTodayResponse(currentPlan, executionInfo, existingRecords);
        response.setMessage(executionInfo.expired()
                ? "当前生活方案周期已结束，可以先查看复盘或生成新的调整方案"
                : "success");
        return response;
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
        triggerInterventionReviewAfterDailyCheckinCompleted(userId, record);
        return toTaskResponse(record);
    }

    private void triggerInterventionReviewAfterDailyCheckinCompleted(Integer userId, CheckinRecord record) {
        if (record.getPlanId() == null || record.getCheckinDate() == null) {
            return;
        }
        if (!isDailyCheckinCompleted(userId, record.getPlanId(), record.getCheckinDate())) {
            return;
        }
        triggerInterventionReview(userId, "checkin_submit", "User submitted complete daily check-in");
    }

    private boolean isDailyCheckinCompleted(Integer userId, Integer planId, LocalDate date) {
        List<CheckinRecord> records = listRecordsForDate(userId, date);
        boolean dietSubmitted = false;
        boolean exerciseSubmitted = false;
        for (CheckinRecord item : records) {
            if (TASK_TYPE_DIET.equals(item.getTaskType())) {
                dietSubmitted = !STATUS_PENDING.equals(item.getStatus());
            }
            if (TASK_TYPE_EXERCISE.equals(item.getTaskType())) {
                exerciseSubmitted = !STATUS_PENDING.equals(item.getStatus());
            }
        }
        return dietSubmitted && exerciseSubmitted;
    }

    private void triggerInterventionReview(Integer userId, String triggerType, String triggerReason) {
        interventionReviewTriggerService.triggerAfterCommit(userId, triggerType, triggerReason);
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

    private List<CheckinRecord> listRecordsForDate(Integer userId, LocalDate date) {
        return checkinRecordMapper.selectList(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .eq(CheckinRecord::getCheckinDate, date)
                .orderByAsc(CheckinRecord::getTaskType)
                .orderByDesc(CheckinRecord::getUpdateTime)
                .orderByDesc(CheckinRecord::getCheckinId));
    }

    private List<CheckinRecord> selectDailyTaskRecords(List<CheckinRecord> records) {
        Map<String, CheckinRecord> selected = new LinkedHashMap<>();
        for (CheckinRecord record : records) {
            if (record == null || !StringUtils.hasText(record.getTaskType())) {
                continue;
            }
            CheckinRecord current = selected.get(record.getTaskType());
            if (current == null || isBetterDailyRecord(record, current)) {
                selected.put(record.getTaskType(), record);
            }
        }
        return new ArrayList<>(selected.values());
    }

    private boolean isBetterDailyRecord(CheckinRecord candidate, CheckinRecord current) {
        int candidateRank = statusRank(candidate.getStatus());
        int currentRank = statusRank(current.getStatus());
        if (candidateRank != currentRank) {
            return candidateRank > currentRank;
        }
        LocalDateTime candidateTime = candidate.getUpdateTime() != null ? candidate.getUpdateTime() : candidate.getCreateTime();
        LocalDateTime currentTime = current.getUpdateTime() != null ? current.getUpdateTime() : current.getCreateTime();
        if (candidateTime != null && currentTime != null && !candidateTime.equals(currentTime)) {
            return candidateTime.isAfter(currentTime);
        }
        if (candidateTime != null && currentTime == null) {
            return true;
        }
        Integer candidateId = candidate.getCheckinId();
        Integer currentId = current.getCheckinId();
        return candidateId != null && currentId != null && candidateId > currentId;
    }

    private int statusRank(String status) {
        if (STATUS_COMPLETED.equals(status) || STATUS_MISSED.equals(status)) {
            return 2;
        }
        if (STATUS_PENDING.equals(status)) {
            return 1;
        }
        return 0;
    }

    private void syncRecordsToCurrentPlan(List<CheckinRecord> records, Integer planId, List<TaskDefinition> taskDefinitions) {
        Map<String, String> taskNames = new LinkedHashMap<>();
        for (TaskDefinition taskDefinition : taskDefinitions) {
            taskNames.put(taskDefinition.taskType(), taskDefinition.taskName());
        }
        for (CheckinRecord record : records) {
            boolean changed = false;
            if (!Objects.equals(record.getPlanId(), planId)) {
                record.setPlanId(planId);
                changed = true;
            }
            String taskName = taskNames.get(record.getTaskType());
            if (StringUtils.hasText(taskName) && !Objects.equals(record.getTaskName(), taskName)) {
                record.setTaskName(taskName);
                changed = true;
            }
            if (changed) {
                record.setUpdateTime(LocalDateTime.now());
                checkinRecordMapper.updateById(record);
            }
        }
    }

    private LambdaQueryWrapper<CheckinRecord> buildHistoryWrapper(Integer userId, CheckinHistoryQuery query) {
        return new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
                .ge(query.getStartDate() != null, CheckinRecord::getCheckinDate, query.getStartDate())
                .le(query.getEndDate() != null, CheckinRecord::getCheckinDate, query.getEndDate())
                .eq(StringUtils.hasText(query.getTaskType()), CheckinRecord::getTaskType, query.getTaskType())
                .eq(StringUtils.hasText(query.getStatus()), CheckinRecord::getStatus, query.getStatus());
    }

    private boolean existsRecord(Integer userId, LocalDate date, String taskType) {
        Long count = checkinRecordMapper.selectCount(new LambdaQueryWrapper<CheckinRecord>()
                .eq(CheckinRecord::getUserId, userId)
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

    private List<TaskDefinition> buildTaskDefinitions(String checkinTasksJson, Map<String, Object> todaySchedule) {
        List<TaskDefinition> definitions = parseTaskDefinitions(checkinTasksJson);
        List<TaskDefinition> enriched = new ArrayList<>();
        for (TaskDefinition definition : definitions) {
            String advice = extractTaskAdvice(definition.taskType(), todaySchedule);
            enriched.add(new TaskDefinition(definition.taskType(), buildTaskName(definition.taskType(), advice, definition.taskName())));
        }
        return enriched;
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
        return toTaskResponse(record, null);
    }

    private CheckinTaskResponse toTaskResponse(CheckinRecord record, Map<String, Object> todaySchedule) {
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
        response.setPlanAdvice(extractTaskAdvice(record.getTaskType(), todaySchedule));
        return response;
    }

    private CheckinTodayResponse buildTodayResponse(LifePlanDTO plan, PlanExecutionInfo executionInfo,
                                                    List<CheckinRecord> records) {
        CheckinTodayResponse response = new CheckinTodayResponse();
        response.setList(records.stream()
                .map(record -> toTaskResponse(record, executionInfo.todaySchedule()))
                .toList());
        response.setPlanId(plan.getPlanId());
        response.setPlanTitle(plan.getPlanTitle());
        response.setPlanGoal(plan.getPlanGoal());
        response.setPlanSummary(plan.getSummary());
        response.setPlanCreateTime(plan.getCreateTime());
        response.setPlanDay(executionInfo.day());
        response.setTotalPlanDays(executionInfo.totalDays());
        response.setPlanExpired(executionInfo.expired());
        response.setTodaySchedule(executionInfo.todaySchedule());
        response.setTodayFocus(extractTodayFocus(executionInfo.todaySchedule(), executionInfo.day()));
        return response;
    }

    private PlanExecutionInfo buildPlanExecutionInfo(LifePlanDTO plan, LocalDate targetDate) {
        List<JsonNode> scheduleDays = extractScheduleDays(plan.getDailyScheduleJson());
        int totalDays = totalPlanDays(scheduleDays);
        LocalDate startDate = plan.getCreateTime() == null ? targetDate : plan.getCreateTime().toLocalDate();
        int day = (int) ChronoUnit.DAYS.between(startDate, targetDate) + 1;
        boolean expired = day > totalDays;
        List<JsonNode> todayNodes = selectScheduleNodes(scheduleDays, day);
        return new PlanExecutionInfo(day, totalDays, expired, toScheduleMap(todayNodes));
    }

    private List<JsonNode> extractScheduleDays(String scheduleJson) {
        if (!StringUtils.hasText(scheduleJson)) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(scheduleJson);
            JsonNode node = unwrapScheduleNode(root);
            if (node == null || node.isMissingNode() || node.isNull()) {
                return List.of();
            }
            if (node.isArray()) {
                List<JsonNode> result = new ArrayList<>();
                node.forEach(result::add);
                return result;
            }
            if (node.isObject()) {
                List<JsonNode> result = new ArrayList<>();
                node.fields().forEachRemaining(entry -> result.add(entry.getValue()));
                return result;
            }
            return List.of(node);
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private int totalPlanDays(List<JsonNode> scheduleDays) {
        if (scheduleDays == null || scheduleDays.isEmpty()) {
            return 7;
        }
        int maxDay = 0;
        for (JsonNode node : scheduleDays) {
            maxDay = Math.max(maxDay, readDayNumber(node));
        }
        if (maxDay > 0) {
            return maxDay;
        }
        return Math.min(Math.max(scheduleDays.size(), 1), 30);
    }

    private JsonNode unwrapScheduleNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        for (String key : List.of("daily_schedule", "weekly_schedule", "weekly_plan", "days", "plan_days", "dailySchedule")) {
            JsonNode child = node.get(key);
            if (child != null && !child.isMissingNode() && !child.isNull()) {
                return unwrapScheduleNode(child);
            }
        }
        return node;
    }

    private List<JsonNode> selectScheduleNodes(List<JsonNode> scheduleDays, int day) {
        if (scheduleDays.isEmpty()) {
            return List.of();
        }
        List<JsonNode> matched = new ArrayList<>();
        for (JsonNode node : scheduleDays) {
            int nodeDay = readDayNumber(node);
            if (nodeDay == day) {
                matched.add(node);
            }
        }
        if (!matched.isEmpty()) {
            return matched;
        }
        int index = Math.max(0, Math.min(day - 1, scheduleDays.size() - 1));
        return List.of(scheduleDays.get(index));
    }

    private int readDayNumber(JsonNode node) {
        if (node == null || !node.isObject()) {
            return -1;
        }
        for (String key : List.of("day", "day_index", "dayIndex")) {
            JsonNode value = node.get(key);
            if (value != null && value.canConvertToInt()) {
                return value.asInt();
            }
        }
        return -1;
    }

    private Map<String, Object> toScheduleMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        try {
            if (node.isObject()) {
                return objectMapper.convertValue(node, new TypeReference<>() {});
            }
            return Map.of("content", node.asText(""));
        } catch (IllegalArgumentException exception) {
            return Map.of();
        }
    }

    private Map<String, Object> toScheduleMap(List<JsonNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Map.of();
        }
        if (nodes.size() == 1) {
            return toScheduleMap(nodes.get(0));
        }
        List<Map<String, Object>> items = nodes.stream()
                .map(this::toScheduleMap)
                .filter(item -> !item.isEmpty())
                .toList();
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put("items", items);
        for (String key : List.of("diet_plan", "diet", "meals", "exercise_plan", "exercise", "reminder", "goal")) {
            List<String> values = distinctTextList(items.stream()
                    .map(item -> stringifyScheduleValue(item.get(key)))
                    .filter(StringUtils::hasText)
                    .toList());
            if (!values.isEmpty()) {
                merged.put(key, String.join("；", values));
            }
        }
        return merged;
    }

    private String extractTaskAdvice(String taskType, Map<String, Object> todaySchedule) {
        if (todaySchedule == null || todaySchedule.isEmpty()) {
            return null;
        }
        if (TASK_TYPE_DIET.equals(taskType)) {
            String text = firstText(todaySchedule, "diet", "diet_advice", "diet_plan", "dietPlan", "meal", "meals",
                    "breakfast", "lunch", "dinner", "snack");
            if (StringUtils.hasText(text)) {
                return text;
            }
            return extractTaskContentByKeywords(todaySchedule, List.of("早餐", "午餐", "晚餐", "加餐", "饮食", "主食"));
        }
        if (TASK_TYPE_EXERCISE.equals(taskType)) {
            String text = firstText(todaySchedule, "exercise", "exercise_advice", "exercise_plan", "exercisePlan", "sport", "training");
            return StringUtils.hasText(text) ? text : firstText(todaySchedule, "reminder", "goal", "content");
        }
        return null;
    }

    private String buildTaskName(String taskType, String advice, String fallback) {
        String compact = compactAdvice(advice);
        if (TASK_TYPE_DIET.equals(taskType)) {
            return StringUtils.hasText(compact) ? "饮食打卡：" + compact : fallback;
        }
        if (TASK_TYPE_EXERCISE.equals(taskType)) {
            return StringUtils.hasText(compact) ? "运动打卡：" + compact : fallback;
        }
        return fallback;
    }

    private String compactAdvice(String advice) {
        if (!StringUtils.hasText(advice)) {
            return "";
        }
        String compact = advice.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 34) {
            return compact;
        }
        return compact.substring(0, 34) + "...";
    }

    private String extractTodayFocus(Map<String, Object> todaySchedule, int day) {
        String title = firstText(todaySchedule, "title", "name");
        String reminder = firstText(todaySchedule, "reminder", "tip", "health_tip", "work_rest", "sleep", "content");
        if (StringUtils.hasText(title) && StringUtils.hasText(reminder)) {
            return title + ": " + reminder;
        }
        if (StringUtils.hasText(reminder)) {
            return reminder;
        }
        return day > 0 ? "第 " + day + " 天，按今日生活方案完成饮食与运动打卡" : "按当前生活方案完成饮食与运动打卡";
    }

    private String extractTaskContentByKeywords(Object source, List<String> keywords) {
        if (source instanceof Map<?, ?> map) {
            String taskText = stringifyScheduleValue(map.get("task"));
            String contentText = stringifyScheduleValue(map.get("content"));
            if (containsAny(taskText, keywords) && StringUtils.hasText(contentText)) {
                return contentText;
            }
            for (Object value : map.values()) {
                String nested = extractTaskContentByKeywords(value, keywords);
                if (StringUtils.hasText(nested)) {
                    return nested;
                }
            }
        }
        if (source instanceof List<?> list) {
            List<String> values = list.stream()
                    .map(item -> extractTaskContentByKeywords(item, keywords))
                    .filter(StringUtils::hasText)
                    .toList();
            return values.isEmpty() ? null : String.join("；", values);
        }
        return null;
    }

    private boolean containsAny(String value, List<String> keywords) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private List<String> distinctTextList(List<String> values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            String text = value == null ? "" : value.trim();
            if (StringUtils.hasText(text) && !result.contains(text)) {
                result.add(text);
            }
        }
        return result;
    }

    private String firstText(Map<String, Object> source, String... keys) {
        if (source == null) {
            return null;
        }
        for (String key : keys) {
            String text = stringifyScheduleValue(source.get(key));
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private String stringifyScheduleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof Map<?, ?> map) {
            return map.values().stream()
                    .map(this::stringifyScheduleValue)
                    .filter(StringUtils::hasText)
                    .reduce((left, right) -> left + "; " + right)
                    .orElse(null);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(this::stringifyScheduleValue)
                    .filter(StringUtils::hasText)
                    .reduce((left, right) -> left + "; " + right)
                    .orElse(null);
        }
        return String.valueOf(value);
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

    private record PlanExecutionInfo(int day, int totalDays, boolean expired, Map<String, Object> todaySchedule) {
    }
}
