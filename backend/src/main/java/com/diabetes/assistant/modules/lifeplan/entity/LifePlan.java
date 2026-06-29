package com.diabetes.assistant.modules.lifeplan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("life_plans")
public class LifePlan {

    @TableId(type = IdType.AUTO)
    private Integer planId;
    private Integer userId;
    private Integer profileId;
    private Integer metricId;
    private Integer assessmentId;
    private String planTitle;
    private String planGoal;
    private String inputSummary;
    private String planJson;
    private String checkinTasksJson;
    private String summary;
    private String status;
    private String callStatus;
    private String errorMessage;
    private String sourceType;
    private Integer sourceReviewId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
