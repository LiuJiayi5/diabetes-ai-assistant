<template>
  <CheckinPageShell title="行为分析" subtitle="基于打卡记录生成生活反馈" @refresh="loadLatest">
    <section class="generate-card">
      <div>
        <p class="eyebrow">AI 行为分析</p>
        <h2>复盘饮食与运动习惯</h2>
        <p>结合近期打卡记录，帮助你理解生活习惯变化。</p>
      </div>
      <button class="generate-button" type="button" :disabled="generating" @click="generateAnalysis">
        <LoaderCircle v-if="generating" class="spin" :size="16" />
        <Sparkles v-else :size="17" />
        <span>{{ generating ? '生成中' : '生成分析' }}</span>
      </button>
    </section>

    <section v-if="loading" class="state-card">
      <LoaderCircle class="spin" :size="24" />
      <p>正在读取最新分析</p>
    </section>

    <section v-else-if="!analysis?.analysis_id" class="state-card">
      <FileQuestion :size="34" />
      <h3>暂无分析结果</h3>
      <p>点击生成分析后，系统会结合最近打卡记录给出生活状态评价。</p>
    </section>

    <template v-else>
      <section v-if="analysis.call_status === 'failed'" class="source-card source-card--error">
        <AlertCircle :size="18" />
        <p>{{ analysis.error_message || analysis.summary || 'Dify 打卡分析工作流调用失败，请检查工作流配置或网络连接。' }}</p>
      </section>
      <section v-else-if="isLocalMock" class="source-card source-card--mock">
        <Info :size="18" />
        <p>当前结果用于辅助理解近期打卡趋势，请结合实际身体感受判断。</p>
      </section>
      <section v-else class="source-card">
        <Sparkles :size="18" />
        <p>当前结果由 AI 行为分析服务结合打卡记录生成。</p>
      </section>

      <section class="score-card">
        <div>
          <p class="eyebrow">{{ rangeText }}</p>
          <h2>{{ displayScore }}</h2>
          <p>习惯评分</p>
        </div>
        <div class="score-meta">
          <span class="call-status" :class="`call-status--${analysis.call_status}`">
            {{ callStatusText(analysis.call_status) }}
          </span>
          <span>{{ completionRateText }} 完成率</span>
        </div>
      </section>

      <section class="stats-card">
        <article>
          <strong>{{ analysis.diet_completion_count || 0 }}</strong>
          <span>饮食完成</span>
        </article>
        <article>
          <strong>{{ analysis.exercise_completion_count || 0 }}</strong>
          <span>运动完成</span>
        </article>
        <article>
          <strong>{{ completedTaskCount }}</strong>
          <span>完成任务</span>
        </article>
      </section>

      <section class="summary-card">
        <div class="section-title">
          <Activity :size="18" />
          <h3>生活状态评价</h3>
        </div>
        <p>{{ analysis.life_evaluation || analysis.summary || '暂无评价内容' }}</p>
      </section>

      <section class="summary-grid">
        <article class="mini-card">
          <Salad :size="19" />
          <h3>饮食总结</h3>
          <p>{{ analysis.diet_summary || '暂无饮食总结' }}</p>
        </article>
        <article class="mini-card mini-card--blue">
          <Dumbbell :size="19" />
          <h3>运动总结</h3>
          <p>{{ analysis.exercise_summary || '暂无运动总结' }}</p>
        </article>
      </section>

      <section class="list-card">
        <div class="section-title">
          <AlertCircle :size="18" />
          <h3>主要问题</h3>
        </div>
        <ul>
          <li v-for="item in listOf(analysis.main_problems, '暂未发现明显问题')" :key="item">{{ item }}</li>
        </ul>
      </section>

      <section class="list-card">
        <div class="section-title">
          <Lightbulb :size="18" />
          <h3>改进建议</h3>
        </div>
        <ul>
          <li v-for="item in listOf(analysis.improvement_suggestions, '继续保持规律记录')" :key="item">{{ item }}</li>
        </ul>
      </section>

      <section class="focus-card">
        <Target :size="20" />
        <div>
          <h3>下一步重点</h3>
          <p>{{ analysis.next_focus || '保持每日饮食与运动打卡，观察完成率变化。' }}</p>
        </div>
      </section>

      <button class="consult-button" type="button" @click="askAiDoctor">
        <MessageCircle :size="17" />
        <span>向 AI 医生咨询这份分析</span>
      </button>

      <p v-if="analysis.error_message" class="error-note">{{ analysis.error_message }}</p>
    </template>

    <p class="medical-note">AI 建议仅供参考，不能替代线下诊疗。</p>
  </CheckinPageShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import {
  Activity,
  AlertCircle,
  Dumbbell,
  FileQuestion,
  Info,
  Lightbulb,
  LoaderCircle,
  MessageCircle,
  Salad,
  Sparkles,
  Target
} from 'lucide-vue-next'
import { generateCheckinAnalysis, getLatestCheckinAnalysis } from '@/api/checkin'
import CheckinPageShell from '../components/CheckinPageShell.vue'

const loading = ref(false)
const generating = ref(false)
const analysis = ref(null)
const router = useRouter()

const rangeText = computed(() => {
  if (!analysis.value?.start_date || !analysis.value?.end_date) return '最近打卡记录'
  return `${analysis.value.start_date} 至 ${analysis.value.end_date}`
})

const completionRateText = computed(() => {
  const raw = Number(analysis.value?.completion_rate || 0)
  const percent = raw <= 1 ? raw * 100 : raw
  return `${Math.round(percent)}%`
})

const completedTaskCount = computed(() => {
  return Number(analysis.value?.diet_completion_count || 0) + Number(analysis.value?.exercise_completion_count || 0)
})

const displayScore = computed(() => {
  if (analysis.value?.habit_score !== null && analysis.value?.habit_score !== undefined) {
    return analysis.value.habit_score
  }
  const raw = Number(analysis.value?.completion_rate || 0)
  return raw <= 1 ? Math.round(raw * 100) : Math.round(raw)
})

const isLocalMock = computed(() => {
  const text = [
    analysis.value?.summary,
    analysis.value?.life_evaluation,
    analysis.value?.diet_summary,
    analysis.value?.exercise_summary,
    analysis.value?.input_summary,
    ...(Array.isArray(analysis.value?.main_problems) ? analysis.value.main_problems : [])
  ].filter(Boolean).join(' ')
  return /mock|模拟|Dify API Key|占位|placeholder/i.test(text)
})

function unwrap(response) {
  return response?.data ?? response
}

async function loadLatest() {
  loading.value = true
  try {
    analysis.value = unwrap(await getLatestCheckinAnalysis())
  } catch (error) {
    const status = error?.response?.status
    if (status !== 404) {
      showFailToast(error?.response?.data?.message || '最新分析读取失败')
    }
    analysis.value = null
  } finally {
    loading.value = false
  }
}

async function generateAnalysis() {
  generating.value = true
  try {
    analysis.value = unwrap(await generateCheckinAnalysis({ period: 7 }))
    showSuccessToast('分析已生成')
  } catch (error) {
    showFailToast(error?.response?.data?.message || '分析生成失败')
  } finally {
    generating.value = false
  }
}

function askAiDoctor() {
  router.push({
    path: '/app/ai-chat/chat',
    query: {
      q: '请帮我解释最近一周打卡分析，并告诉我饮食和运动应该怎么调整。'
    }
  })
}

function listOf(value, fallback) {
  if (Array.isArray(value) && value.length) return value
  if (typeof value === 'string' && value) return [value]
  return [fallback]
}

function callStatusText(status) {
  const map = {
    success: '已生成',
    failed: '生成失败',
    pending: '处理中'
  }
  return map[status] || '已保存'
}

onMounted(loadLatest)
</script>

<style scoped>
.generate-card,
.score-card,
.stats-card,
.summary-card,
.mini-card,
.list-card,
.focus-card,
.state-card,
.source-card {
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.generate-card {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  padding: 18px;
  background: linear-gradient(145deg, #EDF8F4, #EAF5FA);
}

.eyebrow {
  margin: 0 0 5px;
  color: var(--figma-text-muted);
  font-size: 11px;
  font-weight: 600;
}

.generate-card h2 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.4;
}

.generate-card p:last-child {
  margin: 5px 0 0;
  color: var(--figma-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.generate-button {
  min-height: 42px;
  min-width: 102px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 13px;
  font-weight: 600;
  box-shadow: var(--figma-shadow-button);
}

.consult-button {
  width: 100%;
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  margin-top: 12px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 600;
  box-shadow: var(--figma-shadow-button);
}

.generate-button:disabled {
  opacity: 0.72;
}

.source-card {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-bottom: 12px;
  padding: 12px 14px;
  color: #4FB783;
  background: linear-gradient(145deg, #EDF8F4, #EAF5FA);
}

.source-card--mock {
  color: #B8862A;
  background: #FFF8E8;
}

.source-card--error {
  color: #E87878;
  background: rgba(239, 143, 143, 0.12);
}

.source-card p {
  margin: 0;
  color: var(--figma-text-secondary);
  font-size: 11px;
  line-height: 1.6;
}

.score-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 18px;
  border-radius: var(--figma-radius-card-lg);
  background: linear-gradient(135deg, #AEE8C7 0%, #BDEDD9 45%, #BFE9F2 100%);
}

.score-card h2 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
}

.score-card p:last-child {
  margin: 7px 0 0;
  color: rgba(36, 50, 61, 0.68);
  font-size: 12px;
}

.score-meta {
  display: grid;
  gap: 7px;
  justify-items: end;
  color: rgba(36, 50, 61, 0.68);
  font-size: 11px;
  font-weight: 600;
}

.call-status {
  padding: 5px 11px;
  border-radius: var(--figma-radius-pill);
  background: rgba(255, 255, 255, 0.56);
  color: #4A8A6A;
  font-size: 11px;
  font-weight: 700;
}

.call-status--failed {
  color: #E87878;
}

.stats-card {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
  padding: 12px;
}

.stats-card article {
  min-height: 62px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: #F7FCF9;
}

.stats-card strong {
  color: var(--figma-text-strong);
  font-size: 18px;
  line-height: 1.2;
}

.stats-card span {
  margin-top: 3px;
  color: var(--figma-text-muted);
  font-size: 10px;
}

.summary-card,
.list-card,
.focus-card {
  margin-top: 12px;
  padding: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4FB783;
}

.section-title h3,
.mini-card h3,
.focus-card h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 14px;
  font-weight: 600;
}

.summary-card p,
.mini-card p,
.focus-card p,
.list-card li {
  color: var(--figma-text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.summary-card p,
.mini-card p,
.focus-card p {
  margin: 10px 0 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 12px;
}

.mini-card {
  padding: 15px;
  color: #4FB783;
}

.mini-card--blue {
  color: #4FAAC4;
}

.list-card ul {
  display: grid;
  gap: 8px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.list-card li {
  position: relative;
  padding-left: 13px;
}

.list-card li::before {
  content: "";
  position: absolute;
  top: 8px;
  left: 0;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #8ED4AF;
}

.focus-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: linear-gradient(145deg, #EDF8F4, #EAF5FA);
  color: #4FB783;
}

.state-card {
  min-height: 230px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 26px 22px;
  text-align: center;
  color: var(--figma-text-muted);
}

.state-card h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 16px;
}

.state-card p,
.medical-note,
.error-note {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
}

.medical-note,
.error-note {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 14px;
  text-align: center;
}

.medical-note {
  background: rgba(255, 255, 255, 0.65);
  color: rgba(107, 114, 128, 0.78);
  font-size: 10px;
}

.error-note {
  background: rgba(239, 143, 143, 0.12);
  color: #E87878;
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
