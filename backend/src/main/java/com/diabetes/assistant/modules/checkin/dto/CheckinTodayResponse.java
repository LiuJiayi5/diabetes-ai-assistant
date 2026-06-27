package com.diabetes.assistant.modules.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckinTodayResponse {

    private List<CheckinTaskResponse> list;
    private String message;
}
