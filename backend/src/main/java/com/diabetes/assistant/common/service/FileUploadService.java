package com.diabetes.assistant.common.service;

import com.diabetes.assistant.common.vo.UploadImageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    UploadImageResponse uploadImage(MultipartFile file, HttpServletRequest request);
}
