package com.diabetes.assistant.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void hashPassword_and_matches_roundTrip() {
        String raw = "123456";
        String hash = PasswordUtil.hashPassword(raw);
        assertNotEquals(raw, hash);
        assertTrue(PasswordUtil.matches(raw, hash));
    }

    @Test
    void matches_returnsFalseForWrongPassword() {
        String hash = PasswordUtil.hashPassword("123456");
        assertFalse(PasswordUtil.matches("wrong", hash));
    }
}
