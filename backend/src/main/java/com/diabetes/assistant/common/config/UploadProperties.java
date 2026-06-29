package com.diabetes.assistant.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Data
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String rootDir = "uploads";

    private long maxImageSizeMb = 5;

    public Path resolveRootPath() {
        Path configuredPath = Path.of(rootDir);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        Path workingDir = Path.of("").toAbsolutePath().normalize();
        if ("uploads".equals(rootDir)) {
            Path backendDir = findBackendDirectory(workingDir);
            if (backendDir != null) {
                return backendDir.resolve(rootDir).normalize();
            }
        }
        return workingDir.resolve(rootDir).normalize();
    }

    private Path findBackendDirectory(Path start) {
        Path current = start;
        while (current != null) {
            if (current.resolve("pom.xml").toFile().isFile()
                    && current.resolve("src").resolve("main").resolve("java").toFile().isDirectory()) {
                return current;
            }
            if (current.resolve("backend").resolve("pom.xml").toFile().isFile()) {
                return current.resolve("backend");
            }
            current = current.getParent();
        }
        return null;
    }
}
