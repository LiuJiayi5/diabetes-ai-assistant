package com.diabetes.assistant.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("email_verification_codes")
public class EmailVerificationCode {

    @TableId(value = "code_id", type = IdType.AUTO)
    private Integer codeId;
    private String email;
    private String purpose;
    private String codeHash;
    private Boolean used;
    private LocalDateTime expiresAt;
    private LocalDateTime createTime;
    private LocalDateTime usedTime;
}
