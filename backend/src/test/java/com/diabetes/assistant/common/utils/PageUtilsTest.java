package com.diabetes.assistant.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageUtilsTest {

    @Test
    void normalizePage_defaultsToOneWhenNullOrInvalid() {
        assertEquals(1, PageUtils.normalizePage(null));
        assertEquals(1, PageUtils.normalizePage(0));
        assertEquals(1, PageUtils.normalizePage(-1));
    }

    @Test
    void normalizePage_keepsValidValue() {
        assertEquals(2, PageUtils.normalizePage(2));
    }

    @Test
    void normalizePageSize_defaultsToTenWhenNullOrInvalid() {
        assertEquals(10, PageUtils.normalizePageSize(null));
        assertEquals(10, PageUtils.normalizePageSize(0));
    }

    @Test
    void normalizePageSize_capsAtOneHundred() {
        assertEquals(100, PageUtils.normalizePageSize(200));
        assertEquals(20, PageUtils.normalizePageSize(20));
    }
}
