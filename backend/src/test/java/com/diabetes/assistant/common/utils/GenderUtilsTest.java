package com.diabetes.assistant.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GenderUtilsTest {

    @Test
    void toDisplayLabel_mapsKnownValues() {
        assertEquals("男", GenderUtils.toDisplayLabel("male"));
        assertEquals("女", GenderUtils.toDisplayLabel("female"));
        assertEquals("其他", GenderUtils.toDisplayLabel("other"));
    }

    @Test
    void toDisplayLabel_keepsUnknownValue() {
        assertEquals("unknown", GenderUtils.toDisplayLabel("unknown"));
    }

    @Test
    void normalizeGenderLabelInText_replacesStoredCodes() {
        assertEquals("年龄46；性别男；身高172.0 cm",
                GenderUtils.normalizeGenderLabelInText("年龄46；性别male；身高172.0 cm"));
    }
}
