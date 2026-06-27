package com.diabetes.assistant.common.utils;

public final class PageUtils {

    private PageUtils() {
    }

    public static int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
