package com.diabetes.assistant.modules.checkin.dto.admin;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminLogQuery {

    private Integer page;
    private Integer pageSize;
    private Integer userId;
    private String patientKeyword;
    private LocalDate startDate;
    private LocalDate endDate;
    private String callStatus;
}
