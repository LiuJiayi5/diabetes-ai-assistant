<template>
  <div class="figma-page">
    <PageHeader title="评估详情" back-to="/app/risk/history" />

    <div v-if="loading" class="figma-empty">加载中...</div>

    <template v-else-if="detail">
      <FigmaCard variant="gradient">
        <div class="risk-result__head">
          <RiskLevelTag :level="detail.risk_level" />
          <span v-if="detail.risk_score != null" class="risk-result__score">评分 {{ detail.risk_score }}</span>
        </div>
        <p class="detail-summary">{{ detail.summary || '暂无总结' }}</p>
        <p class="detail-time">{{ formatDateTime(detail.create_time) }}</p>
      </FigmaCard>

      <FigmaCard v-if="detail.diabetes_type_tendency">
        <h3 class="block-title">类型倾向</h3>
        <p class="block-text">{{ detail.diabetes_type_tendency }}</p>
      </FigmaCard>

      <FigmaCard v-if="detail.main_risk_factors?.length">
        <h3 class="block-title">主要风险因素</h3>
        <ul class="factor-list">
          <li v-for="(factor, index) in detail.main_risk_factors" :key="index">{{ factor }}</li>
        </ul>
      </FigmaCard>

      <FigmaCard v-if="detail.indicator_analysis">
        <h3 class="block-title">指标分析</h3>
        <p class="block-text">{{ detail.indicator_analysis }}</p>
      </FigmaCard>

      <FigmaCard v-if="detail.health_advice">
        <h3 class="block-title">健康建议</h3>
        <p class="block-text pre-line">{{ detail.health_advice }}</p>
      </FigmaCard>

      <FigmaCard v-if="detail.medical_warning">
        <h3 class="block-title">就医提醒</h3>
        <p class="block-text warning">{{ detail.medical_warning }}</p>
      </FigmaCard>

      <p class="figma-disclaimer">AI 建议仅供参考，不能替代线下诊疗。</p>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import PageHeader from '@/components/mobile/PageHeader.vue'
import FigmaCard from '@/components/mobile/FigmaCard.vue'
import RiskLevelTag from '@/components/mobile/RiskLevelTag.vue'
import { getRiskDetail } from '@/api/riskAssessment'
import { assertSuccess } from '@/utils/response'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const loading = ref(true)
const detail = ref(null)

onMounted(loadDetail)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = assertSuccess(await getRiskDetail(route.params.assessmentId))
  } catch (error) {
    showToast(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.risk-result__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.risk-result__score,
.detail-time {
  font-size: 12px;
  color: var(--figma-text-muted);
}

.detail-summary {
  margin: 0 0 8px;
  font-size: 15px;
  line-height: 1.7;
  color: var(--figma-text-strong);
}

.block-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 700;
  color: var(--figma-text-strong);
}

.block-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: var(--figma-text-secondary);
}

.block-text.warning {
  color: #c67a2e;
}

.pre-line {
  white-space: pre-line;
}

.factor-list {
  margin: 0;
  padding-left: 18px;
  color: var(--figma-text-secondary);
  line-height: 1.8;
  font-size: 14px;
}
</style>
