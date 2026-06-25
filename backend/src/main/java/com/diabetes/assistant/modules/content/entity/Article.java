package com.diabetes.assistant.modules.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("articles")
public class Article {

    @TableId(type = IdType.AUTO)
    private Integer articleId;
    private String title;
    private String category;
    private String coverImage;
    private String summary;
    private String content;
    private String status;
    private Integer viewCount;
    private Integer isRecommended;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
