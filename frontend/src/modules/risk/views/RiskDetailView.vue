<template>
  <div class="figma-page risk-detail-page">
    <PageHeader title="评估详情" back-to="/app/risk/history" />

    <div v-if="loading" class="figma-empty">加载中...</div>

    <template v-else-if="detail">
      <section class="detail-hero">
        <span class="detail-hero__glow detail-hero__glow--green"></span>
        <span class="detail-hero__glow detail-hero__glow--blue"></span>
        <div class="detail-hero__top">
          <div>
            <p class="eyebrow">AI 风险报告</p>
            <h2>{{ riskTitle }}</h2>
          </div>
          <RiskLevelTag :level="detail.risk_level" />
        </div>
        <p class="detail-hero__summary">{{ detail.summary || '暂无总结' }}</p>
        <div class="detail-hero__meta">
          <span v-if="detail.risk_score != null">评分 {{ detail.risk_score }}</span>
          <span>{{ formatDateTime(detail.create_time) || '暂无时间' }}</span>
        </div>
      </section>

      <section class="report-card">
        <div class="section-title">
          <FileText :size="17" />
          <h3>报告摘要</h3>
        </div>
        <p class="block-text">{{ detail.summary || '本次评估暂未生成摘要。' }}</p>
        <p v-if="detail.call_status === 'failed'" class="error-note">
          {{ detail.error_message || 'AI 服务暂不可用，请稍后重新评估。' }}
        </p>
      </section>

      <section v-if="detail.main_risk_factors?.length" class="report-card">
        <div class="section-title">
          <AlertTriangle :size="17" />
          <h3>主要风险因素</h3>
        </div>
        <div class="factor-grid">
          <span v-for="(factor, index) in detail.main_risk_factors" :key="index">{{ factor }}</span>
        </div>
      </section>

      <section class="report-grid">
        <article v-if="detail.diabetes_type_tendency" class="mini-card">
          <div class="mini-card__icon">
            <Activity :size="17" />
          </div>
          <h3>类型倾向</h3>
          <p>{{ detail.diabetes_type_tendency }}</p>
        </article>

        <article v-if="detail.indicator_analysis" class="mini-card mini-card--wide">
          <div class="mini-card__icon mini-card__icon--blue">
            <ChartNoAxesColumnIncreasing :size="17" />
          </div>
          <h3>指标分析</h3>
          <p class="pre-line">{{ detail.indicator_analysis }}</p>
        </article>

        <article v-if="detail.health_advice" class="mini-card mini-card--wide">
          <div class="mini-card__icon">
            <HeartPulse :size="17" />
          </div>
          <h3>健康建议</h3>
          <p class="pre-line">{{ detail.health_advice }}</p>
        </article>

        <article v-if="detail.medical_warning" class="mini-card mini-card--wide warning-card">
          <div class="mini-card__icon mini-card__icon--amber">
            <ShieldAlert :size="17" />
          </div>
          <h3>就医提醒</h3>
          <p class="pre-line">{{ detail.medical_warning }}</p>
        </article>
      </section>

      <section v-if="detail.reference_sources?.length" class="report-card reference-card">
        <div class="section-title">
          <BookOpen :size="17" />
          <h3>知识库参考依据</h3>
        </div>
        <ul class="reference-list">
          <li v-for="(source, index) in detail.reference_sources" :key="index">{{ source }}</li>
        </ul>
      </section>

      <section v-if="detail.request_summary" class="report-card request-card">
        <div class="section-title">
          <ClipboardList :size="17" />
          <h3>本次输入摘要</h3>
        </div>
        <p class="block-text">{{ detail.request_summary }}</p>
      </section>

      <section v-if="isSparseReport" class="report-card empty-report">
        <div class="section-title">
          <Sparkles :size="17" />
          <h3>详细分析待生成</h3>
        </div>
        <p class="block-text">
          当前记录只包含基础摘要。重新发起评估后，这里会按“风险因素、指标分析、健康建议、就医提醒”拆分展示，更适合阅读较长的工作流结果。
        </p>
      </section>

      <p class="figma-disclaimer">AI 建议仅供参考，不能替代线下诊疗。</p>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import {
  Activity,
  AlertTriangle,
  BookOpen,
  ChartNoAxesColumnIncreasing,
  ClipboardList,
  FileText,
  HeartPulse,
  ShieldAlert,
  Sparkles
} from 'lucide-vue-next'
import PageHeader from '@/components/mobile/PageHeader.vue'
import RiskLevelTag from '@/components/mobile/RiskLevelTag.vue'
import { getRiskDetail } from '@/api/riskAssessment'
import { assertSuccess } from '@/utils/response'
import { formatDateTime } from '@/utils/format'
import { formatRiskLevel } from '@/utils/health'

const route = useRoute()
const loading = ref(true)
const detail = ref(null)

const riskTitle = computed(() => {
  const label = formatRiskLevel(detail.value?.risk_level)
  return label && label !== '—' ? `${label}评估` : '综合风险评估'
})

const isSparseReport = computed(() => {
  if (!detail.value) return false
  return !detail.value.diabetes_type_tendency
    && !detail.value.main_risk_factors?.length
    && !detail.value.indicator_analysis
    && !detail.value.health_advice
    && !detail.value.medical_warning
})

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
.risk-detail-page {
  padding-bottom: calc(90px + env(safe-area-inset-bottom));
}

.detail-hero,
.report-card,
.mini-card {
  box-shadow: var(--figma-shadow-card);
}

.detail-hero {
  position: relative;
  overflow: hidden;
  min-height: 158px;
  margin-bottom: 14px;
  padding: 20px;
  border-radius: var(--figma-radius-card-lg);
  background: linear-gradient(145deg, #FFFFFF 0%, #EEF8F3 58%, #EAF5FA 100%);
}

.detail-hero__glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(22px);
  pointer-events: none;
}

.detail-hero__glow--green {
  top: -36px;
  right: 58px;
  width: 138px;
  height: 138px;
  background: rgba(174, 232, 199, 0.22);
}

.detail-hero__glow--blue {
  right: -30px;
  bottom: -34px;
  width: 150px;
  height: 150px;
  background: rgba(191, 233, 242, 0.25);
}

.detail-hero__top,
.detail-hero__summary,
.detail-hero__meta {
  position: relative;
  z-index: 1;
}

.detail-hero__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.eyebrow {
  margin: 0 0 6px;
  color: rgba(36, 50, 61, 0.62);
  font-size: 11px;
  font-weight: 600;
}

.detail-hero h2 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 21px;
  font-weight: 800;
  line-height: 1.35;
}

.detail-hero__summary {
  margin: 16px 0 0;
  color: var(--figma-text-secondary);
  font-size: 13px;
  line-height: 1.75;
}

.detail-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.detail-hero__meta span {
  padding: 5px 11px;
  border-radius: var(--figma-radius-pill);
  background: rgba(255, 255, 255, 0.58);
  color: #4A7A62;
  font-size: 11px;
  font-weight: 600;
}

.report-card,
.mini-card {
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
}

.report-card {
  margin-bottom: 12px;
  padding: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: #4FB783;
}

.section-title h3,
.mini-card h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 15px;
  font-weight: 700;
}

.block-text,
.mini-card p {
  margin: 0;
  color: var(--figma-text-secondary);
  font-size: 13px;
  line-height: 1.85;
}

.error-note {
  margin: 12px 0 0;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(239, 143, 143, 0.10);
  color: var(--figma-error);
  font-size: 12px;
  line-height: 1.6;
}

.factor-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.factor-grid span {
  max-width: 100%;
  padding: 7px 11px;
  border-radius: 14px;
  background: #F7FCF9;
  color: #4A7A62;
  font-size: 12px;
  line-height: 1.45;
}

.report-grid {
  display: grid;
  gap: 12px;
}

.mini-card {
  padding: 16px;
}

.mini-card__icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  margin-bottom: 10px;
  border-radius: 14px;
  background: #E5F6EE;
  color: #4FB783;
}

.mini-card__icon--blue {
  background: #E4F3FB;
  color: #4FAAC4;
}

.mini-card__icon--amber {
  background: #F8EED8;
  color: #C48A32;
}

.mini-card p {
  margin-top: 8px;
}

.warning-card {
  background: linear-gradient(145deg, #FFFFFF 0%, #FFF8EC 100%);
}

.request-card {
  margin-top: 12px;
  background: var(--figma-info-soft);
}

.empty-report {
  background: rgba(255, 255, 255, 0.72);
}

.reference-card {
  background: linear-gradient(145deg, #FFFFFF 0%, #F4FBF7 100%);
}

.reference-list {
  margin: 0;
  padding-left: 18px;
  color: var(--figma-text-secondary);
  font-size: 13px;
  line-height: 1.8;
}

.pre-line {
  white-space: pre-line;
}
</style>
