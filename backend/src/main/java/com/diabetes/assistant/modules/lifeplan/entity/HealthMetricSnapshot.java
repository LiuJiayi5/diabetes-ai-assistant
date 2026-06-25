package com.diabetes.assistant.modules.lifeplan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_metrics")
public class HealthMetricSnapshot {

    @TableId(type = IdType.AUTO)
    private Integer metricId;
    private Integer userId;
    private BigDecimal weightKg;
    private BigDecimal waistCm;
    private Integer systolicBp;
    private Integer diastolicBp;
    private BigDecimal fastingGlucose;
    private BigDecimal postprandialGlucose;
    private BigDecimal hba1c;
    private String dietStatus;
    private String exerciseStatus;
    private LocalDate recordedAt;
    private LocalDateTime createTime;
}
