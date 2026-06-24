package com.diabetes.assistant.common.utils;

import com.diabetes.assistant.common.constants.RoleConstants;
import com.diabetes.assistant.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CurrentUserUtil {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DEV_USER_ID_HEADER = "X-Dev-User-Id";
    private static final String DEV_USER_ROLE_HEADER = "X-Dev-User-Role";

    private final JwtUtil jwtUtil;

    public Integer getCurrentUserId(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length());
            if (!jwtUtil.validateToken(token)) {
                throw new BusinessException(401, "登录凭证无效或已过期");
            }
            return jwtUtil.getUserIdFromToken(token);
        }

        String devUserId = request.getHeader(DEV_USER_ID_HEADER);
        if (StringUtils.hasText(devUserId)) {
            try {
                return Integer.valueOf(devUserId);
            } catch (NumberFormatException exception) {
                throw new BusinessException(400, "X-Dev-User-Id 必须是数字");
            }
        }

        throw new BusinessException(401, "请先登录");
    }

    public String getCurrentRole(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length());
            if (!jwtUtil.validateToken(token)) {
                throw new BusinessException(401, "登录凭证无效或已过期");
            }
            return jwtUtil.getRoleFromToken(token);
        }

        String devRole = request.getHeader(DEV_USER_ROLE_HEADER);
        if (StringUtils.hasText(devRole)) {
            return devRole;
        }

        if (StringUtils.hasText(request.getHeader(DEV_USER_ID_HEADER))) {
            return RoleConstants.PATIENT;
        }

        throw new BusinessException(401, "请先登录");
    }

    public void requireAdmin(HttpServletRequest request) {
        if (!RoleConstants.ADMIN.equals(getCurrentRole(request))) {
            throw new BusinessException(403, "无权限访问管理端数据");
        }
    }
}
