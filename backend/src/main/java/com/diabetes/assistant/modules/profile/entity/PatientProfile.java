package com.diabetes.assistant.modules.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("patient_profiles")
public class PatientProfile {

    @TableId(type = IdType.AUTO)
    private Integer profileId;
    private Integer userId;
    private Integer age;
    private String gender;
    private BigDecimal heightCm;
    private BigDecimal baseWeightKg;
    private BigDecimal baseWaistCm;
    private String familyHistory;
    private String chronicHistory;
    private String allergyHistory;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
