package com.diabetes.assistant.common.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.common.service.FileUploadService;
import com.diabetes.assistant.common.vo.UploadImageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/images")
    public ApiResponse<UploadImageResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        return ApiResponse.success(fileUploadService.uploadImage(file, request));
    }
}
