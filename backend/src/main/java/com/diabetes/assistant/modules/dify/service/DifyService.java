package com.diabetes.assistant.modules.dify.service;

import com.diabetes.assistant.modules.dify.client.DifyClient;
import com.diabetes.assistant.modules.dify.config.DifyProperties;
import com.diabetes.assistant.modules.dify.dto.DifyWorkflowResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DifyService {

    private final DifyClient difyClient;
    private final DifyProperties difyProperties;
    private final ObjectMapper objectMapper;

    public String callRiskPrediction(Map<String, Object> inputs, String user) {
        DifyWorkflowResult result = difyClient.runWorkflow(difyProperties.getRiskPredictApiKey(), inputs, user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("outputs", result.getOutputs());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        return toJson(response);
    }

    public String callAiDoctor(String message, String conversationId, Map<String, Object> context, String user) {
        return difyClient.sendChatMessage(difyProperties.getAiDoctorApiKey(), message, conversationId, context, user);
    }

    public DifyWorkflowResult callLifePlan(Map<String, Object> inputs, String user) {
        return difyClient.runWorkflow(difyProperties.getLifePlanApiKey(), inputs, user);
    }

    public String callCheckinAnalysis(Map<String, Object> inputs, String user) {
        DifyWorkflowResult result = difyClient.runWorkflow(difyProperties.getCheckinAnalysisApiKey(), inputs, user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("outputs", result.getOutputs());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        return toJson(response);
    }

    public DifyWorkflowResult callInterventionReview(Map<String, Object> inputs, String user) {
        return difyClient.runWorkflow(difyProperties.getInterventionReviewApiKey(), inputs, user);
    }

    public DifyWorkflowResult callComprehensiveReport(Map<String, Object> inputs, String user) {
        return difyClient.runWorkflow(difyProperties.getComprehensiveReportApiKey(), inputs, user);
    }

    private String outputAsText(Map<String, Object> outputs, String preferredKey) {
        if (outputs == null || outputs.isEmpty()) {
            return "{}";
        }
        Object preferred = outputs.get(preferredKey);
        if (preferred != null) {
            return preferred instanceof String text ? text : toJson(preferred);
        }
        return toJson(outputs);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
