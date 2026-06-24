package com.diabetes.assistant.modules.checkin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckinSubmitRequest {

    @NotNull(message = "打卡记录ID不能为空")
    @JsonProperty("checkin_id")
    private Integer checkinId;

    @NotBlank(message = "打卡状态不能为空")
    private String status;

    private String note;
}
