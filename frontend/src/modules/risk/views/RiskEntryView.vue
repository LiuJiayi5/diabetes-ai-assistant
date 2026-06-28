<template>
  <div class="figma-page risk-page">
    <PageHeader title="糖尿病风险预测" :show-back="false" />

    <section class="risk-hero">
      <span class="risk-hero__glow risk-hero__glow--green"></span>
      <span class="risk-hero__glow risk-hero__glow--blue"></span>
      <div class="risk-hero__content">
        <div class="risk-hero__copy">
          <p class="eyebrow">基于档案与最新健康数据</p>
          <h2>智能风险评估</h2>
          <p>{{ latestMetricSummary }}</p>
        </div>
        <div class="risk-hero__score">
          <span>{{ latestAssessment?.risk_score ?? '--' }}</span>
          <small>风险分</small>
        </div>
      </div>
      <div class="metric-chip-row">
        <span>{{ metricChipText('fasting_glucose', '空腹血糖', 'mmol/L') }}</span>
        <span>{{ metricChipText('weight_kg', '体重', 'kg') }}</span>
        <span>{{ pressureChipText }}</span>
      </div>
      <button class="hero-action" type="button" @click="metricPopup = true">
        <PlusCircle :size="17" />
        <span>录入今日数据</span>
      </button>
    </section>

    <section v-if="!entry?.can_predict" class="state-card">
      <div class="state-card__icon">
        <ShieldAlert :size="20" />
      </div>
      <div>
        <h3>还不能发起评估</h3>
        <p>{{ entry?.missing_reason || '请先完善健康档案并录入健康数据。' }}</p>
      </div>
      <button
        v-if="!hasProfile"
        class="ghost-button"
        type="button"
        @click="router.push('/app/profile')"
      >
        去填写健康档案
      </button>
    </section>

    <section v-if="latestAssessment" class="latest-card">
      <div class="latest-card__head">
        <div>
          <p class="eyebrow">最新评估</p>
          <h3>{{ latestTitle }}</h3>
        </div>
        <RiskLevelTag :level="latestAssessment.risk_level" />
      </div>
      <p class="latest-card__summary">{{ latestAssessment.summary || latestAssessment.request_summary || '暂无摘要' }}</p>
      <p v-if="latestAssessment.call_status === 'failed'" class="risk-result__error">
        {{ latestAssessment.error_message || 'AI 服务暂不可用' }}
      </p>
      <button
        v-if="latestAssessment.assessment_id"
        class="link-row"
        type="button"
        @click="router.push(`/app/risk/${latestAssessment.assessment_id}`)"
      >
        <span>查看完整报告</span>
        <ChevronRight :size="17" />
      </button>
    </section>

    <section class="action-panel">
      <button
        v-if="entry?.can_predict"
        class="primary-action"
        type="button"
        :disabled="predicting"
        @click="handlePredict"
      >
        <LoaderCircle v-if="predicting" class="spin" :size="17" />
        <Activity v-else :size="17" />
        <span>{{ predicting ? '评估中...' : '重新发起评估' }}</span>
      </button>

      <button class="secondary-action" type="button" @click="router.push('/app/risk/history')">
        <History :size="17" />
        <span>查看评估记录</span>
      </button>
    </section>

    <section v-if="hasTrendData" class="trend-section">
      <div class="trend-section__head">
        <h3>趋势可视化</h3>
        <p>跟踪风险分数与关键健康指标变化</p>
      </div>
      <div class="trend-card">
        <TrendLineChart
          title="风险分数趋势"
          unit="分"
          color="#16A34A"
          :points="riskTrendPoints"
          value-key="risk_score"
          time-key="recorded_at"
        />
      </div>
      <div v-for="series in metricTrendSeries" :key="series.key" class="trend-card">
        <TrendLineChart
          v-if="series.points?.length"
          :title="`${series.label}趋势`"
          :unit="series.unit"
          :color="seriesColor(series.key)"
          :points="series.points"
        />
      </div>
    </section>

    <p class="figma-disclaimer">AI 建议仅供参考，不能替代线下诊疗。</p>

    <div v-if="metricPopup" class="metric-sheet-layer" @click.self="metricPopup = false">
      <section class="metric-sheet">
        <div class="metric-sheet__handle"></div>
        <div class="metric-sheet__head">
          <div>
            <p class="eyebrow">今日指标</p>
            <h3>录入健康数据</h3>
          </div>
          <button type="button" class="sheet-close" @click="metricPopup = false">关闭</button>
        </div>
        <van-form class="metric-form" @submit="handleSaveMetric">
          <van-field v-model="metricForm.recorded_at" label="记录日期" placeholder="YYYY-MM-DD" required />
          <van-field v-model="metricForm.weight_kg" type="number" label="体重(kg)" placeholder="如 68.5" />
          <van-field v-model="metricForm.waist_cm" type="number" label="腰围(cm)" placeholder="如 85" />
          <van-field v-model="metricForm.systolic_bp" type="digit" label="收缩压" placeholder="如 120" />
          <van-field v-model="metricForm.diastolic_bp" type="digit" label="舒张压" placeholder="如 80" />
          <van-field v-model="metricForm.fasting_glucose" type="number" label="空腹血糖" placeholder="mmol/L" />
          <van-field v-model="metricForm.postprandial_glucose" type="number" label="餐后血糖" placeholder="mmol/L" />
          <van-field v-model="metricForm.hba1c" type="number" label="糖化血红蛋白" placeholder="%" />
          <van-field v-model="metricForm.diet_status" label="饮食状态" placeholder="如：主食偏多" />
          <van-field v-model="metricForm.exercise_status" label="运动状态" placeholder="如：每周运动2次" />
          <button class="primary-action metric-submit" type="submit" :disabled="savingMetric">
            <LoaderCircle v-if="savingMetric" class="spin" :size="17" />
            <CheckCircle2 v-else :size="17" />
            <span>{{ savingMetric ? '保存中...' : '保存并评估' }}</span>
          </button>
        </van-form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Form as VanForm, Field as VanField, showToast } from 'vant'
import {
  Activity,
  CheckCircle2,
  ChevronRight,
  History,
  LoaderCircle,
  PlusCircle,
  ShieldAlert
} from 'lucide-vue-next'
import PageHeader from '@/components/mobile/PageHeader.vue'
import RiskLevelTag from '@/components/mobile/RiskLevelTag.vue'
import TrendLineChart from '@/components/charts/TrendLineChart.vue'
import { getMyProfile } from '@/api/profile'
import { getLatestMetric, getMetricTrends, saveMetric } from '@/api/healthMetric'
import { getRiskEntry, getRiskTrends, predictRisk } from '@/api/riskAssessment'
import { assertSuccess } from '@/utils/response'
import { buildMetricSummary, formatRiskLevel, todayString } from '@/utils/health'

const router = useRouter()
const entry = ref(null)
const latestAssessment = ref(null)
const latestMetric = ref(null)
const hasProfile = ref(false)
const metricPopup = ref(false)
const savingMetric = ref(false)
const predicting = ref(false)
const riskTrendPoints = ref([])
const metricTrendSeries = ref([])

const metricForm = reactive({
  recorded_at: todayString(),
  weight_kg: '',
  waist_cm: '',
  systolic_bp: '',
  diastolic_bp: '',
  fasting_glucose: '',
  postprandial_glucose: '',
  hba1c: '',
  diet_status: '',
  exercise_status: ''
})

const latestMetricSummary = computed(() => buildMetricSummary(latestMetric.value))
const latestTitle = computed(() => {
  const level = formatRiskLevel(latestAssessment.value?.risk_level)
  return level && level !== '—' ? `${level} · 综合报告` : '综合报告'
})

const pressureChipText = computed(() => {
  const metric = latestMetric.value
  if (metric?.systolic_bp != null && metric?.diastolic_bp != null) {
    return `血压 ${metric.systolic_bp}/${metric.diastolic_bp}`
  }
  return '血压 待录入'
})

const hasTrendData = computed(() =>
  riskTrendPoints.value.length > 0 || metricTrendSeries.value.some((series) => series.points?.length > 1)
)

function seriesColor(key) {
  return {
    fasting_glucose: '#0284C7',
    weight_kg: '#D97706',
    waist_cm: '#7C3AED'
  }[key] || '#16A34A'
}

onMounted(loadPage)

async function loadPage() {
  try {
    const [entryData, metricData, profileData, riskTrendData, metricTrendData] = await Promise.all([
      getRiskEntry(),
      getLatestMetric(),
      getMyProfile(),
      getRiskTrends(),
      getMetricTrends()
    ])
    entry.value = assertSuccess(entryData)
    latestMetric.value = assertSuccess(metricData)
    const profile = assertSuccess(profileData)
    hasProfile.value = Boolean(profile)
    latestAssessment.value = entry.value?.latest_assessment || null

    const riskTrend = assertSuccess(riskTrendData)
    riskTrendPoints.value = (riskTrend?.points || []).map((item) => ({
      risk_score: item.risk_score ?? item.riskScore,
      recorded_at: item.recorded_at || item.recordedAt
    }))

    const metricTrend = assertSuccess(metricTrendData)
    metricTrendSeries.value = metricTrend?.series || []
  } catch (error) {
    showToast(error.message || '加载失败')
  }
}

function metricChipText(field, label, unit) {
  const value = latestMetric.value?.[field]
  return value == null ? `${label} 待录入` : `${label} ${value} ${unit}`
}

function buildMetricPayload() {
  const pickNumber = (value) => (value === '' || value == null ? null : Number(value))
  const pickInt = (value) => (value === '' || value == null ? null : parseInt(value, 10))
  return {
    recorded_at: metricForm.recorded_at,
    weight_kg: pickNumber(metricForm.weight_kg),
    waist_cm: pickNumber(metricForm.waist_cm),
    systolic_bp: pickInt(metricForm.systolic_bp),
    diastolic_bp: pickInt(metricForm.diastolic_bp),
    fasting_glucose: pickNumber(metricForm.fasting_glucose),
    postprandial_glucose: pickNumber(metricForm.postprandial_glucose),
    hba1c: pickNumber(metricForm.hba1c),
    diet_status: metricForm.diet_status || null,
    exercise_status: metricForm.exercise_status || null
  }
}

async function handleSaveMetric() {
  savingMetric.value = true
  try {
    assertSuccess(await saveMetric(buildMetricPayload()))
    metricPopup.value = false
    showToast('健康数据已保存')
    await loadPage()

    if (!hasProfile.value) {
      showToast('请先完善健康档案后再评估')
      return
    }

    await handlePredict()
  } catch (error) {
    showToast(error.message || '保存失败')
  } finally {
    savingMetric.value = false
  }
}

async function handlePredict() {
  predicting.value = true
  try {
    const result = assertSuccess(await predictRisk())
    latestAssessment.value = result
    showToast('评估完成')
    await loadPage()
  } catch (error) {
    if (error.code === 400) {
      showToast(error.message || '请先补齐档案和健康数据')
    } else if (error.code === 502) {
      showToast(error.message || 'AI 服务暂不可用，请稍后重试')
      await loadPage()
    } else {
      showToast(error.message || '评估失败')
    }
  } finally {
    predicting.value = false
  }
}
</script>

<style scoped>
.risk-page {
  position: relative;
  padding-bottom: calc(90px + env(safe-area-inset-bottom));
}

:deep(.page-header__title) {
  white-space: normal;
  word-break: keep-all;
  overflow-wrap: normal;
  line-height: 1.25;
}

.risk-hero,
.state-card,
.latest-card,
.action-panel {
  box-shadow: var(--figma-shadow-card);
}

.risk-hero {
  position: relative;
  overflow: hidden;
  margin-bottom: 14px;
  padding: 20px;
  border-radius: var(--figma-radius-card-lg);
  background: linear-gradient(145deg, #FFFFFF 0%, #EEF8F3 58%, #EAF5FA 100%);
}

.risk-hero__glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(22px);
  pointer-events: none;
}

.risk-hero__glow--green {
  top: -34px;
  right: 54px;
  width: 140px;
  height: 140px;
  background: rgba(174, 232, 199, 0.23);
}

.risk-hero__glow--blue {
  right: -32px;
  bottom: -34px;
  width: 148px;
  height: 148px;
  background: rgba(191, 233, 242, 0.25);
}

.risk-hero__content,
.metric-chip-row,
.hero-action {
  position: relative;
  z-index: 1;
}

.risk-hero__content {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.risk-hero__copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 6px;
  color: rgba(36, 50, 61, 0.62);
  font-size: 11px;
  font-weight: 600;
}

.risk-hero h2 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 21px;
  font-weight: 800;
  line-height: 1.35;
}

.risk-hero p:not(.eyebrow) {
  margin: 9px 0 0;
  color: var(--figma-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.risk-hero__score {
  width: 74px;
  height: 74px;
  flex: 0 0 74px;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.66);
  box-shadow: inset 0 0 0 8px rgba(229, 246, 238, 0.78);
  color: #3C8D66;
}

.risk-hero__score span {
  font-size: 20px;
  font-weight: 800;
  line-height: 1;
}

.risk-hero__score small {
  margin-top: 5px;
  font-size: 10px;
  font-weight: 700;
}

.metric-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.metric-chip-row span {
  padding: 6px 10px;
  border-radius: var(--figma-radius-pill);
  background: rgba(255, 255, 255, 0.58);
  color: #4A7A62;
  font-size: 11px;
  font-weight: 600;
}

.hero-action,
.primary-action,
.secondary-action,
.ghost-button,
.link-row {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
}

.hero-action,
.primary-action {
  min-height: 46px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 700;
  box-shadow: var(--figma-shadow-button);
}

.hero-action {
  width: 100%;
  margin-top: 16px;
}

.state-card,
.latest-card,
.action-panel {
  margin-bottom: 12px;
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
}

.state-card {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 11px;
  padding: 16px;
}

.state-card__icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: #F8EED8;
  color: #C48A32;
}

.state-card h3,
.latest-card h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 15px;
  font-weight: 700;
}

.state-card p,
.latest-card__summary {
  margin: 5px 0 0;
  color: var(--figma-text-secondary);
  font-size: 13px;
  line-height: 1.75;
}

.ghost-button {
  grid-column: 1 / -1;
  min-height: 40px;
  border-radius: var(--figma-radius-pill);
  background: #F7FCF9;
  color: var(--figma-tabbar-active);
  font-size: 13px;
  font-weight: 700;
}

.latest-card {
  padding: 16px;
}

.latest-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.risk-result__error {
  margin: 12px 0 0;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(239, 143, 143, 0.10);
  color: var(--figma-error);
  font-size: 12px;
  line-height: 1.6;
}

.link-row {
  width: 100%;
  min-height: 42px;
  justify-content: space-between;
  margin-top: 12px;
  padding: 0 12px 0 14px;
  border-radius: 16px;
  background: #F7FCF9;
  color: #4FB783;
  font-size: 13px;
  font-weight: 700;
}

.action-panel {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.primary-action,
.secondary-action {
  width: 100%;
}

.primary-action:disabled {
  opacity: 0.72;
}

.trend-section {
  margin-bottom: 12px;
  padding: 16px;
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.trend-section__head h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 16px;
  font-weight: 800;
}

.trend-section__head p {
  margin: 6px 0 14px;
  color: var(--figma-text-secondary);
  font-size: 12px;
}

.trend-card {
  margin-bottom: 12px;
  padding: 8px 4px 0;
  border-radius: 18px;
  background: #F8FCFA;
}

.trend-card:last-child {
  margin-bottom: 0;
}

.secondary-action {
  min-height: 44px;
  border: 1px solid rgba(174, 232, 199, 0.62);
  border-radius: var(--figma-radius-pill);
  background: #FFFFFF;
  color: var(--figma-tabbar-active);
  font-size: 13px;
  font-weight: 700;
}

.metric-sheet-layer {
  position: absolute;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: flex-end;
  background: rgba(23, 40, 35, 0.50);
  border-radius: var(--figma-device-radius);
}

.metric-sheet {
  width: 100%;
  max-height: 82%;
  overflow-y: auto;
  padding: 10px 18px 18px;
  border-radius: 28px 28px 0 0;
  background: var(--figma-bg-page);
  box-shadow: 0 -12px 30px rgba(55, 80, 70, 0.16);
}

.metric-sheet__handle {
  width: 44px;
  height: 4px;
  margin: 0 auto 14px;
  border-radius: 999px;
  background: rgba(122, 135, 148, 0.28);
}

.metric-sheet__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.metric-sheet__head h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 18px;
  font-weight: 800;
}

.sheet-close {
  min-height: 32px;
  padding: 0 12px;
  border-radius: var(--figma-radius-pill);
  background: rgba(174, 232, 199, 0.30);
  color: #4A8A6A;
  font-size: 12px;
  font-weight: 700;
}

.metric-form {
  display: grid;
  gap: 9px;
}

.metric-submit {
  margin-top: 8px;
}

:deep(.van-cell) {
  border-radius: 16px;
  background: #FFFFFF;
  color: var(--figma-text-strong);
}

:deep(.van-cell::after) {
  display: none;
}

:deep(.van-field__label) {
  color: var(--figma-text-secondary);
  font-size: 12px;
  font-weight: 500;
}

:deep(.van-field__control) {
  color: var(--figma-text-strong);
  font-size: 13px;
}

.spin {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
