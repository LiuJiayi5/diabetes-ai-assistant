package com.diabetes.assistant.modules.dify.service;

import com.diabetes.assistant.modules.dify.client.DifyClient;
import com.diabetes.assistant.modules.dify.config.DifyProperties;
import com.diabetes.assistant.modules.dify.dto.DifyWorkflowResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DifyService {

    private final DifyClient difyClient;
    private final DifyProperties difyProperties;

    public DifyWorkflowResult callRiskPrediction(Map<String, Object> inputs, String user) {
        return difyClient.runWorkflow(difyProperties.getRiskPredictApiKey(), inputs, user);
    }

    public String callAiDoctor(String message, String conversationId, Map<String, Object> context, String user) {
        return difyClient.sendChatMessage(difyProperties.getAiDoctorApiKey(), message, conversationId, context, user);
    }

    public DifyWorkflowResult callLifePlan(Map<String, Object> inputs, String user) {
        return difyClient.runWorkflow(difyProperties.getLifePlanApiKey(), inputs, user);
    }

    public DifyWorkflowResult callCheckinAnalysis(Map<String, Object> inputs, String user) {
        return difyClient.runWorkflow(difyProperties.getCheckinAnalysisApiKey(), inputs, user);
    }
}
