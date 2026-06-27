package com.diabetes.assistant.modules.aichat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_chat_sessions")
public class AiChatSession {

    @TableId(value = "session_id", type = IdType.AUTO)
    private Integer sessionId;
    private Integer userId;
    private Integer expertId;
    private String sessionTitle;
    private String difyConversationId;
    private String status;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
