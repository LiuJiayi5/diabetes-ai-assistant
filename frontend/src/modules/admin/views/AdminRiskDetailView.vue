<template>
  <div class="admin-page">
    <div class="admin-page__header">
      <h1>评估详情 · #{{ route.params.assessmentId }}</h1>
      <button class="admin-btn admin-btn--ghost" type="button" @click="router.push('/admin/risk-assessments')">返回列表</button>
    </div>

    <div v-if="loading" class="admin-loading">加载中...</div>
    <div v-else-if="detail" class="admin-table-wrap" style="padding: 20px">
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">
        <RiskLevelTag :level="detail.risk_level" />
        <span v-if="detail.risk_score != null" style="font-size:14px;font-weight:600">评分 {{ detail.risk_score }}</span>
        <span class="admin-status" :class="detail.call_status === 'success' ? 'admin-status--success' : 'admin-status--failed'">
          {{ detail.call_status === 'success' ? '调用成功' : '调用失败' }}
        </span>
      </div>

      <div class="admin-detail-grid">
        <div class="admin-detail-item">
          <span class="admin-detail-item__label">评估时间</span>
          <span class="admin-detail-item__value">{{ formatDateTime(detail.create_time) }}</span>
        </div>
        <div class="admin-detail-item">
          <span class="admin-detail-item__label">类型倾向</span>
          <span class="admin-detail-item__value">{{ detail.diabetes_type_tendency || '—' }}</span>
        </div>
      </div>

      <div class="admin-detail-block">
        <h3>总结</h3>
        <p>{{ detail.summary || '—' }}</p>
      </div>

      <div v-if="detail.main_risk_factors?.length" class="admin-detail-block">
        <h3>主要风险因素</h3>
        <ul>
          <li v-for="(factor, index) in detail.main_risk_factors" :key="index">{{ factor }}</li>
        </ul>
      </div>

      <div v-if="detail.indicator_analysis" class="admin-detail-block">
        <h3>指标分析</h3>
        <p>{{ detail.indicator_analysis }}</p>
      </div>

      <div v-if="detail.health_advice" class="admin-detail-block">
        <h3>健康建议</h3>
        <p style="white-space:pre-line">{{ detail.health_advice }}</p>
      </div>

      <div v-if="detail.medical_warning" class="admin-detail-block">
        <h3>就医提醒</h3>
        <p>{{ detail.medical_warning }}</p>
      </div>

      <div v-if="detail.request_summary" class="admin-detail-block">
        <h3>请求摘要</h3>
        <p>{{ detail.request_summary }}</p>
      </div>

      <div v-if="detail.error_message" class="admin-detail-block">
        <h3>错误信息</h3>
        <p>{{ detail.error_message }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import RiskLevelTag from '@/components/mobile/RiskLevelTag.vue'
import { adminGetRiskDetail } from '@/api/riskAssessment'
import { assertSuccess } from '@/utils/response'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const detail = ref(null)

onMounted(loadDetail)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = assertSuccess(await adminGetRiskDetail(route.params.assessmentId))
  } catch (error) {
    showToast(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import '@/styles/admin-page.css';
</style>
