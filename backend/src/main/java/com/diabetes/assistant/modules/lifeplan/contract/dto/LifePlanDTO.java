package com.diabetes.assistant.modules.lifeplan.contract.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LifePlanDTO {

    private Integer planId;
    private Integer userId;
    private Integer assessmentId;
    private String planTitle;
    private String planGoal;
    private String dietPlanJson;
    private String exercisePlanJson;
    private String dailyScheduleJson;
    private String checkinTasksJson;
    private String healthTipsJson;
    private String summary;
    private String status;
    private String callStatus;
    private String errorMessage;
    private LocalDateTime createTime;
}
