package com.diabetes.assistant.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.diabetes.assistant.modules.*.mapper")
public class MybatisPlusConfig {
}
