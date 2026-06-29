<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <el-button text class="back-btn" @click="router.push('/admin/checkin-analysis')">
          <ArrowLeft :size="16" /> 返回调用日志
        </el-button>
        <h1 class="admin-page-title">打卡分析调用日志详情</h1>
        <p class="admin-page-desc">查看 Dify 打卡行为分析的输入、输出、调用状态和错误信息。</p>
      </div>
      <el-tag v-if="log" :type="log.call_status === 'success' ? 'success' : 'danger'" round>
        {{ callStatusText(log.call_status) }}
      </el-tag>
    </div>

    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else-if="log">
      <section class="admin-card detail-card hero-card">
        <div class="log-title-row">
          <div>
            <h2>调用日志 #{{ log.log_id }}</h2>
            <p>
              {{ log.patient?.username || '未知用户' }}
              <span>·</span>
              用户 ID {{ log.user_id }}
              <span>·</span>
              {{ formatTime(log.create_time) }}
            </p>
          </div>
          <div class="tag-row">
            <el-tag type="info" round>{{ log.service_type || 'checkin_analysis' }}</el-tag>
            <el-tag :type="log.call_status === 'success' ? 'success' : 'danger'" round>
              {{ callStatusText(log.call_status) }}
            </el-tag>
          </div>
        </div>
      </section>

      <div class="admin-detail-grid">
        <section class="admin-card detail-card">
          <h3>基础信息</h3>
          <dl>
            <div class="admin-kv"><dt>日志 ID</dt><dd>#{{ log.log_id }}</dd></div>
            <div class="admin-kv"><dt>用户 ID</dt><dd>{{ log.user_id }}</dd></div>
            <div class="admin-kv"><dt>服务类型</dt><dd>{{ log.service_type || '-' }}</dd></div>
            <div class="admin-kv"><dt>调用时间</dt><dd>{{ formatTime(log.create_time) }}</dd></div>
          </dl>
        </section>

        <section class="admin-card detail-card">
          <h3>患者摘要</h3>
          <p class="summary-text">{{ log.patient?.profile_summary || '暂无患者摘要。' }}</p>
          <template v-if="log.error_message">
            <h3 class="danger-title">失败原因</h3>
            <p class="summary-text danger">{{ log.error_message }}</p>
          </template>
        </section>
      </div>

      <section class="admin-card detail-card">
        <h3>输入摘要</h3>
        <pre class="admin-code-box">{{ prettyText(log.request_summary) }}</pre>
      </section>

      <section class="admin-card detail-card">
        <h3>输出摘要</h3>
        <pre class="admin-code-box">{{ prettyText(log.response_summary) }}</pre>
      </section>

      <section v-if="log.error_message" class="admin-card detail-card detail-card--danger">
        <h3>错误信息</h3>
        <pre class="admin-code-box">{{ prettyText(log.error_message) }}</pre>
      </section>
    </template>

    <div v-else class="admin-card admin-empty">未找到调用日志</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'
import { getAdminCheckinAnalysisLogDetail } from '@/api/adminCheckin'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const log = ref(null)

onMounted(loadLog)

async function loadLog() {
  loading.value = true
  try {
    const response = await getAdminCheckinAnalysisLogDetail(route.params.logId)
    log.value = response.data
  } catch {
    log.value = null
  } finally {
    loading.value = false
  }
}

function callStatusText(value) {
  const map = { success: '成功', failed: '失败' }
  return map[value] || value || '-'
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '-'
}

function prettyText(value) {
  if (!value) return '暂无'
  if (typeof value !== 'string') return JSON.stringify(value, null, 2)
  const trimmed = value.trim()
  if (!trimmed) return '暂无'
  try {
    return JSON.stringify(JSON.parse(trimmed), null, 2)
  } catch {
    return trimmed
  }
}
</script>

<style scoped>
.back-btn {
  margin: 0 0 8px -10px;
  color: var(--admin-text-secondary);
}

.detail-card {
  padding: 18px 20px;
  margin-bottom: 16px;
}

.hero-card {
  background:
    linear-gradient(135deg, rgba(16, 185, 129, 0.08), rgba(59, 130, 246, 0.08)),
    #ffffff;
}

.log-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.log-title-row h2 {
  margin: 0 0 4px;
  color: var(--admin-text-title);
}

.log-title-row p,
.summary-text {
  margin: 0;
  color: var(--admin-text-secondary);
  line-height: 1.7;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-card h3 {
  margin: 0 0 12px;
  color: var(--admin-text-title);
  font-size: 16px;
}

.danger-title {
  margin-top: 16px !important;
  color: var(--admin-danger-text) !important;
}

.danger {
  color: var(--admin-danger-text);
}

.detail-card--danger {
  border: 1px solid rgba(239, 68, 68, 0.16);
}

.admin-code-box {
  max-height: 420px;
  overflow: auto;
  margin: 0;
  padding: 14px 16px;
  border: 1px solid var(--admin-border-subtle);
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: anywhere;
}

@media (max-width: 768px) {
  .log-title-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
