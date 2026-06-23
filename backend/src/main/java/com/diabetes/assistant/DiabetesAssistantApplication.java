package com.diabetes.assistant;

import com.diabetes.assistant.modules.dify.config.DifyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DifyProperties.class)
public class DiabetesAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiabetesAssistantApplication.class, args);
    }
}
