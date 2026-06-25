package com.diabetes.assistant.modules.checkin.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminApiCallLogResponse {

    @JsonProperty("log_id")
    private Integer logId;
    @JsonProperty("user_id")
    private Integer userId;
    @JsonProperty("service_type")
    private String serviceType;
    @JsonProperty("request_summary")
    private String requestSummary;
    @JsonProperty("response_summary")
    private String responseSummary;
    @JsonProperty("call_status")
    private String callStatus;
    @JsonProperty("error_message")
    private String errorMessage;
    @JsonProperty("create_time")
    private LocalDateTime createTime;
    private PatientSummaryResponse patient;
}
