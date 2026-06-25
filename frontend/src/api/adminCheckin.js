import request from './request'

export function getAdminCheckinRecords(params = {}) {
  return request.get('/admin/checkins/records', { params })
}

export function getAdminCheckinRecordDetail(checkinId) {
  return request.get(`/admin/checkins/records/${checkinId}`)
}

export function getAdminCheckinOverview(params = {}) {
  return request.get('/admin/checkins/overview', { params })
}

export function getAdminCheckinAnalyses(params = {}) {
  return request.get('/admin/checkins/analyses', { params })
}

export function getAdminCheckinAnalysisDetail(analysisId) {
  return request.get(`/admin/checkins/analyses/${analysisId}`)
}

export function getAdminInactiveUsers(params = {}) {
  return request.get('/admin/checkins/inactive-users', { params })
}

export function getAdminCheckinAnalysisLogs(params = {}) {
  return request.get('/admin/checkins/analysis-logs', { params })
}
