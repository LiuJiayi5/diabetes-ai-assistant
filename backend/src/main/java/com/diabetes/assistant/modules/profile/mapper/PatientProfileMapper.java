package com.diabetes.assistant.modules.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diabetes.assistant.modules.profile.entity.PatientProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientProfileMapper extends BaseMapper<PatientProfile> {
}
