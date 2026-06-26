package com.diabetes.assistant.modules.lifeplan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MissingLifePlanDataResponse {

    private List<String> missing;
}
