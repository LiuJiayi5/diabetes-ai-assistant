package com.diabetes.assistant.common.utils;

import com.diabetes.assistant.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CurrentUserUtil {

    private static final String BEARER_PREFIX = "Bearer ";

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

        // Dev/test fallback until the login filter is wired into the project.
        String devUserId = request.getHeader("X-Dev-User-Id");
        if (StringUtils.hasText(devUserId)) {
            try {
                return Integer.valueOf(devUserId);
            } catch (NumberFormatException exception) {
                throw new BusinessException(400, "X-Dev-User-Id 必须是数字");
            }
        }

        throw new BusinessException(401, "请先登录");
    }
}
