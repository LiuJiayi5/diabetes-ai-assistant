package com.diabetes.assistant.modules.interventionreview.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterventionReviewResponse {

    private Integer reviewId;
    private Integer userId;
    private Integer planId;
    private Integer generatedPlanId;
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
    private List<Integer> affectedDays;
    private List<String> mainProblemTags;
    private List<String> preservedItems;
    private List<String> changedItems;
    private String adjustmentStrategy;
    private String patientNotice;
    private String explanation;
    private String safetyWarning;
    private Object adjustedPlanPatch;
    private String callStatus;
    private String errorMessage;
    private LocalDateTime createTime;
}
