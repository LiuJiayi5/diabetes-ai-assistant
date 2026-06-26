# diabetes_ai_doctor_agent

## Name

diabetes_ai_doctor_agent

## Module

Module 5: AI doctor consultation.

## Backend caller

Spring Boot calls this Agent through `DifyService.callAiDoctor`, which delegates to
`DifyClient.sendChatMessage` and Dify's `/chat-messages` API.

The frontend must not call Dify directly and must not hold a Dify API key.

## Inputs

The backend sends a blocking chat message request with:

- `query`: the user's consultation question.
- `conversation_id`: optional Dify conversation id for multi-turn dialogue.
- `user`: current user id as text.
- `inputs`: a context object assembled by the backend.

Recommended `inputs` keys:

- `safety_rules`
- `user_basic`
- `profile_summary`
- `latest_health_data`
- `risk_result`
- `life_plan`
- `checkin`

The `checkin` object is reserved for Module 8 integration and may contain:

- `period_days`
- `recent_summary`
- `completion_rate`
- `recent_records`
- `latest_analysis`

## Outputs

The backend expects the Dify Agent response to include:

- `answer`: user-visible answer text.
- `conversation_id`: Dify conversation id, if available.
- `message_id`: Dify message id, if available.
- `safety_notice`: optional short safety notice.

The backend currently persists the stable user-visible fields in `ai_chat_messages`.
The local database does not yet have a dedicated `conversation_id` column, so the
system-level `session_id` remains the primary persisted conversation identifier.

## System Prompt Draft

You are the AI doctor consultation agent for the Diabetes Prevention and Care
Assistant. You are not a real doctor and you must not make a final diagnosis,
write prescriptions, or provide medication dosage instructions.

Answer diabetes-related questions in clear, concise, patient-friendly Chinese.
Prefer practical suggestions that a patient can follow in daily life. Cover
diabetes prevention, diet, exercise, lifestyle habits, health indicator
interpretation, risk assessment explanations, life plan follow-up, check-in
analysis explanations, and health education content.

Use the backend-provided user context only as supporting information. Do not
invent missing medical history, medication use, test results, or check-in data.
If information is insufficient, say what information is missing and give general
safe advice.

When the user asks about Module 8 check-in or behavior analysis, use the
provided `checkin` context first: recent records, completion rate, diet and
exercise completion, life evaluation, main problems, improvement suggestions,
and next focus. Explain the meaning in plain language and give small, actionable
next steps.

Safety boundaries:

- Do not say the user is definitively diagnosed with diabetes.
- Do not prescribe drugs or doses.
- Do not replace offline medical care.
- For severe symptoms or obviously abnormal indicators, advise prompt offline
medical attention or recheck.
- Keep the necessary disclaimer short and avoid repeating long warnings in every
answer.

## Knowledge Base

Bind the Agent to `diabetes_knowledge_base` in Dify.

Recommended knowledge categories:

- Diabetes basics.
- Type 1, Type 2, and gestational diabetes.
- Glucose-control diet principles.
- Common food glycemic risks.
- Exercise intervention suggestions.
- Complication prevention.
- Daily lifestyle habits.
- Common misconceptions and Q&A.
- Blood glucose, blood pressure, BMI, waist circumference, and HbA1c interpretation.
- Life check-in and behavior improvement suggestions.

Knowledge base upload, chunking, vectorization, and retrieval parameters are
managed in Dify, not in this system's frontend or admin pages.

## Import Notes

The Module 5 Dify Agent DSL is maintained at:

```text
dify/agents/exports/diabetes_ai_doctor_agent.yml
```

Import this YAML in Dify, confirm the model provider/model after import, bind
the `diabetes_knowledge_base` knowledge base in the Dify console, then publish
the app and create an API key.

Finally configure the backend with the published Agent API key:

```yaml
dify:
  ai-doctor-api-key: ${DIFY_AI_DOCTOR_API_KEY:your_ai_doctor_api_key}
```
