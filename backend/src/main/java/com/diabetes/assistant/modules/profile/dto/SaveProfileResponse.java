package com.diabetes.assistant.modules.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SaveProfileResponse {

    private Integer profileId;
    private LocalDateTime updateTime;
}
