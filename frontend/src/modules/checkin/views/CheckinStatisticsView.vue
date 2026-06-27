<template>
  <CheckinPageShell title="打卡统计" subtitle="查看阶段完成率与执行稳定性" @refresh="loadStatistics">
    <section class="period-card">
      <button
        v-for="item in periods"
        :key="item.value"
        type="button"
        :class="{ active: period === item.value }"
        @click="changePeriod(item.value)"
      >
        {{ item.label }}
      </button>
    </section>

    <section v-if="loading" class="state-card">
      <LoaderCircle class="spin" :size="24" />
      <p>正在统计打卡数据</p>
    </section>

    <template v-else>
      <section class="summary-card">
        <div>
          <p class="eyebrow">{{ rangeText }}</p>
          <h2>{{ ratePercent }}%</h2>
          <p>综合完成率</p>
        </div>
        <div class="progress-ring" :style="{ '--rate': ratePercent }">
          <span>{{ completedTaskCount }}/{{ totalTaskCount }}</span>
        </div>
      </section>

      <section class="metric-grid">
        <article class="metric-card">
          <Salad :size="20" />
          <span>饮食完成</span>
          <strong>{{ statistics.diet_completion_count || 0 }} 次</strong>
        </article>
        <article class="metric-card metric-card--blue">
          <Dumbbell :size="20" />
          <span>运动完成</span>
          <strong>{{ statistics.exercise_completion_count || 0 }} 次</strong>
        </article>
        <article class="metric-card metric-card--orange">
          <CalendarDays :size="20" />
          <span>统计天数</span>
          <strong>{{ statistics.total_days || 0 }} 天</strong>
        </article>
        <article class="metric-card metric-card--purple">
          <CheckCircle2 :size="20" />
          <span>完成任务</span>
          <strong>{{ completedTaskCount }} 项</strong>
        </article>
      </section>

      <section class="insight-card">
        <div class="section-title">
          <TrendingUp :size="18" />
          <h3>执行反馈</h3>
        </div>
        <p>{{ insightText }}</p>
        <div class="bar-track">
          <span :style="{ width: `${ratePercent}%` }"></span>
        </div>
      </section>

      <p class="medical-note">AI 建议仅供参考，不能替代线下诊疗。</p>
    </template>
  </CheckinPageShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { showFailToast } from 'vant'
import { CalendarDays, CheckCircle2, Dumbbell, LoaderCircle, Salad, TrendingUp } from 'lucide-vue-next'
import { getCheckinStatistics } from '@/api/checkin'
import CheckinPageShell from '../components/CheckinPageShell.vue'

const loading = ref(false)
const period = ref(7)
const statistics = ref({})

const periods = [
  { label: '近7天', value: 7 },
  { label: '近14天', value: 14 },
  { label: '近30天', value: 30 }
]

const ratePercent = computed(() => {
  const raw = Number(statistics.value.completion_rate || 0)
  return raw <= 1 ? Math.round(raw * 100) : Math.round(raw)
})

const totalTaskCount = computed(() => Number(statistics.value.total_task_count || 0))
const completedTaskCount = computed(() => Number(statistics.value.completed_task_count || 0))

const rangeText = computed(() => {
  if (!statistics.value.start_date || !statistics.value.end_date) return `近 ${period.value} 天`
  return `${statistics.value.start_date} 至 ${statistics.value.end_date}`
})

const insightText = computed(() => {
  if (ratePercent.value >= 80) return '整体执行非常稳定，可以继续保持当前饮食和运动节奏。'
  if (ratePercent.value >= 50) return '已经形成一定记录习惯，建议优先补齐漏掉的运动或饮食任务。'
  if (totalTaskCount.value === 0) return '当前统计周期暂无可分析任务，先完成今日打卡后再查看趋势。'
  return '近期完成率偏低，可以从每天固定一个容易完成的任务开始。'
})

function unwrap(response) {
  return response?.data ?? response
}

async function loadStatistics() {
  loading.value = true
  try {
    statistics.value = unwrap(await getCheckinStatistics({ period: period.value })) || {}
  } catch (error) {
    showFailToast(error?.response?.data?.message || '统计数据读取失败')
  } finally {
    loading.value = false
  }
}

function changePeriod(value) {
  period.value = value
  loadStatistics()
}

onMounted(loadStatistics)
</script>

<style scoped>
.period-card,
.summary-card,
.metric-card,
.insight-card,
.state-card {
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.period-card {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 14px;
  padding: 8px;
}

.period-card button {
  min-height: 36px;
  border-radius: var(--figma-radius-pill);
  background: transparent;
  color: var(--figma-text-muted);
  font-size: 12px;
  font-weight: 600;
}

.period-card button.active {
  background: var(--figma-secondary-green);
  color: #4FB783;
}

.summary-card {
  min-height: 150px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 20px;
  border-radius: var(--figma-radius-card-lg);
  background: linear-gradient(145deg, #FFFFFF 0%, #EDF8F4 58%, #EAF5FA 100%);
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--figma-text-muted);
  font-size: 11px;
}

.summary-card h2 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 32px;
  font-weight: 700;
  line-height: 1.1;
}

.summary-card p:last-child {
  margin: 6px 0 0;
  color: var(--figma-text-secondary);
  font-size: 12px;
}

.progress-ring {
  width: 96px;
  height: 96px;
  flex: 0 0 96px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: conic-gradient(#6FCF97 calc(var(--rate) * 1%), rgba(174, 232, 199, 0.22) 0);
  box-shadow: inset 0 0 0 12px #FFFFFF;
}

.progress-ring span {
  color: #4A8A6A;
  font-size: 13px;
  font-weight: 700;
}

.metric-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 12px;
}

.metric-card {
  min-height: 104px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 7px;
  padding: 16px;
  color: #4FB783;
}

.metric-card--blue {
  color: #4FAAC4;
}

.metric-card--orange {
  color: #B8862A;
}

.metric-card--purple {
  color: #B07CD4;
}

.metric-card span {
  color: var(--figma-text-muted);
  font-size: 11px;
}

.metric-card strong {
  color: var(--figma-text-strong);
  font-size: 18px;
  font-weight: 700;
}

.insight-card {
  margin-top: 12px;
  padding: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4FB783;
}

.section-title h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 15px;
  font-weight: 600;
}

.insight-card p {
  margin: 10px 0 12px;
  color: var(--figma-text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.bar-track {
  height: 8px;
  border-radius: var(--figma-radius-pill);
  background: rgba(174, 232, 199, 0.28);
  overflow: hidden;
}

.bar-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--figma-green-button);
}

.state-card {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 26px 22px;
  text-align: center;
  color: var(--figma-text-muted);
}

.state-card p,
.medical-note {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
}

.medical-note {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.65);
  color: rgba(107, 114, 128, 0.78);
  font-size: 10px;
  text-align: center;
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
