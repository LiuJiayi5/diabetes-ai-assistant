# 后端模块 Contract 规则

## 为什么需要 contract 接口

本项目后端采用模块化 Spring Boot 架构。每个模块拥有自己的 Controller、Service、ServiceImpl、Mapper、Entity、DTO。为了避免模块之间互相穿透内部实现，跨模块读取数据只能通过 `contract` 包中的 Java Interface 和 contract DTO。

contract 接口用于稳定模块边界，降低模块耦合，方便各模块负责人独立开发。

## 核心规则

允许调用：

- 其他模块 `contract` 包下的 Java Interface
- 其他模块 `contract/dto` 包下的 DTO

禁止跨模块调用：

- 其他模块 Controller
- 其他模块 ServiceImpl
- 其他模块 Mapper
- 其他模块 Entity
- 其他模块私有 DTO

contract DTO 不等于数据库 Entity，不能直接暴露数据库实体。

## 模块1：用户模块

接口路径：

```text
backend/src/main/java/com/diabetes/assistant/modules/user/contract/UserQueryApi.java
```

方法：

```java
UserBasicDTO getUserBasicById(Integer userId);
boolean existsActiveUser(Integer userId);
boolean isAdmin(Integer userId);
```

DTO：`UserBasicDTO`

字段：

- Integer userId
- String username
- String phone
- String email
- String avatar
- String role
- String status

## 模块2：健康档案模块

接口路径：

```text
backend/src/main/java/com/diabetes/assistant/modules/profile/contract/PatientProfileQueryApi.java
```

方法：

```java
PatientProfileDTO getProfileByUserId(Integer userId);
String getProfileSummaryByUserId(Integer userId);
boolean hasProfile(Integer userId);
```

DTO：`PatientProfileDTO`

字段：

- Integer profileId
- Integer userId
- Integer age
- String gender
- BigDecimal heightCm
- BigDecimal baseWeightKg
- BigDecimal baseWaistCm
- String familyHistory
- String chronicHistory
- String allergyHistory
- String profileSummary
- LocalDateTime createTime
- LocalDateTime updateTime

## 模块3：健康数据模块

接口路径：

```text
backend/src/main/java/com/diabetes/assistant/modules/healthmetric/contract/HealthMetricQueryApi.java
```

方法：

```java
HealthMetricDTO getLatestMetricByUserId(Integer userId);
List<HealthMetricDTO> listMetricsByUserId(Integer userId, LocalDate startDate, LocalDate endDate);
String getLatestMetricSummaryByUserId(Integer userId);
```

DTO：`HealthMetricDTO`

字段：

- Integer metricId
- Integer userId
- BigDecimal weightKg
- BigDecimal waistCm
- Integer systolicBp
- Integer diastolicBp
- BigDecimal fastingGlucose
- BigDecimal postprandialGlucose
- BigDecimal hba1c
- String dietStatus
- String exerciseStatus
- String metricSummary
- LocalDate recordedAt
- LocalDateTime createTime

## 模块4：风险评估模块

接口路径：

```text
backend/src/main/java/com/diabetes/assistant/modules/risk/contract/RiskAssessmentQueryApi.java
```

方法：

```java
RiskAssessmentDTO getLatestAssessmentByUserId(Integer userId);
List<RiskAssessmentDTO> listAssessmentsByUserId(Integer userId, LocalDate startDate, LocalDate endDate);
String getLatestRiskSummaryByUserId(Integer userId);
```

DTO：`RiskAssessmentDTO`

字段：

- Integer assessmentId
- Integer userId
- Integer metricId
- String riskLevel
- Integer riskScore
- String diabetesTypeTendency
- String mainRiskFactors
- String indicatorAnalysis
- String healthAdvice
- String medicalWarning
- String summary
- String callStatus
- String errorMessage
- LocalDateTime createTime

## 模块5：AI 医生咨询模块

接口路径：

```text
backend/src/main/java/com/diabetes/assistant/modules/aichat/contract/AiChatQueryApi.java
```

方法：

```java
long countMessagesByUserId(Integer userId);
List<AiChatSessionSummaryDTO> listRecentSessionsByUserId(Integer userId, Integer limit);
String getLatestChatSummaryByUserId(Integer userId);
```

DTO：`AiChatSessionSummaryDTO`

字段：

- Integer sessionId
- Integer userId
- String sessionTitle
- String status
- LocalDateTime lastMessageTime
- LocalDateTime createTime

## 模块6：生活方案模块

接口路径：

```text
backend/src/main/java/com/diabetes/assistant/modules/lifeplan/contract/LifePlanQueryApi.java
```

方法：

```java
LifePlanDTO getCurrentPlanByUserId(Integer userId);
Integer getCurrentPlanIdByUserId(Integer userId);
String getCurrentLifePlanSummaryByUserId(Integer userId);
String getCurrentCheckinTasksJsonByUserId(Integer userId);
```

DTO：`LifePlanDTO`

字段：

- Integer planId
- Integer userId
- Integer assessmentId
- String planTitle
- String planGoal
- String dietPlanJson
- String exercisePlanJson
- String dailyScheduleJson
- String checkinTasksJson
- String healthTipsJson
- String summary
- String status
- String callStatus
- String errorMessage
- LocalDateTime createTime

## 模块7：内容模块

接口路径：

```text
backend/src/main/java/com/diabetes/assistant/modules/content/contract/ContentQueryApi.java
```

方法：

```java
ArticleSummaryDTO getArticleSummaryById(Integer articleId);
List<ArticleSummaryDTO> listRecommendedArticles(Integer limit);
List<HomeContentDTO> listEnabledHomeContents();
```

DTO：`ArticleSummaryDTO`

字段：

- Integer articleId
- String title
- String category
- String coverImage
- String summary
- Integer viewCount
- Boolean isRecommended
- LocalDateTime createTime

DTO：`HomeContentDTO`

字段：

- Integer contentId
- String contentType
- String title
- String subtitle
- String imageUrl
- String linkType
- String linkValue
- Integer sortOrder
- String status

## 模块8：生活打卡与行为分析模块

接口路径：

```text
backend/src/main/java/com/diabetes/assistant/modules/checkin/contract/CheckinQueryApi.java
```

方法：

```java
List<CheckinRecordDTO> listRecentCheckins(Integer userId, Integer period);
BigDecimal getRecentCompletionRate(Integer userId, Integer period);
String getLatestCheckinSummaryByUserId(Integer userId, Integer period);
CheckinAnalysisDTO getLatestAnalysisByUserId(Integer userId);
String getLatestCheckinAnalysisSummaryByUserId(Integer userId);
```

DTO：`CheckinRecordDTO`

字段：

- Integer checkinId
- Integer userId
- Integer planId
- String taskType
- String taskName
- String status
- String note
- LocalDate checkinDate
- LocalDateTime completedTime
- LocalDateTime createTime

DTO：`CheckinAnalysisDTO`

字段：

- Integer analysisId
- Integer userId
- Integer planId
- LocalDate startDate
- LocalDate endDate
- Integer totalDays
- Integer dietCompletionCount
- Integer exerciseCompletionCount
- BigDecimal completionRate
- Integer habitScore
- String dietSummary
- String exerciseSummary
- String lifeEvaluation
- String mainProblems
- String improvementSuggestions
- String nextFocus
- String summary
- String inputSummary
- String callStatus
- String errorMessage
- LocalDateTime createTime

## 调用示例

```java
@Service
public class RiskContextAssembler {

    private final PatientProfileQueryApi patientProfileQueryApi;
    private final HealthMetricQueryApi healthMetricQueryApi;

    public RiskContextAssembler(PatientProfileQueryApi patientProfileQueryApi,
                                HealthMetricQueryApi healthMetricQueryApi) {
        this.patientProfileQueryApi = patientProfileQueryApi;
        this.healthMetricQueryApi = healthMetricQueryApi;
    }

    public void assemble(Integer userId) {
        String profileSummary = patientProfileQueryApi.getProfileSummaryByUserId(userId);
        String metricSummary = healthMetricQueryApi.getLatestMetricSummaryByUserId(userId);
        // TODO: 组装风险预测上下文。
    }
}
```

## 后续负责人如何实现自己的 contract 接口

每个模块负责人在本模块内创建实现类，例如：

```text
modules/profile/service/impl/PatientProfileQueryApiImpl.java
```

实现类可以访问本模块 Service、Mapper、Entity，并把 Entity 转换为 contract DTO 后返回。实现类不能返回 Entity，也不能让其他模块直接依赖本模块 Mapper。
