package com.diabetes.assistant.modules.lifeplan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_assessments")
public class RiskAssessmentSnapshot {

    @TableId(type = IdType.AUTO)
    private Integer assessmentId;
    private Integer userId;
    private Integer metricId;
    private String riskLevel;
    private Integer riskScore;
    private String diabetesTypeTendency;
    private String mainRiskFactors;
    private String indicatorAnalysis;
    private String healthAdvice;
    private String medicalWarning;
    private String summary;
    private String callStatus;
    private String errorMessage;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String requestSummary;

    @TableField(exist = false)
    private String responseResult;
}
