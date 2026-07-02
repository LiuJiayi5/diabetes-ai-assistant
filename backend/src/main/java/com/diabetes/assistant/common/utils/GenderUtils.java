package com.diabetes.assistant.common.utils;

import org.springframework.util.StringUtils;

public final class GenderUtils {

    private GenderUtils() {
    }

    public static String toDisplayLabel(String gender) {
        if (!StringUtils.hasText(gender)) {
            return "—";
        }
        return switch (gender.trim().toLowerCase()) {
            case "male" -> "男";
            case "female" -> "女";
            case "other" -> "其他";
            default -> gender;
        };
    }

    public static String normalizeGenderLabelInText(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return text.replace("性别male", "性别男")
                .replace("性别female", "性别女");
    }
}
