package com.diabetes.assistant.modules.healthmetric.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.security.UserContext;
import com.diabetes.assistant.common.utils.PageUtils;
import com.diabetes.assistant.modules.healthmetric.contract.HealthMetricQueryApi;
import com.diabetes.assistant.modules.healthmetric.contract.dto.HealthMetricDTO;
import com.diabetes.assistant.modules.healthmetric.dto.AdminMetricListItem;
import com.diabetes.assistant.modules.healthmetric.dto.SaveMetricRequest;
import com.diabetes.assistant.modules.healthmetric.dto.SaveMetricResponse;
import com.diabetes.assistant.modules.healthmetric.entity.HealthMetric;
import com.diabetes.assistant.modules.healthmetric.mapper.HealthMetricMapper;
import com.diabetes.assistant.modules.healthmetric.service.HealthMetricService;
import com.diabetes.assistant.modules.healthmetric.util.MetricAbnormalUtils;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import com.diabetes.assistant.modules.user.contract.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthMetricServiceImpl implements HealthMetricService, HealthMetricQueryApi {

    private final HealthMetricMapper healthMetricMapper;
    private final UserQueryApi userQueryApi;

    @Override
    public HealthMetricDTO getEntry() {
        return getLatestMetricByUserId(UserContext.getUserId());
    }

    @Override
    public SaveMetricResponse saveMetric(SaveMetricRequest request) {
        Integer userId = UserContext.getUserId();
        HealthMetric metric = new HealthMetric();
        metric.setUserId(userId);
        metric.setWeightKg(request.getWeightKg());
        metric.setWaistCm(request.getWaistCm());
        metric.setSystolicBp(request.getSystolicBp());
        metric.setDiastolicBp(request.getDiastolicBp());
        metric.setFastingGlucose(request.getFastingGlucose());
        metric.setPostprandialGlucose(request.getPostprandialGlucose());
        metric.setHba1c(request.getHba1c());
        metric.setDietStatus(request.getDietStatus());
        metric.setExerciseStatus(request.getExerciseStatus());
        metric.setRecordedAt(request.getRecordedAt().atStartOfDay());

        if (!MetricAbnormalUtils.hasAnyMetricValue(metric)) {
            throw new BusinessException(400, "请至少填写一项健康指标");
        }

        healthMetricMapper.insert(metric);
        HealthMetric saved = healthMetricMapper.selectById(metric.getMetricId());
        return new SaveMetricResponse(saved.getMetricId(), saved.getCreateTime());
    }

    @Override
    public HealthMetricDTO getLatestMetric() {
        return getLatestMetricByUserId(UserContext.getUserId());
    }

    @Override
    public PageResult<HealthMetricDTO> getHistory(Integer page, Integer pageSize,
                                                LocalDate startDate, LocalDate endDate) {
        Integer userId = UserContext.getUserId();
        int currentPage = PageUtils.normalizePage(page);
        int size = PageUtils.normalizePageSize(pageSize);

        LambdaQueryWrapper<HealthMetric> wrapper = buildUserDateWrapper(userId, startDate, endDate);
        wrapper.orderByDesc(HealthMetric::getRecordedAt);

        Page<HealthMetric> pageResult = healthMetricMapper.selectPage(new Page<>(currentPage, size), wrapper);
        List<HealthMetricDTO> list = pageResult.getRecords().stream().map(this::toDto).toList();
        return new PageResult<>(list, pageResult.getTotal(), currentPage, size);
    }

    @Override
    public PageResult<AdminMetricListItem> adminListMetrics(Integer userId, LocalDate startDate, LocalDate endDate,
                                                           String abnormalOnly, Integer page, Integer pageSize) {
        UserContext.requireAdmin();
        return queryAdminMetrics(userId, startDate, endDate, abnormalOnly, page, pageSize);
    }

    @Override
    public PageResult<AdminMetricListItem> adminListAbnormalMetrics(Integer userId, Integer page, Integer pageSize) {
        UserContext.requireAdmin();
        return queryAdminMetrics(userId, null, null, "yes", page, pageSize);
    }

    @Override
    public HealthMetricDTO getLatestMetricByUserId(Integer userId) {
        HealthMetric metric = findLatestByUserId(userId);
        return metric == null ? null : toDto(metric);
    }

    @Override
    public List<HealthMetricDTO> listMetricsByUserId(Integer userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<HealthMetric> wrapper = buildUserDateWrapper(userId, startDate, endDate);
        wrapper.orderByDesc(HealthMetric::getRecordedAt);
        return healthMetricMapper.selectList(wrapper).stream().map(this::toDto).toList();
    }

    @Override
    public String getLatestMetricSummaryByUserId(Integer userId) {
        HealthMetric metric = findLatestByUserId(userId);
        return metric == null ? null : MetricAbnormalUtils.buildSummary(metric);
    }

    private PageResult<AdminMetricListItem> queryAdminMetrics(Integer userId, LocalDate startDate, LocalDate endDate,
                                                             String abnormalOnly, Integer page, Integer pageSize) {
        int currentPage = PageUtils.normalizePage(page);
        int size = PageUtils.normalizePageSize(pageSize);

        LambdaQueryWrapper<HealthMetric> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(HealthMetric::getUserId, userId);
        }
        applyDateRange(wrapper, startDate, endDate);
        if ("yes".equalsIgnoreCase(abnormalOnly)) {
            applyAbnormalFilter(wrapper);
        }
        wrapper.orderByDesc(HealthMetric::getRecordedAt);

        Page<HealthMetric> pageResult = healthMetricMapper.selectPage(new Page<>(currentPage, size), wrapper);
        List<AdminMetricListItem> list = pageResult.getRecords().stream()
                .map(this::toAdminListItem)
                .toList();
        return new PageResult<>(list, pageResult.getTotal(), currentPage, size);
    }

    private void applyAbnormalFilter(LambdaQueryWrapper<HealthMetric> wrapper) {
        wrapper.and(w -> w
                .lt(HealthMetric::getFastingGlucose, new BigDecimal("3.9"))
                .or().gt(HealthMetric::getFastingGlucose, new BigDecimal("7.0"))
                .or().gt(HealthMetric::getPostprandialGlucose, new BigDecimal("11.1"))
                .or().lt(HealthMetric::getSystolicBp, 90)
                .or().gt(HealthMetric::getSystolicBp, 140)
                .or().lt(HealthMetric::getDiastolicBp, 60)
                .or().gt(HealthMetric::getDiastolicBp, 90));
    }

    private LambdaQueryWrapper<HealthMetric> buildUserDateWrapper(Integer userId,
                                                                  LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<HealthMetric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthMetric::getUserId, userId);
        applyDateRange(wrapper, startDate, endDate);
        return wrapper;
    }

    private void applyDateRange(LambdaQueryWrapper<HealthMetric> wrapper,
                                LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            wrapper.ge(HealthMetric::getRecordedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(HealthMetric::getRecordedAt, endDate.atTime(LocalTime.MAX));
        }
    }

    private HealthMetric findLatestByUserId(Integer userId) {
        return healthMetricMapper.selectOne(new LambdaQueryWrapper<HealthMetric>()
                .eq(HealthMetric::getUserId, userId)
                .orderByDesc(HealthMetric::getRecordedAt)
                .last("LIMIT 1"));
    }

    private HealthMetricDTO toDto(HealthMetric metric) {
        HealthMetricDTO dto = new HealthMetricDTO();
        dto.setMetricId(metric.getMetricId());
        dto.setUserId(metric.getUserId());
        dto.setWeightKg(metric.getWeightKg());
        dto.setWaistCm(metric.getWaistCm());
        dto.setSystolicBp(metric.getSystolicBp());
        dto.setDiastolicBp(metric.getDiastolicBp());
        dto.setFastingGlucose(metric.getFastingGlucose());
        dto.setPostprandialGlucose(metric.getPostprandialGlucose());
        dto.setHba1c(metric.getHba1c());
        dto.setDietStatus(metric.getDietStatus());
        dto.setExerciseStatus(metric.getExerciseStatus());
        dto.setMetricSummary(MetricAbnormalUtils.buildSummary(metric));
        if (metric.getRecordedAt() != null) {
            dto.setRecordedAt(metric.getRecordedAt().toLocalDate());
        }
        dto.setCreateTime(metric.getCreateTime());
        return dto;
    }

    private AdminMetricListItem toAdminListItem(HealthMetric metric) {
        AdminMetricListItem item = new AdminMetricListItem();
        item.setMetricId(metric.getMetricId());
        item.setUserId(metric.getUserId());
        item.setWeightKg(metric.getWeightKg());
        item.setWaistCm(metric.getWaistCm());
        item.setSystolicBp(metric.getSystolicBp());
        item.setDiastolicBp(metric.getDiastolicBp());
        item.setFastingGlucose(metric.getFastingGlucose());
        item.setPostprandialGlucose(metric.getPostprandialGlucose());
        item.setHba1c(metric.getHba1c());
        item.setDietStatus(metric.getDietStatus());
        item.setExerciseStatus(metric.getExerciseStatus());
        if (metric.getRecordedAt() != null) {
            item.setRecordedAt(metric.getRecordedAt().toLocalDate());
        }
        item.setCreateTime(metric.getCreateTime());

        UserBasicDTO user = userQueryApi.getUserBasicById(metric.getUserId());
        if (user != null) {
            item.setUsername(user.getUsername());
        }
        return item;
    }
}
