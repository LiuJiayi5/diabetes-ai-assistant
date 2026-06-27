package com.diabetes.assistant.modules.lifeplan.controller;

import com.diabetes.assistant.common.response.ApiResponse;
import com.diabetes.assistant.modules.lifeplan.service.impl.LifePlanMissingDataException;
import com.diabetes.assistant.modules.lifeplan.vo.MissingLifePlanDataResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LifePlanExceptionHandler {

    @ExceptionHandler(LifePlanMissingDataException.class)
    public ResponseEntity<ApiResponse<MissingLifePlanDataResponse>> handleMissingData(LifePlanMissingDataException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(exception.getCode(), exception.getMessage(), exception.getPayload()));
    }
}
