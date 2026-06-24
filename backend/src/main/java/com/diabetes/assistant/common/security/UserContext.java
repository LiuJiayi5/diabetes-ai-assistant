package com.diabetes.assistant.common.security;

import com.diabetes.assistant.common.constants.RoleConstants;
import com.diabetes.assistant.common.exception.BusinessException;

public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser getRequired() {
        LoginUser loginUser = HOLDER.get();
        if (loginUser == null) {
            throw new BusinessException(401, "登录状态无效，请重新登录");
        }
        return loginUser;
    }

    public static Integer getUserId() {
        return getRequired().getUserId();
    }

    public static void requireAdmin() {
        LoginUser loginUser = getRequired();
        if (!RoleConstants.ADMIN.equals(loginUser.getRole())) {
            throw new BusinessException(403, "无权限访问管理端数据");
        }
    }

    public static void clear() {
        HOLDER.remove();
    }
}
