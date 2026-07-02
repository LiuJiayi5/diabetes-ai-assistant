package com.diabetes.assistant.modules.risk.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SimilarCaseAnonymizerTest {

    @Test
    void anonymizeInterventionSummary_removesNameFromPlanTitle() {
        String raw = "方案《何燕的血压血糖综合管理计划》；以安全和可持续为先，把慢走、少盐和复查准备做好；近7天打卡完成率 83%";
        String sanitized = SimilarCaseAnonymizer.anonymizeInterventionSummary(raw);

        assertFalse(sanitized.contains("何燕"));
        assertEquals("方案《血压血糖综合管理计划》；以安全和可持续为先，把慢走、少盐和复查准备做好；近7天打卡完成率 83%", sanitized);
    }

    @Test
    void anonymizeInterventionSummary_removesNameFromSecondCase() {
        String raw = "方案《赵晴的稳定随访计划》；当前状态较好，重点是避免因为工作压力打乱节奏；近7天打卡完成率 88%";
        String sanitized = SimilarCaseAnonymizer.anonymizeInterventionSummary(raw);

        assertFalse(sanitized.contains("赵晴"));
        assertEquals("方案《稳定随访计划》；当前状态较好，重点是避免因为工作压力打乱节奏；近7天打卡完成率 88%", sanitized);
    }
}
