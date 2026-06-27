package com.diabetes.assistant.modules.dify.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class DifyWorkflowResult {

    private Map<String, Object> outputs;
}
