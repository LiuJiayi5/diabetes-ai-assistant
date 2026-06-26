package com.diabetes.assistant.modules.profile.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminProfileListItem {

    private Integer profileId;
    private Integer userId;
    private String username;
    private Integer age;
    private String gender;
    private BigDecimal heightCm;
    private BigDecimal baseWeightKg;
    private LocalDateTime updateTime;
}
