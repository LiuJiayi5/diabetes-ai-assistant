export function safeJsonParse(value, fallback = null) {
  if (value == null || value === '') return fallback
  if (typeof value === 'object') return value
  if (typeof value !== 'string') return fallback

  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

export function normalizePlan(plan) {
  const planJson = safeJsonParse(plan?.plan_json, plan?.plan_json || {})
  const checkinTasks = safeJsonParse(plan?.checkin_tasks_json, planJson?.checkin_tasks || [])
  const dietPlan = planJson?.diet_plan || planJson?.dietPlan || {}
  const exercisePlan = planJson?.exercise_plan || planJson?.exercisePlan || {}
  const workRestPlan = planJson?.work_rest_plan || planJson?.workRestPlan || planJson?.sleep_plan || {}
  const scheduleSource =
    planJson?.daily_schedule ||
    planJson?.weekly_schedule ||
    planJson?.weekly_plan ||
    planJson?.days ||
    planJson?.plan_days ||
    planJson?.dailySchedule ||
    planJson
  const dailySchedule = normalizeWeeklySchedule(scheduleSource, { dietPlan, exercisePlan, workRestPlan })

  return {
    ...plan,
    id: plan?.plan_id || plan?.planId || plan?.id,
    title: cleanText(plan?.plan_title || planJson?.plan_title || planJson?.title, '个性化控糖生活方案'),
    goal: normalizeGoal(plan?.plan_goal || planJson?.plan_goal || planJson?.goal),
    summary: cleanText(
      plan?.summary || planJson?.summary || planJson?.risk_summary,
      '根据健康档案、近期指标和风险评估生成，帮助你安排饮食、运动和日常控糖提醒。'
    ),
    riskSummary: cleanText(planJson?.risk_summary || plan?.risk_level || ''),
    riskLevel: cleanText(plan?.risk_level || planJson?.risk_level || planJson?.risk_level_label || ''),
    dietPlan,
    exercisePlan,
    workRestPlan,
    dailySchedule,
    checkinTasks: Array.isArray(checkinTasks) ? checkinTasks : [],
    healthTips: toArray(planJson?.health_tips || planJson?.tips),
    medicalWarning: cleanText(planJson?.medical_warning, 'AI 建议仅供参考，不能替代线下诊疗。如血糖明显异常或身体不适，请及时咨询线下医生。'),
    planJson
  }
}

export function formatPlanTime(value) {
  if (!value) return '暂无更新时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return cleanText(value, '暂无更新时间')
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  return isToday ? `今天 ${time}` : `${date.getMonth() + 1}月${date.getDate()}日 ${time}`
}

export function getRiskMeta(plan) {
  const source = `${plan?.riskLevel || plan?.riskSummary || plan?.planJson?.risk_level || plan?.planJson?.risk_level_label || ''}`
  if (/high|高|偏高|重度/i.test(source)) return { label: '高风险', className: 'risk-high' }
  if (/low|低|轻度/i.test(source)) return { label: '低风险', className: 'risk-low' }
  return { label: '中风险', className: 'risk-medium' }
}

export function normalizeWeeklySchedule(source, context = {}) {
  const days = extractScheduleDays(source).map((day, index) => normalizeDay(day, index, context))
  return ensureSevenDays(days, context)
}

export function groupScheduleByDay(schedule = []) {
  return normalizeWeeklySchedule(schedule).reduce((groups, day) => {
    groups[day.title] = [
      ...day.dietCards.map((item) => ({ time: item.time, content: item.content })),
      ...day.exerciseCards.map((item) => ({ time: item.time, content: item.content })),
      ...day.reminders.map((item) => ({ time: '提醒', content: item }))
    ]
    return groups
  }, {})
}

export function toText(value, fallback = '暂无具体建议') {
  return cleanText(value, fallback)
}

function extractScheduleDays(source) {
  if (Array.isArray(source)) return normalizeScheduleArray(source)
  if (typeof source === 'string') return normalizeTextSchedule(source)
  if (!source || typeof source !== 'object') return []

  const candidate = source.daily_schedule || source.weekly_schedule || source.weekly_plan || source.days || source.plan_days || source.dailySchedule
  if (candidate && candidate !== source) return extractScheduleDays(candidate)

  return Object.entries(source)
    .filter(([key]) => /day|第|周|星期/i.test(key))
    .map(([key, value], index) => ({ ...(typeof value === 'object' && value ? value : { reminder: value }), title: key, day: index + 1 }))
}

function normalizeScheduleArray(items) {
  if (!items.length) return []
  if (items.every((item) => item && typeof item === 'object' && hasDayFields(item))) return items

  const buckets = new Map()
  items.forEach((item, index) => {
    const dayKey = item?.day || item?.date || item?.day_index || item?.dayIndex || Math.floor(index / 3) + 1
    const title = normalizeDayTitle(dayKey, buckets.size)
    const list = buckets.get(title) || []
    list.push(item)
    buckets.set(title, list)
  })

  return Array.from(buckets.entries()).map(([title, list]) => ({ title, items: list }))
}

function normalizeTextSchedule(value) {
  const text = cleanText(value, '')
  if (!text) return []

  const parts = text
    .split(/(?=第\s*\d+\s*天|Day\s*\d+|DAY\s*\d+)/)
    .map((item) => item.trim())
    .filter(Boolean)

  const dayTexts = parts.length > 1 ? parts : [text]
  return dayTexts.map((item, index) => ({ title: `第 ${index + 1} 天`, reminder: item }))
}

function normalizeDay(value, index = 0, context = {}) {
  const source = value && typeof value === 'object' ? value : { reminder: value }
  const items = Array.isArray(source.items) ? source.items : Array.isArray(source.tasks) ? source.tasks : []
  const meals = source.meals || source.meal_plan || source.diet_plan || source.dietPlan || source.diet || {}
  const sports = source.exercise_plan || source.exercisePlan || source.exercise || source.sport || source.training || {}
  const generalDiet = cleanText(source.diet_advice || source.diet || source.meal || pickItem(items, /饮食|meal|diet/i), '')
  const generalExercise = cleanText(source.exercise_advice || source.exercise || source.sport || source.training || pickItem(items, /运动|锻炼|exercise|sport/i), '')
  const dayOffset = index % 7

  const dietCards = [
    createCard('breakfast', '早餐建议', '07:00', pickMeal(meals, context.dietPlan, items, ['breakfast', '早餐', 'morning'], generalDiet, breakfastFallback(dayOffset))),
    createCard('lunch', '午餐建议', '12:00', pickMeal(meals, context.dietPlan, items, ['lunch', '午餐', 'noon'], generalDiet, lunchFallback(dayOffset))),
    createCard('dinner', '晚餐建议', '18:00', pickMeal(meals, context.dietPlan, items, ['dinner', '晚餐', 'evening'], generalDiet, dinnerFallback(dayOffset))),
    createCard('snack', '加餐建议', '两餐之间', pickMeal(meals, context.dietPlan, items, ['snack', '加餐', 'fruit'], generalDiet, snackFallback(dayOffset)))
  ]

  const exerciseCards = [
    createCard('light', '晨间/餐后轻运动', '餐后 20 分钟', pickExercise(sports, context.exercisePlan, items, ['light', 'walk', '晨', '餐后', '散步'], generalExercise, lightExerciseFallback(dayOffset))),
    createCard('aerobic', '有氧运动', '20-30 分钟', pickExercise(sports, context.exercisePlan, items, ['aerobic', '有氧', '快走', '骑行'], generalExercise, aerobicFallback(dayOffset))),
    createCard('resistance', '抗阻或拉伸训练', '10-15 分钟', pickExercise(sports, context.exercisePlan, items, ['resistance', 'stretch', '抗阻', '拉伸', '力量'], generalExercise, resistanceFallback(dayOffset))),
    createCard('notice', '运动注意事项', '运动前后', pickExercise(sports, context.exercisePlan, items, ['notice', 'tips', '注意', '补水', '鞋袜'], '', noticeFallback(dayOffset)))
  ]

  const reminder = cleanText(
    source.reminder || source.tip || source.health_tip || source.work_rest || source.sleep || source.content || pickItem(items, /提醒|作息|控糖|睡眠|tip|reminder/i),
    cleanText(context.workRestPlan, reminderFallback(dayOffset))
  )

  return {
    title: normalizeDayTitle(source.day || source.date || source.title || source.day_index || source.dayIndex, index),
    dietCards,
    exerciseCards,
    reminders: [reminder, '如血糖明显异常、身体不适或需要调整用药，请及时咨询线下医生。'].filter(Boolean),
    diet: dietCards.map((item) => item.content).join('；'),
    exercise: exerciseCards.map((item) => item.content).join('；'),
    reminder
  }
}

function ensureSevenDays(days, context) {
  const result = days.slice(0, 7)
  while (result.length < 7) {
    result.push(normalizeDay({ title: `第 ${result.length + 1} 天` }, result.length, context))
  }
  return result.map((day, index) => ({ ...day, title: normalizeDayTitle(day.title, index) }))
}

function createCard(key, title, time, content) {
  return { key, title, time, content: cleanText(content, '请结合自身情况保持规律安排。') }
}

function pickMeal(meals, plan, items, keys, general, fallback) {
  return cleanText(pickByKeys(meals, keys) || pickByKeys(plan, keys) || pickItem(items, new RegExp(keys.join('|'), 'i')) || '', general || fallback)
}

function pickExercise(sports, plan, items, keys, general, fallback) {
  return cleanText(pickByKeys(sports, keys) || pickByKeys(plan, keys) || pickItem(items, new RegExp(keys.join('|'), 'i')) || '', general || fallback)
}

function pickByKeys(source, keys) {
  if (!source || typeof source !== 'object') return ''
  const entries = Object.entries(source)
  const found = entries.find(([key]) => keys.some((item) => String(key).toLowerCase().includes(String(item).toLowerCase())))
  return found?.[1]
}

function hasDayFields(item) {
  return ['diet', 'diet_advice', 'exercise', 'exercise_advice', 'reminder', 'sleep', 'work_rest', 'content', 'items', 'meals', 'tasks'].some((key) => key in item)
}

function normalizeDayTitle(value, index = 0) {
  const text = cleanText(value, '')
  if (!text) return `第 ${index + 1} 天`
  if (/^\d+$/.test(String(text))) return `第 ${text} 天`
  if (/^day\s*\d+$/i.test(text)) return `第 ${text.match(/\d+/)?.[0] || index + 1} 天`
  return text
}

function normalizeGoal(value) {
  const text = cleanText(value, '')
  const map = {
    glucose_control: '控糖管理',
    weight_loss: '体重管理',
    diet: '饮食管理',
    exercise: '运动管理',
    comprehensive: '综合生活干预'
  }
  return map[text] || text || '控糖管理'
}

function pickItem(items, pattern) {
  const match = items.find((item) => pattern.test(`${item?.type || ''}${item?.task_type || ''}${item?.name || ''}${item?.task_name || ''}${item?.title || ''}${item?.time || ''}`))
  return match?.content || match?.description || match?.task_name || match?.name || match?.title || match
}

function toArray(value) {
  if (Array.isArray(value)) return value.map((item) => cleanText(item)).filter(Boolean)
  const text = cleanText(value, '')
  return text ? [text] : []
}

function cleanText(value, fallback = '') {
  if (Array.isArray(value)) {
    return value.map((item) => cleanText(item, '')).filter(Boolean).join('；') || fallback
  }
  if (value && typeof value === 'object') {
    return Object.values(value).map((item) => cleanText(item, '')).filter(Boolean).join('；') || fallback
  }
  const text = String(value ?? '').trim()
  if (!text || ['undefined', 'null', 'NaN', '[object Object]'].includes(text) || /\?{3,}/.test(text)) return fallback
  return text
}

function breakfastFallback(index) {
  const list = [
    '选择燕麦、全麦面包或杂粮粥，搭配鸡蛋、牛奶或豆制品，避免甜饮料。',
    '主食保持定量，加入一份蔬菜或少量坚果，让早餐更稳定、耐饿。',
    '早餐不要空腹只喝咖啡或茶，可搭配优质蛋白，减少上午血糖波动。'
  ]
  return list[index % list.length]
}

function lunchFallback(index) {
  const list = [
    '按餐盘法搭配：半盘非淀粉蔬菜、适量主食和鱼禽蛋豆等优质蛋白。',
    '外出用餐优先选择蒸、煮、炖类菜品，少油少盐，主食不额外加量。',
    '先吃蔬菜和蛋白质，再吃主食，帮助控制餐后血糖上升速度。'
  ]
  return list[index % list.length]
}

function dinnerFallback(index) {
  const list = [
    '晚餐保持清淡，避免过晚进食和高油高糖菜品，主食比午餐略少即可。',
    '增加绿叶菜和菌菇类，蛋白质选择鱼、鸡胸肉、豆腐等较清淡来源。',
    '晚餐后避免继续吃甜点和含糖饮料，如饥饿可选择少量低糖加餐。'
  ]
  return list[index % list.length]
}

function snackFallback(index) {
  const list = [
    '两餐之间如有饥饿，可选择无糖酸奶、少量坚果或小份低糖水果。',
    '加餐不是零食自由，避免蛋糕、奶茶、甜饮料和大量蜜饯。',
    '是否加餐可结合血糖记录和运动量调整，不饿时不必额外进食。'
  ]
  return list[index % list.length]
}

function lightExerciseFallback(index) {
  const list = [
    '餐后休息片刻后散步 10-20 分钟，以能轻松说话的强度为宜。',
    '久坐超过 1 小时可以起身活动 3-5 分钟，做肩颈伸展或室内步行。',
    '早晨可做轻柔热身，避免空腹剧烈运动，运动中留意头晕、心慌等反应。'
  ]
  return list[index % list.length]
}

function aerobicFallback(index) {
  const list = [
    '选择快走、骑车或轻慢跑等有氧活动，循序渐进完成当天运动目标。',
    '今天以稳定坚持为主，不追求强度过高，运动后身体应感觉舒展而非疲惫。',
    '可把有氧运动拆成两段完成，适合工作日或体力有限时执行。'
  ]
  return list[index % list.length]
}

function resistanceFallback(index) {
  const list = [
    '用弹力带、靠墙俯卧撑或坐站练习做轻抗阻训练，动作保持缓慢稳定。',
    '拉伸小腿、肩背和髋部，每个动作保持 10-20 秒，不要憋气。',
    '关节不适时降低幅度，优先选择安全、可持续的动作。'
  ]
  return list[index % list.length]
}

function noticeFallback(index) {
  const list = [
    '运动前后注意补水，穿舒适鞋袜，如出现明显不适应立即停止。',
    '有低血糖风险或近期血糖波动较大时，运动前可先确认身体状态。',
    '足部有破损、胸闷心悸或感染发热时，不建议勉强运动。'
  ]
  return list[index % list.length]
}

function reminderFallback(index) {
  const list = [
    '保持规律作息，记录空腹和餐后血糖，帮助后续调整方案。',
    '今天重点减少久坐，按时饮水，晚间尽量提前放松入睡。',
    '关注餐后反应和精神状态，若指标明显异常，请及时线下就医。'
  ]
  return list[index % list.length]
}
