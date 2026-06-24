package com.diabetes.assistant.modules.checkin.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CheckinHistoryQuery {

    private Integer page = 1;
    private Integer pageSize = 10;
    private LocalDate startDate;
    private LocalDate endDate;
    private String taskType;
    private String status;
}
