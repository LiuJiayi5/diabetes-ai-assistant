package com.diabetes.assistant.modules.checkin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diabetes.assistant.modules.checkin.entity.CheckinRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckinRecordMapper extends BaseMapper<CheckinRecord> {
}
