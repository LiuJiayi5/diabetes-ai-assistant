package com.diabetes.assistant.modules.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_read_events")
public class ArticleReadEvent {

    @TableId(value = "event_id", type = IdType.AUTO)
    private Integer eventId;
    private Integer userId;
    private Integer articleId;
    private Integer recommendationId;
    private String sourceScenario;
    private Integer readSeconds;
    private Integer progressPercent;
    private LocalDateTime createTime;
}
