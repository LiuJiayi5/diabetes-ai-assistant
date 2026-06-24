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

  if (message) return message
  if (status === 401) return '登录已失效，请重新登录'
  if (status === 403) return '当前账号无权限生成生活方案'
  if (status === 400) return '缺少健康档案、健康数据或成功风险评估'
  if (status === 502) return 'AI 方案生成失败，请稍后重试'
  if (status === 500) return '服务器保存失败，请稍后重试'
  return fallback
}

function isActiveSuccessPlan(plan) {
  return plan?.status === 'active' && plan?.call_status === 'success'
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
    generateError: ''
  }),
  actions: {
    async fetchLifePlans() {
      this.loading = true
      this.error = ''
      try {
        const response = await getLifePlans()
        this.plans = normalizeList(response)
        this.currentPlan = this.plans.find(isActiveSuccessPlan) || null
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
      try {
        const response = await generateLifePlan(payload)
        const plan = unwrapResponse(response)
        await this.fetchLifePlans()
        return plan
      } catch (error) {
        this.generateError = resolveErrorMessage(error, '生活方案生成失败')
        throw error
      } finally {
        this.generating = false
      }
    }
  }
})
