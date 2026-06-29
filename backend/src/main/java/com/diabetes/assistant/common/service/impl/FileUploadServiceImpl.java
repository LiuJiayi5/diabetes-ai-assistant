package com.diabetes.assistant.common.service.impl;

import com.diabetes.assistant.common.config.UploadProperties;
import com.diabetes.assistant.common.exception.BusinessException;
import com.diabetes.assistant.common.service.FileUploadService;
import com.diabetes.assistant.common.vo.UploadImageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private static final Set<String> IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "webp");
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final UploadProperties uploadProperties;

    @Override
    public UploadImageResponse uploadImage(MultipartFile file, HttpServletRequest request) {
        validate(file);

        String extension = extension(file.getOriginalFilename());
        String relativeDir = "images/" + LocalDate.now().format(DATE_PATH);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path uploadRoot = uploadProperties.resolveRootPath();
        Path targetDir = uploadRoot.resolve(relativeDir).normalize();
        Path target = targetDir.resolve(filename).normalize();

        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException(400, "图片保存路径不合法");
        }

        try {
            Files.createDirectories(targetDir);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException(500, "图片上传失败，请稍后重试");
        }

        String path = "/uploads/" + relativeDir + "/" + filename;
        return UploadImageResponse.builder()
                .url(path)
                .path(path)
                .filename(filename)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        long maxSize = uploadProperties.getMaxImageSizeMb() * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException(400, "图片大小不能超过 " + uploadProperties.getMaxImageSizeMb() + "MB");
        }
        String extension = extension(file.getOriginalFilename());
        if (!IMAGE_TYPES.contains(extension)) {
            throw new BusinessException(400, "仅支持 jpg、jpeg、png、webp 格式图片");
        }
    }

    private String extension(String filename) {
        String cleanFilename = StringUtils.cleanPath(filename == null ? "" : filename);
        int index = cleanFilename.lastIndexOf('.');
        if (index < 0 || index == cleanFilename.length() - 1) {
            return "";
        }
        return cleanFilename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
