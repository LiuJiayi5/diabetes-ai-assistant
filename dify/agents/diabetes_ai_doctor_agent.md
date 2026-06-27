# diabetes_ai_doctor_agent

## Name

diabetes_ai_doctor_agent

## Module

Module 5: AI doctor consultation.

## Backend caller

Spring Boot calls this Agent through `DifyService.callAiDoctor`, which delegates to
`DifyClient.sendChatMessage` and Dify's `/chat-messages` API.

The backend currently calls the Agent in streaming mode because Dify Agent Chat
apps do not support the blocking mode used by workflow apps. `DifyClient`
aggregates the SSE response into a normal JSON string for the AI chat module.

The frontend must not call Dify directly and must not hold a Dify API key.

## Inputs

The backend sends a streaming chat message request with:

- `query`: the user's consultation question.
- `conversation_id`: optional Dify conversation id for multi-turn dialogue.
- `user`: current user id as text.
- `inputs`: a context object assembled by the backend.

Current `inputs` keys. These names must match the YAML `user_input_form`
variables exactly:

- `expert_identity`: the expert persona selected for this chat session. It may
  include `expert_id`, `expert_name`, `title`, `department`, `specialty`,
  `persona`, and `opening_message`. The Agent must answer as this expert
  identity for the whole conversation. If the user asks who you are or what you
  do, answer with the selected expert's name, title, department, specialty, and
  persona in natural language. Do not expose raw JSON or internal ids.
- `safety_rules`: backend safety constraints. This is the highest-priority
  context and should not be contradicted by the prompt.
- `user_basic`: current user id, username, role, and status. The Agent should
  use it only as identity context and should not expose internal ids.
- `profile_summary`: long-term health profile summary, such as age, sex,
  height, base weight, waist circumference, family history, chronic disease,
  allergy history, and lifestyle background.
- `latest_health_data`: latest health metric summary, such as weight, waist
  circumference, blood pressure, fasting glucose, postprandial glucose, HbA1c,
  diet, exercise, and record date.
- `risk_result`: latest risk assessment summary, including risk level, score,
  risk factors, indicator analysis, advice, and medical warning.
- `life_plan`: current life plan summary, including stage goal, plan summary,
  diet tasks, exercise tasks, weight management tasks, and follow-up focus.
- `checkin`: recent life check-in records and behavior analysis context.

The `checkin` object is provided by Module 8 integration and may contain:

- `period_days`
- `recent_summary`
- `completion_rate`
- `recent_records`: each item may include `checkin_id`, `date`, `task_type`,
  `task_name`, `status`, and `note`.
- `latest_analysis`: may include `analysis_id`, `start_date`, `end_date`,
  `total_days`, `diet_completion_count`, `exercise_completion_count`,
  `completion_rate`, `habit_score`, `diet_summary`, `exercise_summary`,
  `life_evaluation`, `main_problems`, `improvement_suggestions`,
  `next_focus`, `summary`, `call_status`, and `error_message`.

`DifyClient` normalizes non-string input values to JSON text before sending the
chat request, because Dify Agent chat input form variables are text fields.

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

Use the context with these priorities:

- `safety_rules` always wins.
- `expert_identity` defines the speaking identity and consultation focus. Keep
  this identity stable in multi-turn chat unless the backend starts a new
  session with another expert.
- For current measurements and today-level advice, prefer `latest_health_data`.
- For long-term background and risk factors, use `profile_summary`.
- Treat `risk_result`, `life_plan`, and `checkin` as advisory context, not
  medical diagnosis.
- If contexts conflict, prefer newer and more specific data and state the
  uncertainty briefly.

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

After import, confirm that the Agent input form contains all eight variables:
`expert_identity`, `safety_rules`, `user_basic`, `profile_summary`,
`latest_health_data`, `risk_result`, `life_plan`, and `checkin`. If Dify creates a new app/API key
during import, update the backend environment variable accordingly. If the
existing published app is updated in place, the current key can continue to be
used.

Finally configure the backend with the published Agent API key:

```yaml
dify:
  ai-doctor-api-key: ${DIFY_AI_DOCTOR_API_KEY:your_ai_doctor_api_key}
```
