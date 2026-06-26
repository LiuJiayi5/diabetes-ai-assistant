package com.diabetes.assistant.modules.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("home_contents")
public class HomeContent {

    @TableId(type = IdType.AUTO)
    private Integer contentId;
    private String contentType;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkType;
    private String linkValue;
    private Integer sortOrder;
    private String status;
    private Integer createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
