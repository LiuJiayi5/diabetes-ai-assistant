package com.diabetes.assistant.modules.risk.util;

import com.diabetes.assistant.modules.risk.dto.PatientSimilarCaseItem;
import com.diabetes.assistant.modules.risk.dto.SimilarCaseItem;

public final class PatientSimilarCaseMapper {

    private PatientSimilarCaseMapper() {
    }

    public static PatientSimilarCaseItem toPatientItem(int index, SimilarCaseItem source) {
        PatientSimilarCaseItem item = new PatientSimilarCaseItem();
        item.setCaseLabel("参考案例 " + index);
        item.setSimilarityScore(source.getSimilarityScore());
        item.setMatchReason(source.getMatchReason());
        item.setInterventionSummary(SimilarCaseAnonymizer.anonymizeInterventionSummary(source.getInterventionSummary()));
        item.setOutcomeSummary(source.getOutcomeSummary());
        item.setCheckinCompletionRate(source.getCheckinCompletionRate());
        return item;
    }
}
