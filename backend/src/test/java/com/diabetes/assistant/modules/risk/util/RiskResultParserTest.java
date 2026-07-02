package com.diabetes.assistant.modules.risk.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskResultParserTest {

    @Test
    void parse_plainJsonObject() {
        String raw = """
                {
                  "risk_level": "medium",
                  "risk_score": 64,
                  "diabetes_type_tendency": "2型倾向",
                  "main_risk_factors": ["家族史", "空腹血糖偏高"],
                  "indicator_analysis": "指标分析",
                  "health_advice": "建议",
                  "medical_warning": "提醒",
                  "summary": "总结",
                  "reference_sources": ["筛查诊断指标", "风险因素预防"]
                }
                """;
        RiskResultParser.ParsedRiskResult result = RiskResultParser.parse(raw);
        assertEquals("medium", result.getRiskLevel());
        assertEquals(64, result.getRiskScore());
        assertEquals(2, result.getMainRiskFactors().size());
        assertEquals(2, result.getReferenceSources().size());
    }

    @Test
    void parse_difyWorkflowOutputsText() {
        String raw = """
                {
                  "data": {
                    "outputs": {
                      "text": "{\\"risk_level\\":\\"high\\",\\"risk_score\\":82,\\"diabetes_type_tendency\\":\\"2型\\",\\"main_risk_factors\\":[\\"BMI偏高\\"],\\"indicator_analysis\\":\\"a\\",\\"health_advice\\":\\"b\\",\\"medical_warning\\":\\"c\\",\\"summary\\":\\"d\\",\\"reference_sources\\":[\\"知识1\\"]}"
                    }
                  }
                }
                """;
        RiskResultParser.ParsedRiskResult result = RiskResultParser.parse(raw);
        assertEquals("high", result.getRiskLevel());
        assertEquals(82, result.getRiskScore());
        assertNotNull(result.getReferenceSources());
    }

    @Test
    void parse_stripsMarkdownCodeBlock() {
        String raw = """
                ```json
                {"risk_level":"low","risk_score":30,"diabetes_type_tendency":"t","main_risk_factors":["a","b"],"indicator_analysis":"i","health_advice":"h","medical_warning":"m","summary":"s","reference_sources":["r"]}
                ```
                """;
        RiskResultParser.ParsedRiskResult result = RiskResultParser.parse(raw);
        assertEquals("low", result.getRiskLevel());
    }

    @Test
    void parse_throwsWhenEmpty() {
        assertThrows(IllegalArgumentException.class, () -> RiskResultParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> RiskResultParser.parse(null));
    }

    @Test
    void parse_fallsBackToPlainTextWhenNotJson() {
        RiskResultParser.ParsedRiskResult result = RiskResultParser.parse("整体风险中等，空腹血糖偏高，建议复查。");
        assertEquals("medium", result.getRiskLevel());
        assertTrue(result.getFormatWarning().contains("兼容解析"));
    }
}
