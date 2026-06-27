package com.diabetes.assistant.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(Integer code, String message, T data) {
        return new ApiResponse<>(code, message, data, LocalDateTime.now());
    }
}
