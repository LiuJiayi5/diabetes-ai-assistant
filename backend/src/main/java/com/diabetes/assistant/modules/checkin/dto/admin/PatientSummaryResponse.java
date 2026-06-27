package com.diabetes.assistant.modules.checkin.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PatientSummaryResponse {

    @JsonProperty("user_id")
    private Integer userId;
    private String username;
    private String phone;
    private String email;
    private String status;
    @JsonProperty("profile_summary")
    private String profileSummary;
}
