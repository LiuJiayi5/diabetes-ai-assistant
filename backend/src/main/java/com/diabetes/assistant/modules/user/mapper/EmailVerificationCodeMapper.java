package com.diabetes.assistant.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diabetes.assistant.modules.user.entity.EmailVerificationCode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailVerificationCodeMapper extends BaseMapper<EmailVerificationCode> {
}
