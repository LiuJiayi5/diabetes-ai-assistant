package com.diabetes.assistant.modules.checkin.contract.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CheckinRecordDTO {

    private Integer checkinId;
    private Integer userId;
    private Integer planId;
    private String taskType;
    private String taskName;
    private String status;
    private String note;
    private LocalDate checkinDate;
    private LocalDateTime completedTime;
    private LocalDateTime createTime;
}
