package com.diabetes.assistant.modules.interventionreview.service.impl;

import com.diabetes.assistant.modules.interventionreview.service.InterventionReviewService;
import com.diabetes.assistant.modules.interventionreview.service.InterventionReviewTriggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.concurrent.Executor;

@Slf4j
@Service
public class InterventionReviewTriggerServiceImpl implements InterventionReviewTriggerService {

    private final ObjectProvider<InterventionReviewService> interventionReviewServiceProvider;
    private final Executor interventionReviewTaskExecutor;

    public InterventionReviewTriggerServiceImpl(
            ObjectProvider<InterventionReviewService> interventionReviewServiceProvider,
            @Qualifier("interventionReviewTaskExecutor") Executor interventionReviewTaskExecutor) {
        this.interventionReviewServiceProvider = interventionReviewServiceProvider;
        this.interventionReviewTaskExecutor = interventionReviewTaskExecutor;
    }

    @Override
    public void triggerAsync(Integer userId, String triggerType, String triggerReason) {
        if (userId == null || !StringUtils.hasText(triggerType)) {
            return;
        }
        interventionReviewTaskExecutor.execute(() -> runReview(userId, triggerType, triggerReason));
    }

    private void runReview(Integer userId, String triggerType, String triggerReason) {
        try {
            InterventionReviewService interventionReviewService = interventionReviewServiceProvider.getIfAvailable();
            if (interventionReviewService == null) {
                return;
            }
            interventionReviewService.tryAutoReview(userId, triggerType, triggerReason);
        } catch (Exception exception) {
            log.warn("Async intervention review trigger failed: userId={}, triggerType={}, error={}",
                    userId, triggerType, exception.getMessage());
        }
    }

    @Override
    public void triggerAfterCommit(Integer userId, String triggerType, String triggerReason) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            triggerAsync(userId, triggerType, triggerReason);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                triggerAsync(userId, triggerType, triggerReason);
            }
        });
    }
}
