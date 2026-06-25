package com.diabetes.assistant.common.security;

import com.diabetes.assistant.common.constants.RoleConstants;
import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AdminApiRoleFilter extends OncePerRequestFilter {

    private final CurrentUserUtil currentUserUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        CurrentUserUtil.TokenPrincipal principal;
        try {
            principal = currentUserUtil.getCurrentPrincipal(request);
        } catch (Exception exception) {
            writeError(response, 401, "管理员登录状态已失效，请重新登录");
            return;
        }

        if (!RoleConstants.ADMIN.equals(principal.role())) {
            writeError(response, 403, "当前账号无权访问管理端");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(status, message)));
    }
}
