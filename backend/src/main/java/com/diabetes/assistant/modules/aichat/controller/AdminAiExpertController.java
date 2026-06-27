package com.diabetes.assistant.modules.aichat.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.aichat.dto.AiExpertResponse;
import com.diabetes.assistant.modules.aichat.dto.AiExpertSaveRequest;
import com.diabetes.assistant.modules.aichat.service.AiExpertService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai-experts")
@RequiredArgsConstructor
public class AdminAiExpertController {

    private final AiExpertService aiExpertService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping
    public ApiResponse<PageResult<AiExpertResponse>> experts(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        currentUserUtil.requireAdmin(request);
        return ApiResponse.success(aiExpertService.listAdminExperts(page, pageSize, keyword, status));
    }

    @GetMapping("/{expertId}")
    public ApiResponse<AiExpertResponse> detail(HttpServletRequest request, @PathVariable Integer expertId) {
        currentUserUtil.requireAdmin(request);
        return ApiResponse.success(aiExpertService.getAdminExpert(expertId));
    }

    @PostMapping
    public ApiResponse<AiExpertResponse> create(HttpServletRequest request, @RequestBody AiExpertSaveRequest body) {
        currentUserUtil.requireAdmin(request);
        return ApiResponse.success(aiExpertService.saveAdminExpert(null, body));
    }

    @PutMapping("/{expertId}")
    public ApiResponse<AiExpertResponse> update(
            HttpServletRequest request,
            @PathVariable Integer expertId,
            @RequestBody AiExpertSaveRequest body) {
        currentUserUtil.requireAdmin(request);
        return ApiResponse.success(aiExpertService.saveAdminExpert(expertId, body));
    }

    @DeleteMapping("/{expertId}")
    public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable Integer expertId) {
        currentUserUtil.requireAdmin(request);
        aiExpertService.deleteAdminExpert(expertId);
        return ApiResponse.success();
    }
}
