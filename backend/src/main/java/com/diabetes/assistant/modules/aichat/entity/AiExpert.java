package com.diabetes.assistant.modules.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_experts")
public class AiExpert {

    @TableId(value = "expert_id", type = IdType.AUTO)
    private Integer expertId;
    private String expertName;
    private String title;
    private String department;
    private String avatarUrl;
    private String specialty;
    private String persona;
    private String openingMessage;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
