package com.diabetes.assistant.modules.report.service;

import com.diabetes.assistant.common.response.PageResult;
import com.diabetes.assistant.modules.report.dto.GenerateReportRequest;
import com.diabetes.assistant.modules.report.dto.HealthReportResponse;

public interface HealthReportService {

    HealthReportResponse generate(Integer userId, GenerateReportRequest request);

    PageResult<HealthReportResponse> list(Integer userId, Integer page, Integer pageSize);

    HealthReportResponse getDetail(Integer userId, Integer reportId);

    HealthReportResponse getPublicDetail(Integer reportId);

    byte[] exportPublicHtml(Integer reportId);

    byte[] exportPublicPdf(Integer reportId);

    byte[] exportMarkdown(Integer userId, Integer reportId);

    byte[] exportPdf(Integer userId, Integer reportId);

    byte[] exportFhir(Integer userId, Integer reportId);

    byte[] exportHl7(Integer userId, Integer reportId);
}
