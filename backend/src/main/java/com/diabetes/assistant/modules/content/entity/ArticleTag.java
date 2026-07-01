package com.diabetes.assistant.modules.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_tags")
public class ArticleTag {

    @TableId(value = "tag_id", type = IdType.AUTO)
    private Integer tagId;
    private Integer articleId;
    private String tagCode;
    private String tagName;
    private String tagType;
    private Integer weight;
    private LocalDateTime createTime;
}
