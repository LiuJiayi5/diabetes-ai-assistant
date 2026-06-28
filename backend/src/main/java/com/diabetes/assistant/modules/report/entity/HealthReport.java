package com.diabetes.assistant.modules.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("health_reports")
public class HealthReport {

    @TableId(type = IdType.AUTO)
    private Integer reportId;
    private Integer userId;
    private String reportType;
    private String reportTitle;
    private String reportMarkdown;
    private String reportSummary;
    private String dataSnapshotJson;
    private Integer completenessScore;
    private String reportStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
