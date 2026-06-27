package com.diabetes.assistant.modules.aichat.controller;

import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.aichat.dto.admin.AdminAiChatLogResponse;
import com.diabetes.assistant.modules.aichat.dto.admin.AdminAiChatQuery;
import com.diabetes.assistant.modules.aichat.dto.admin.AdminAiChatSessionResponse;
import com.diabetes.assistant.modules.aichat.service.AiChatService;
import com.diabetes.assistant.modules.user.contract.UserQueryApi;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/ai-chat")
@RequiredArgsConstructor
public class AdminAiChatController {

    private final AiChatService aiChatService;
    private final CurrentUserUtil currentUserUtil;
    private final UserQueryApi userQueryApi;

    @GetMapping("/sessions")
    public ApiResponse<PageResult<AdminAiChatSessionResponse>> sessions(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(name = "expert_id", required = false) Integer expertId,
            @RequestParam(name = "user_keyword", required = false) String userKeyword,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        requireAdmin(request);
        AdminAiChatQuery query = buildQuery(page, pageSize, userId, expertId, userKeyword, startDate, endDate, keyword, status);
        return ApiResponse.success(aiChatService.listAdminSessions(query));
    }

    @GetMapping("/logs")
    public ApiResponse<PageResult<AdminAiChatLogResponse>> logs(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(name = "expert_id", required = false) Integer expertId,
            @RequestParam(name = "user_keyword", required = false) String userKeyword,
            @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "call_status", required = false) String callStatus) {
        requireAdmin(request);
        AdminAiChatQuery query = buildQuery(page, pageSize, userId, expertId, userKeyword, startDate, endDate, keyword, callStatus);
        return ApiResponse.success(aiChatService.listAdminLogs(query));
    }

    @GetMapping("/logs/{messageId}")
    public ApiResponse<AdminAiChatLogResponse> logDetail(
            HttpServletRequest request,
            @PathVariable Integer messageId) {
        requireAdmin(request);
        return ApiResponse.success(aiChatService.getAdminMessageDetail(messageId));
    }

    private AdminAiChatQuery buildQuery(Integer page, Integer pageSize, Integer userId, Integer expertId, String userKeyword,
                                        LocalDate startDate, LocalDate endDate, String keyword, String status) {
        AdminAiChatQuery query = new AdminAiChatQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setUserId(userId);
        query.setExpertId(expertId);
        query.setUserKeyword(userKeyword);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setKeyword(keyword);
        query.setCallStatus(status);
        return query;
    }

    private void requireAdmin(HttpServletRequest request) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        if (!userQueryApi.isAdmin(userId)) {
            throw new BusinessException(403, "Only admins can access AI chat admin data");
        }
    }
}
