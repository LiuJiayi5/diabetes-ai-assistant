import request from './request'
import { getTokenForRequest } from '@/utils/token'

export function listReports(params = {}) {
  return request.get('/reports', { params })
}

export function getReportDetail(reportId) {
  return request.get(`/reports/${encodeURIComponent(reportId)}`)
}

export function generateReport(payload) {
  return request.post('/reports', {
    report_type: payload.report_type || payload.reportType || 'personal',
    days: payload.days ?? 30
  }, { timeout: 120000 })
}

export function getReportExportUrl(reportId, type) {
  const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
  return `${baseURL}/reports/${encodeURIComponent(reportId)}/export/${encodeURIComponent(type)}`
}

export async function downloadReportExport(reportId, type) {
  const url = `/reports/${encodeURIComponent(reportId)}/export/${encodeURIComponent(type)}`
  const token = getTokenForRequest(url)
  const response = await request.get(url, {
    responseType: 'blob',
    transformResponse: [(data) => data],
    headers: token ? { Authorization: `Bearer ${token}` } : undefined
  })
  return response
}
