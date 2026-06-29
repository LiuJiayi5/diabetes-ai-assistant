<template>
  <div class="admin-page">
    <div class="admin-page__header">
      <h1>评估详情 · #{{ route.params.assessmentId }}</h1>
      <button class="admin-btn admin-btn--ghost" type="button" @click="router.push('/admin/risk-assessments')">返回列表</button>
    </div>

    <div v-if="loading" class="admin-loading">加载中...</div>
    <div v-else-if="detail" class="admin-detail-layout">
      <section class="admin-table-wrap admin-detail-main">
        <div class="admin-detail-head">
          <RiskLevelTag :level="detail.risk_level" />
          <span v-if="detail.risk_score != null" class="admin-score">评分 {{ detail.risk_score }}</span>
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

        <div v-if="detail.reference_sources?.length" class="admin-detail-block">
          <h3>知识库参考依据</h3>
          <ul>
            <li v-for="(source, index) in detail.reference_sources" :key="index">{{ source }}</li>
          </ul>
        </div>

        <div v-if="detail.indicator_analysis" class="admin-detail-block">
          <h3>指标分析</h3>
          <p>{{ detail.indicator_analysis }}</p>
        </div>

        <div v-if="detail.health_advice" class="admin-detail-block">
          <h3>健康建议</h3>
          <p class="pre-line">{{ detail.health_advice }}</p>
        </div>

        <div v-if="detail.medical_warning" class="admin-detail-block">
          <h3>就医提醒</h3>
          <p>{{ detail.medical_warning }}</p>
        </div>
      </section>

      <aside class="admin-detail-side">
        <section class="admin-card side-card">
          <div class="side-card__head">
            <h3>相似案例 Top-K</h3>
            <span>{{ similarCases.length }} 条</span>
          </div>
          <div v-if="!similarCases.length" class="admin-empty compact-empty">暂无足够相似案例</div>
          <article v-for="item in similarCases" :key="item.user_id || item.userId" class="similar-case">
            <div class="similar-case__top">
              <strong>{{ item.username }}</strong>
              <span>{{ item.similarity_score ?? item.similarityScore }}%</span>
            </div>
            <p>{{ item.match_reason || item.matchReason }}</p>
            <small>
              {{ item.age }}岁 · 空腹血糖 {{ item.fasting_glucose ?? item.fastingGlucose ?? '—' }} ·
              风险 {{ item.risk_score ?? item.riskScore ?? '—' }} 分
            </small>
            <p v-if="item.summary" class="similar-case__summary">{{ item.summary }}</p>
          </article>
        </section>

        <section v-if="riskTrendPoints.length" class="admin-card side-card">
          <h3>风险分数趋势</h3>
          <TrendLineChart
            title=""
            unit="分"
            color="#0369A1"
            height="220px"
            :points="riskTrendPoints"
            value-key="risk_score"
            time-key="recorded_at"
          />
        </section>

        <section v-for="series in metricTrendSeries" :key="series.key" class="admin-card side-card">
          <TrendLineChart
            v-if="series.points?.length"
            :title="`${series.label}趋势`"
            :unit="series.unit"
            :color="seriesColor(series.key)"
            height="220px"
            :points="series.points"
          />
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import RiskLevelTag from '@/components/mobile/RiskLevelTag.vue'
import TrendLineChart from '@/components/charts/TrendLineChart.vue'
import {
  adminGetMetricTrends,
  adminGetRiskAssessmentDetail,
  adminGetRiskSimilarCases,
  adminGetRiskTrends
} from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const detail = ref(null)
const similarCases = ref([])
const riskTrendPoints = ref([])
const metricTrendSeries = ref([])

onMounted(loadDetail)

function seriesColor(key) {
  return {
    fasting_glucose: '#0284C7',
    weight_kg: '#D97706',
    waist_cm: '#7C3AED'
  }[key] || '#16A34A'
}

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await adminGetRiskAssessmentDetail(route.params.assessmentId)
    const userId = detail.value?.user_id ?? detail.value?.userId
    const [cases, riskTrend, metricTrend] = await Promise.all([
      adminGetRiskSimilarCases(route.params.assessmentId, { limit: 3 }),
      userId ? adminGetRiskTrends({ user_id: userId }) : Promise.resolve({ points: [] }),
      userId ? adminGetMetricTrends({ user_id: userId }) : Promise.resolve({ series: [] })
    ])
    similarCases.value = cases || []
    riskTrendPoints.value = (riskTrend?.points || []).map((item) => ({
      risk_score: item.risk_score ?? item.riskScore,
      recorded_at: item.recorded_at || item.recordedAt
    }))
    metricTrendSeries.value = (metricTrend?.series || []).filter((series) => series.points?.length)
  } catch (error) {
    showToast(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import '@/styles/admin-page.css';

.admin-detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
  gap: 16px;
}

.admin-detail-main {
  padding: 20px;
}

.admin-detail-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.admin-score {
  font-size: 14px;
  font-weight: 600;
}

.admin-detail-side {
  display: grid;
  gap: 16px;
  align-content: start;
}

.side-card {
  padding: 16px;
}

.side-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.side-card__head h3,
.side-card h3 {
  margin: 0;
  font-size: 15px;
}

.side-card__head span {
  color: #64748B;
  font-size: 12px;
}

.similar-case {
  padding: 12px 0;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}

.similar-case:first-of-type {
  border-top: none;
  padding-top: 0;
}

.similar-case__top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.similar-case__top span {
  color: #0369A1;
  font-size: 12px;
  font-weight: 700;
}

.similar-case p,
.similar-case small {
  margin: 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.similar-case__summary {
  margin-top: 6px !important;
}

.pre-line {
  white-space: pre-line;
}

@media (max-width: 1080px) {
  .admin-detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
