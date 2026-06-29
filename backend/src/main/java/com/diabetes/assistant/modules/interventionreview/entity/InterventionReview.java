package com.diabetes.assistant.modules.interventionreview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("intervention_reviews")
public class InterventionReview {

    @TableId(value = "review_id", type = IdType.AUTO)
    private Integer reviewId;
    private Integer userId;
    private Integer planId;
    private Integer sourceReviewId;
    private String triggerType;
    private String triggerReason;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private Integer reviewDays;
    private Integer planDay;
    private Integer adherenceScore;
    private String interventionLevel;
    private Boolean shouldUpdatePlan;
    private String updateScope;
    private String affectedDays;
    private String mainProblemTags;
    private String preservedItems;
    private String changedItems;
    private String adjustmentStrategy;
    private String patientNotice;
    private String explanation;
    private String safetyWarning;
    private String adjustedPlanPatch;
    private Integer generatedPlanId;
    private String inputSummary;
    private String rawResponse;
    private String callStatus;
    private String errorMessage;
    private LocalDateTime createTime;
}
