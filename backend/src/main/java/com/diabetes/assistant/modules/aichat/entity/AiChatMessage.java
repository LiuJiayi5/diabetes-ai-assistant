package com.diabetes.assistant.modules.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_messages")
public class AiChatMessage {

    @TableId(value = "message_id", type = IdType.AUTO)
    private Integer messageId;
    private Integer sessionId;
    private Integer userId;
    private String userMessage;
    private String aiResponse;
    private String contextSummary;
    private String callStatus;
    private String errorMessage;
    private LocalDateTime createTime;
}
