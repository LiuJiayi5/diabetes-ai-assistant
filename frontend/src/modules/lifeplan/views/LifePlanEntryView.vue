<template>
  <div class="life-plan-page">
    <header class="life-plan-topbar">
      <button type="button" class="life-back-button" @click="router.push('/app/home')">
        <ArrowLeft />
      </button>
      <h1>生活方案</h1>
      <button type="button" class="adjust-button" @click="openGenerateDialog">调整方案</button>
    </header>

    <main class="life-plan-scroll mobile-scroll">
      <section v-if="lifePlanStore.loading" class="state-card">
        <LoaderCircle class="spin" />
        <p>正在加载生活方案...</p>
      </section>

      <section v-else-if="lifePlanStore.error" class="state-card state-card--error">
        <CircleAlert />
        <p>{{ lifePlanStore.error }}</p>
        <button type="button" @click="loadCurrentPlan">重新加载</button>
      </section>

      <template v-else>
        <section v-if="currentPlan" class="summary-card">
          <div class="summary-card__glow summary-card__glow--green" />
          <div class="summary-card__glow summary-card__glow--blue" />
          <div class="summary-card__head">
            <div>
              <h2>{{ currentPlan.title }}</h2>
              <p>{{ currentPlan.summary }}</p>
            </div>
            <span class="risk-pill" :class="riskMeta.className">{{ riskMeta.label }}</span>
          </div>
          <div class="tag-row">
            <span>{{ currentPlan.goal }}</span>
            <span>{{ dayCards.length || 1 }} 天安排</span>
            <span>已生成</span>
          </div>
          <div class="summary-card__footer">
            <span class="updated-at">
              <Clock />
              {{ formatPlanTime(currentPlan.update_time || currentPlan.create_time) }} 更新
            </span>
            <button type="button" class="regenerate-button" :disabled="lifePlanStore.generating" @click="openGenerateDialog">
              <RefreshCw :class="{ spin: lifePlanStore.generating }" />
              {{ lifePlanStore.generating ? '生成中' : '重新生成方案' }}
            </button>
          </div>
        </section>

        <section v-else class="empty-card">
          <span class="empty-card__icon">
            <FileText />
          </span>
          <h2>暂未生成方案</h2>
          <p>完善健康档案、健康指标和风险评估后，可以生成个性化控糖生活方案。</p>
          <button type="button" class="regenerate-button" @click="openGenerateDialog">立即生成</button>
        </section>

        <button type="button" class="history-entry" @click="router.push('/app/life-plan/history')">
          <span class="history-entry__icon">
            <History />
          </span>
          <span>历史方案</span>
          <ChevronRight />
        </button>

        <template v-if="currentPlan">
          <div class="week-title-row">
            <div>
              <h2>{{ expandedWeek ? '一周安排' : '第 1 天重点' }}</h2>
              <p>{{ expandedWeek ? '按天查看饮食、运动和作息提醒' : '首屏先聚焦今天最重要的安排' }}</p>
            </div>
            <button type="button" class="week-toggle-button" @click="expandedWeek = !expandedWeek">
              {{ expandedWeek ? '收起一周安排' : '展开一周安排' }}
            </button>
          </div>

          <section class="day-plan-list">
            <article v-for="day in visibleDays" :key="day.title" class="day-plan-card">
              <header>
                <span>{{ day.title }}</span>
              </header>
              <div class="day-plan-block day-plan-block--diet">
                <span><Utensils /></span>
                <div>
                  <strong>饮食建议</strong>
                  <p>{{ day.diet }}</p>
                </div>
              </div>
              <div class="day-plan-block day-plan-block--exercise">
                <span><Dumbbell /></span>
                <div>
                  <strong>运动建议</strong>
                  <p>{{ day.exercise }}</p>
                </div>
              </div>
              <div class="day-plan-block day-plan-block--reminder">
                <span><Clock /></span>
                <div>
                  <strong>作息/控糖提醒</strong>
                  <p>{{ day.reminder }}</p>
                </div>
              </div>
            </article>
          </section>

          <section class="reminder-card">
            <h2>健康提示</h2>
            <ul>
              <li v-for="tip in reminderTips" :key="tip">{{ tip }}</li>
            </ul>
          </section>
        </template>
      </template>
    </main>

    <PlanGenerateDialog
      :show="showGenerateDialog"
      :generating="lifePlanStore.generating"
      :error="lifePlanStore.generateError"
      @close="showGenerateDialog = false"
      @submit="submitGenerate"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  ArrowLeft,
  ChevronRight,
  CircleAlert,
  Clock,
  Dumbbell,
  FileText,
  History,
  LoaderCircle,
  RefreshCw,
  Utensils
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useLifePlanStore } from '@/stores/lifePlan'
import { formatPlanTime, getRiskMeta, normalizePlan, toText } from '../utils'
import PlanGenerateDialog from '../components/PlanGenerateDialog.vue'
import '../styles/life-plan.css'

const router = useRouter()
const authStore = useAuthStore()
const lifePlanStore = useLifePlanStore()
const showGenerateDialog = ref(false)
const expandedWeek = ref(false)

const currentPlan = computed(() => lifePlanStore.currentPlan ? normalizePlan(lifePlanStore.currentPlan) : null)
const riskMeta = computed(() => getRiskMeta(currentPlan.value))
const dayCards = computed(() => {
  const days = currentPlan.value?.dailySchedule || []
  return days.length ? days : [fallbackDay.value]
})
const visibleDays = computed(() => expandedWeek.value ? dayCards.value.slice(0, 7) : dayCards.value.slice(0, 1))
const fallbackDay = computed(() => {
  const diet = currentPlan.value?.dietPlan || {}
  const exercise = currentPlan.value?.exercisePlan || {}
  return {
    title: '第 1 天',
    diet: [
      toText(diet.breakfast, ''),
      toText(diet.lunch, ''),
      toText(diet.dinner, '')
    ].filter(Boolean).join('；') || '保持三餐规律，主食定量，搭配足量蔬菜和优质蛋白。',
    exercise: toText(exercise.exercise_type || exercise.frequency || exercise.duration, '餐后进行低到中等强度活动，循序渐进完成当天运动目标。'),
    reminder: toText(currentPlan.value?.workRestPlan, '按时监测血糖，避免久坐，保持规律睡眠。')
  }
})

const reminderTips = computed(() => {
  const tips = currentPlan.value?.healthTips || []
  return tips.length ? tips : [
    '保持规律睡眠，尽量在固定时间入睡和起床。',
    '餐后 2 小时可记录血糖变化，便于后续调整方案。',
    currentPlan.value?.medicalWarning || 'AI 建议仅供参考，不能替代线下诊疗。'
  ]
})

onMounted(loadCurrentPlan)

async function loadCurrentPlan() {
  try {
    await lifePlanStore.fetchCurrentPlan()
  } catch {
    // Store already exposes a user-facing error.
  }
}

function openGenerateDialog() {
  if (!authStore.isLoggedIn) {
    showToast('请先登录后再生成生活方案')
    router.push('/login')
    return
  }
  showGenerateDialog.value = true
}

async function submitGenerate(payload) {
  try {
    await lifePlanStore.generateLifePlan(payload)
    await lifePlanStore.fetchCurrentPlan()
    expandedWeek.value = false
    showGenerateDialog.value = false
    showToast('生活方案已更新')
  } catch {
    // Error is displayed in the dialog.
  }
}
</script>
