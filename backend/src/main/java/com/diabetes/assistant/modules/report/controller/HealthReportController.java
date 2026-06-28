package com.diabetes.assistant.modules.report.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.common.utils.CurrentUserUtil;
import com.diabetes.assistant.modules.report.dto.GenerateReportRequest;
import com.diabetes.assistant.modules.report.dto.HealthReportResponse;
import com.diabetes.assistant.modules.report.service.HealthReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class HealthReportController {

    private final HealthReportService reportService;
    private final CurrentUserUtil currentUserUtil;

    @PostMapping
    public ApiResponse<HealthReportResponse> generate(HttpServletRequest request,
                                                       @Valid @RequestBody GenerateReportRequest generateRequest) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(reportService.generate(userId, generateRequest));
    }

    @GetMapping
    public ApiResponse<PageResult<HealthReportResponse>> list(HttpServletRequest request,
                                                              @RequestParam(defaultValue = "1") Integer page,
                                                              @RequestParam(name = "page_size", defaultValue = "10") Integer pageSize) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(reportService.list(userId, page, pageSize));
    }

    @GetMapping("/{reportId}")
    public ApiResponse<HealthReportResponse> detail(HttpServletRequest request, @PathVariable Integer reportId) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return ApiResponse.success(reportService.getDetail(userId, reportId));
    }

    @GetMapping("/{reportId}/export/markdown")
    public ResponseEntity<byte[]> exportMarkdown(HttpServletRequest request, @PathVariable Integer reportId) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return download(reportService.exportMarkdown(userId, reportId), "report-" + reportId + ".md",
                MediaType.parseMediaType("text/markdown; charset=UTF-8"));
    }

    @GetMapping("/{reportId}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(HttpServletRequest request, @PathVariable Integer reportId) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return download(reportService.exportPdf(userId, reportId), "report-" + reportId + ".pdf",
                MediaType.APPLICATION_PDF);
    }

    @GetMapping("/{reportId}/export/fhir")
    public ResponseEntity<byte[]> exportFhir(HttpServletRequest request, @PathVariable Integer reportId) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return download(reportService.exportFhir(userId, reportId), "report-" + reportId + ".fhir.json",
                MediaType.APPLICATION_JSON);
    }

    @GetMapping("/{reportId}/export/hl7")
    public ResponseEntity<byte[]> exportHl7(HttpServletRequest request, @PathVariable Integer reportId) {
        Integer userId = currentUserUtil.getCurrentUserId(request);
        return download(reportService.exportHl7(userId, reportId), "report-" + reportId + ".hl7",
                MediaType.parseMediaType("text/plain; charset=UTF-8"));
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String filename, MediaType mediaType) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .body(bytes);
    }
}
