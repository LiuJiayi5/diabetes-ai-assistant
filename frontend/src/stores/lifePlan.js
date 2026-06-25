import { defineStore } from 'pinia'
import { generateLifePlan, getLifePlanDetail, getLifePlans } from '@/api/lifePlan'

function unwrapResponse(response) {
  if (Array.isArray(response)) return response
  return response?.data ?? response?.records ?? response?.list ?? response?.items ?? response
}

function normalizeList(response) {
  const data = unwrapResponse(response)
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.items)) return data.items
  return []
}

function resolveErrorMessage(error, fallback = '请求失败，请稍后重试') {
  const status = error?.response?.status
  const data = error?.response?.data
  const message = data?.message || data?.error_message || data?.error || error?.message

  if (status === 401) return '登录已失效，请重新登录'
  if (status === 403) return '当前账号无权生成生活方案'
  if (status === 400) return '请先完善健康档案、健康数据和风险评估'
  if (status === 502) return 'AI 方案暂时不可用，已保留当前可执行方案'
  if (status === 500) return '服务保存失败，请稍后重试'
  if (message && !/exception|stack|dify|sql|\/api|http/i.test(message)) return message
  return fallback
}

function isActiveSuccessPlan(plan) {
  return plan?.status === 'active' && plan?.call_status === 'success'
}

const GENERATE_OPTIONS_KEY = 'lifePlan.generateOptions'
const DEFAULT_GENERATE_OPTIONS = {
  plan_goal: '综合生活干预',
  avoid_items: [],
  plan_days: 7
}

function normalizeGenerateOptions(payload = {}) {
  return {
    plan_goal: payload.plan_goal || payload.planGoal || DEFAULT_GENERATE_OPTIONS.plan_goal,
    avoid_items: Array.isArray(payload.avoid_items) ? payload.avoid_items.filter(Boolean) : [],
    plan_days: payload.plan_days ?? DEFAULT_GENERATE_OPTIONS.plan_days
  }
}

function loadGenerateOptions() {
  if (typeof window === 'undefined') return null
  try {
    const raw = window.localStorage.getItem(GENERATE_OPTIONS_KEY)
    return raw ? normalizeGenerateOptions(JSON.parse(raw)) : null
  } catch {
    return null
  }
}

function saveGenerateOptions(options) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(GENERATE_OPTIONS_KEY, JSON.stringify(normalizeGenerateOptions(options)))
  } catch {
    // 本地缓存只用于下次重新生成时沿用用户偏好。
  }
}

export const useLifePlanStore = defineStore('lifePlan', {
  state: () => ({
    currentPlan: null,
    plans: [],
    detail: null,
    loading: false,
    detailLoading: false,
    generating: false,
    error: '',
    detailError: '',
    generateError: '',
    lastGenerateOptions: loadGenerateOptions()
  }),
  getters: {
    currentGenerateOptions(state) {
      const currentGoal = state.currentPlan?.plan_goal || state.currentPlan?.planGoal
      return normalizeGenerateOptions({
        ...state.lastGenerateOptions,
        plan_goal: state.lastGenerateOptions?.plan_goal || currentGoal || DEFAULT_GENERATE_OPTIONS.plan_goal
      })
    }
  },
  actions: {
    async fetchLifePlans() {
      this.loading = true
      this.error = ''
      try {
        const response = await getLifePlans()
        this.plans = normalizeList(response)
        this.currentPlan = this.plans.find(isActiveSuccessPlan) || this.plans[0] || null
        return this.plans
      } catch (error) {
        this.error = resolveErrorMessage(error, '生活方案加载失败')
        this.plans = []
        this.currentPlan = null
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchCurrentPlan() {
      await this.fetchLifePlans()
      return this.currentPlan
    },
    async fetchLifePlanDetail(planId) {
      this.detailLoading = true
      this.detailError = ''
      try {
        const response = await getLifePlanDetail(planId)
        this.detail = unwrapResponse(response)
        return this.detail
      } catch (error) {
        this.detailError = resolveErrorMessage(error, '方案详情加载失败')
        this.detail = null
        throw error
      } finally {
        this.detailLoading = false
      }
    },
    async generateLifePlan(payload) {
      if (this.generating) return null

      this.generating = true
      this.generateError = ''
      const safePayload = normalizeGenerateOptions(payload)
      this.lastGenerateOptions = safePayload
      saveGenerateOptions(safePayload)
      try {
        const response = await generateLifePlan(safePayload)
        const plan = unwrapResponse(response)
        await this.fetchLifePlans()
        return plan
      } catch (error) {
        this.generateError = resolveErrorMessage(error, '生活方案生成失败，请稍后重试')
        try {
          await this.fetchLifePlans()
        } catch {
          // 页面保留正式错误态，避免暴露底层接口信息。
        }
        throw error
      } finally {
        this.generating = false
      }
    }
  }
})
