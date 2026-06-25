package com.diabetes.assistant.modules.checkin.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminCheckinAnalysisResponse {

    @JsonProperty("analysis_id")
    private Integer analysisId;
    @JsonProperty("user_id")
    private Integer userId;
    @JsonProperty("plan_id")
    private Integer planId;
    @JsonProperty("start_date")
    private LocalDate startDate;
    @JsonProperty("end_date")
    private LocalDate endDate;
    @JsonProperty("total_days")
    private Integer totalDays;
    @JsonProperty("diet_completion_count")
    private Integer dietCompletionCount;
    @JsonProperty("exercise_completion_count")
    private Integer exerciseCompletionCount;
    @JsonProperty("completion_rate")
    private BigDecimal completionRate;
    @JsonProperty("habit_score")
    private Integer habitScore;
    @JsonProperty("diet_summary")
    private String dietSummary;
    @JsonProperty("exercise_summary")
    private String exerciseSummary;
    @JsonProperty("life_evaluation")
    private String lifeEvaluation;
    @JsonProperty("main_problems")
    private List<String> mainProblems;
    @JsonProperty("improvement_suggestions")
    private List<String> improvementSuggestions;
    @JsonProperty("next_focus")
    private String nextFocus;
    private String summary;
    @JsonProperty("input_summary")
    private String inputSummary;
    @JsonProperty("input_items")
    private List<String> inputItems;
    @JsonProperty("call_status")
    private String callStatus;
    @JsonProperty("error_message")
    private String errorMessage;
    @JsonProperty("create_time")
    private LocalDateTime createTime;
    private PatientSummaryResponse patient;
    private PlanSummaryResponse plan;
}
