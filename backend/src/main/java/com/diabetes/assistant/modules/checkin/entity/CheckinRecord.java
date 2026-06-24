package com.diabetes.assistant.modules.checkin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("checkin_records")
public class CheckinRecord {

    @TableId(value = "checkin_id", type = IdType.AUTO)
    private Integer checkinId;
    private Integer userId;
    private Integer planId;
    private String taskType;
    private String taskName;
    private String status;
    private String note;
    private LocalDate checkinDate;
    private LocalDateTime completedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
