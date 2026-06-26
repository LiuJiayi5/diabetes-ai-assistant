package com.diabetes.assistant.modules.aichat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.diabetes.assistant.modules.aichat.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiChatMessageMapper extends BaseMapper<AiChatMessage> {
}
