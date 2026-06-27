package com.diabetes.assistant.modules.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("api_call_logs")
public class ApiCallLog {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Integer logId;
    private Integer userId;
    private String serviceType;
    private String requestSummary;
    private String responseSummary;
    private String callStatus;
    private String errorMessage;
    private LocalDateTime createTime;
}
