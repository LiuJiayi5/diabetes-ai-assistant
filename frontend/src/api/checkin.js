import request from './request'

const MOCK_KEY = 'diabetes_ai_assistant_checkin_mock'

function today() {
  return new Date().toISOString().slice(0, 10)
}

function nowText() {
  return new Date().toISOString().slice(0, 19)
}

function shouldUseMock() {
  return import.meta.env.VITE_CHECKIN_MOCK === 'true'
}

function readMockState() {
  const defaultState = {
    tasks: [
      {
        checkin_id: 1001,
        user_id: 1,
        plan_id: 1,
        task_type: 'diet',
        task_name: '饮食打卡',
        status: 'pending',
        note: '',
        checkin_date: today(),
        completed_time: null
      },
      {
        checkin_id: 1002,
        user_id: 1,
        plan_id: 1,
        task_type: 'exercise',
        task_name: '运动打卡',
        status: 'pending',
        note: '',
        checkin_date: today(),
        completed_time: null
      }
    ],
    analysis: null
  }

  try {
    const parsed = JSON.parse(localStorage.getItem(MOCK_KEY) || 'null')
    if (!parsed) return defaultState

    const sameDayTasks = Array.isArray(parsed.tasks)
      ? parsed.tasks.map((task) => ({
          ...task,
          checkin_date: task.checkin_date || today()
        }))
      : defaultState.tasks

    const hasToday = sameDayTasks.some((task) => task.checkin_date === today())
    return {
      tasks: hasToday ? sameDayTasks : [...sameDayTasks, ...defaultState.tasks],
      analysis: parsed.analysis || null
    }
  } catch {
    return defaultState
  }
}

function writeMockState(state) {
  localStorage.setItem(MOCK_KEY, JSON.stringify(state))
}

function mockResponse(data) {
  return Promise.resolve({
    code: 200,
    message: 'mock success',
    data,
    timestamp: nowText()
  })
}

async function withMockFallback(requestPromise, fallback) {
  try {
    const response = await requestPromise
    if (response?.code && response.code !== 200) {
      const error = new Error(response.message || 'Request failed')
      error.response = { data: response }
      throw error
    }
    return response
  } catch (error) {
    if (!shouldUseMock()) throw error
    console.warn('[checkin] using local mock data because backend request failed:', error?.message)
    return mockResponse(fallback())
  }
}

function todayTasks() {
  return readMockState().tasks.filter((task) => task.checkin_date === today())
}

function calculateStatistics(period = 7) {
  const state = readMockState()
  const end = new Date(today())
  const start = new Date(end)
  start.setDate(end.getDate() - Number(period || 7) + 1)

  const records = state.tasks.filter((task) => {
    const date = new Date(task.checkin_date)
    return date >= start && date <= end
  })
  const completed = records.filter((task) => task.status === 'completed')

  return {
    total_days: Number(period || 7),
    diet_completion_count: new Set(completed.filter((task) => task.task_type === 'diet').map((task) => task.checkin_date)).size,
    exercise_completion_count: new Set(completed.filter((task) => task.task_type === 'exercise').map((task) => task.checkin_date)).size,
    total_task_count: records.length,
    completed_task_count: completed.length,
    completion_rate: records.length ? Number(((completed.length / records.length) * 100).toFixed(2)) : 0,
    start_date: start.toISOString().slice(0, 10),
    end_date: today()
  }
}

export function getTodayCheckins(params = {}) {
  return withMockFallback(
    request.get('/checkins/today', { params }),
    () => ({
      list: todayTasks(),
      message: 'mock success'
    })
  )
}

export function submitCheckin(payload) {
  return withMockFallback(
    request.post('/checkins', payload),
    () => {
      const state = readMockState()
      const index = state.tasks.findIndex((task) => task.checkin_id === payload.checkin_id)
      if (index >= 0) {
        state.tasks[index] = {
          ...state.tasks[index],
          status: payload.status,
          note: payload.note || '',
          completed_time: payload.status === 'completed' ? nowText() : null
        }
      }
      writeMockState(state)
      return state.tasks[index]
    }
  )
}

export function getCheckinHistory(params = {}) {
  return withMockFallback(
    request.get('/checkins/history', { params }),
    () => {
      const page = Number(params.page || 1)
      const pageSize = Number(params.page_size || 10)
      const list = readMockState().tasks
        .filter((task) => !params.start_date || task.checkin_date >= params.start_date)
        .filter((task) => !params.end_date || task.checkin_date <= params.end_date)
        .filter((task) => !params.task_type || task.task_type === params.task_type)
        .filter((task) => !params.status || task.status === params.status)
        .sort((a, b) => b.checkin_date.localeCompare(a.checkin_date) || a.task_type.localeCompare(b.task_type))

      return {
        list: list.slice((page - 1) * pageSize, page * pageSize),
        total: list.length,
        page,
        page_size: pageSize
      }
    }
  )
}

export function getCheckinStatistics(params = {}) {
  return withMockFallback(
    request.get('/checkins/statistics', { params }),
    () => calculateStatistics(params.period)
  )
}

export function generateCheckinAnalysis(payload = {}) {
  return withMockFallback(
    request.post('/ai/checkin-analysis', payload),
    () => {
      const state = readMockState()
      const statistics = calculateStatistics(payload.period || 7)
      const rate = Number(statistics.completion_rate || 0)
      const analysis = {
        analysis_id: Date.now(),
        user_id: 1,
        plan_id: 1,
        start_date: statistics.start_date,
        end_date: statistics.end_date,
        total_days: statistics.total_days,
        diet_completion_count: statistics.diet_completion_count,
        exercise_completion_count: statistics.exercise_completion_count,
        completion_rate: statistics.completion_rate,
        habit_score: Math.min(95, Math.max(45, Math.round(rate || 45))),
        diet_summary: rate > 0 ? '饮食记录已经开始沉淀，建议继续关注主食量和餐后反馈。' : '当前饮食打卡较少，建议先保持每日记录。',
        exercise_summary: rate > 0 ? '运动打卡可帮助观察执行稳定性，建议固定一个更容易坚持的时间段。' : '当前运动打卡较少，可以先从饭后散步开始。',
        life_evaluation: rate >= 80 ? '近期执行较稳定，可以继续保持。' : '当前记录仍在起步阶段，分析仅供本地联调参考。',
        main_problems: rate >= 80 ? ['偶尔仍需关注漏打卡情况'] : ['打卡数据较少，分析仅供参考', '饮食与运动记录连续性仍需提升'],
        improvement_suggestions: ['每天固定一个时间完成打卡', '优先保证饮食和运动各记录一次', '连续记录 7 天后再生成正式分析'],
        next_focus: '先完成连续 7 天饮食和运动打卡。',
        summary: '本地开发环境模拟分析结果。',
        input_summary: 'local mock checkin data',
        call_status: 'success',
        error_message: null,
        create_time: nowText()
      }
      state.analysis = analysis
      writeMockState(state)
      return analysis
    }
  )
}

export function getLatestCheckinAnalysis() {
  return withMockFallback(
    request.get('/checkin-analysis/latest'),
    () => readMockState().analysis
  )
}

export function getCheckinAnalysisHistory(params = {}) {
  return withMockFallback(
    request.get('/checkin-analysis/history', { params }),
    () => {
      const analysis = readMockState().analysis
      return {
        list: analysis ? [analysis] : [],
        total: analysis ? 1 : 0,
        page: Number(params.page || 1),
        page_size: Number(params.page_size || 10)
      }
    }
  )
}
