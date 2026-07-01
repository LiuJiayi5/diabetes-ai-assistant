package com.diabetes.assistant.modules.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_recommendation_logs")
public class ArticleRecommendationLog {

    @TableId(value = "recommendation_id", type = IdType.AUTO)
    private Integer recommendationId;
    private Integer userId;
    private Integer articleId;
    private String scenario;
    private Integer rankNo;
    private Integer score;
    private String sourceSignals;
    private String reasonText;
    private String engineType;
    private String batchKey;
    private LocalDateTime createTime;
}
