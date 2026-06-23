package com.diabetes.assistant.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {
    }

    public static String format(LocalDateTime value) {
        return value == null ? "" : value.format(DEFAULT_FORMATTER);
    }
}
