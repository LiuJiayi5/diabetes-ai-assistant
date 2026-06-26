package com.diabetes.assistant.modules.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("risk_assessments")
public class RiskAssessment {

    @TableId(type = IdType.AUTO)
    private Integer assessmentId;
    private Integer userId;
    private String requestSummary;
    private String responseResult;
    private String riskLevel;
    private Integer riskScore;
    private String callStatus;
    private String errorMessage;
    private LocalDateTime createTime;
}
