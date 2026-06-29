package com.diabetes.assistant.modules.interventionreview.service;

public interface InterventionReviewTriggerService {

    void triggerAsync(Integer userId, String triggerType, String triggerReason);

    void triggerAfterCommit(Integer userId, String triggerType, String triggerReason);
}
