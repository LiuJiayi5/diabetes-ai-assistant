package com.diabetes.assistant.modules.risk.util;

import com.diabetes.assistant.modules.risk.dto.PatientSimilarCaseItem;
import com.diabetes.assistant.modules.risk.dto.SimilarCaseItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatientSimilarCaseMapperTest {

    @Test
    void toPatientItem_stripsIdentityFields() {
        SimilarCaseItem source = new SimilarCaseItem();
        source.setUserId(4);
        source.setUsername("he_yan");
        source.setAge(59);
        source.setGender("女");
        source.setSimilarityScore(72);
        source.setMatchReason("空腹血糖接近");
        source.setInterventionSummary("方案《何燕的血压血糖综合管理计划》；慢走、少盐");
        source.setOutcomeSummary("空腹血糖由 7.1 降至 6.5 mmol/L");
        source.setCheckinCompletionRate(83);

        PatientSimilarCaseItem item = PatientSimilarCaseMapper.toPatientItem(1, source);

        assertEquals("参考案例 1", item.getCaseLabel());
        assertEquals(72, item.getSimilarityScore());
        assertEquals("空腹血糖接近", item.getMatchReason());
        assertEquals("方案《血压血糖综合管理计划》；慢走、少盐", item.getInterventionSummary());
        assertEquals("空腹血糖由 7.1 降至 6.5 mmol/L", item.getOutcomeSummary());
        assertEquals(83, item.getCheckinCompletionRate());
    }
}
