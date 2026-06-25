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
          <span class="risk-pill summary-card__risk" :class="riskMeta.className">{{ riskMeta.label }}</span>
          <div class="summary-card__title">
            <h2>{{ currentPlan.title }}</h2>
            <p>{{ currentPlan.summary }}</p>
          </div>
          <div class="tag-row">
            <span>{{ currentPlan.goal }}</span>
            <span>{{ dayCards.length }} 天安排</span>
            <span>已生成</span>
          </div>
          <div class="summary-card__footer">
            <span class="updated-at">
              <Clock />
              {{ formatPlanTime(currentPlan.update_time || currentPlan.create_time) }} 更新
            </span>
            <button type="button" class="regenerate-button" :disabled="lifePlanStore.generating" @click="regeneratePlan">
              <RefreshCw :class="{ spin: lifePlanStore.generating }" />
              {{ lifePlanStore.generating ? '正在生成方案...' : '重新生成方案' }}
            </button>
          </div>
        </section>

        <section v-if="currentPlan && lifePlanStore.generating" class="generating-notice">
          <LoaderCircle class="spin" />
          <span>正在生成新方案，完成后会刷新当前内容</span>
        </section>

        <section v-if="!currentPlan" class="empty-card">
          <span class="empty-card__icon">
            <FileText />
          </span>
          <h2>暂未生成方案</h2>
          <p>完善健康档案、健康指标和风险评估后，可以生成个性化控糖生活方案。</p>
          <button type="button" class="regenerate-button" :disabled="lifePlanStore.generating" @click="regeneratePlan">
            <LoaderCircle v-if="lifePlanStore.generating" class="spin" />
            {{ lifePlanStore.generating ? '正在生成方案...' : '立即生成' }}
          </button>
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
              <h2>第 {{ selectedDayIndex + 1 }} 天安排</h2>
              <p>点击数字切换一周内每天的饮食、运动和控糖提醒</p>
            </div>
          </div>

          <div class="day-switch-row" aria-label="选择一周安排日期">
            <button
              v-for="day in daySwitchButtons"
              :key="day.index"
              type="button"
              class="day-switch-button"
              :class="{ 'is-active': selectedDayIndex === day.index }"
              :style="{ '--day-color': day.color, '--day-bg': day.bg }"
              @click="selectDay(day.index)"
            >
              {{ day.label }}
            </button>
          </div>

          <section class="structured-day-list">
            <article v-for="day in visibleDays" :key="day.title" class="structured-day">
              <header class="structured-day__header">
                <span>{{ day.title }}</span>
                <small>{{ day.dietCards.length + day.exerciseCards.length }} 项建议</small>
              </header>

              <PlanSection
                type="diet"
                title="饮食管理"
                subtitle="早餐、午餐、晚餐和加餐建议"
                :items="decorateDietCards(day)"
                @select="selectCard(day, $event, '饮食管理')"
              />

              <PlanSection
                type="exercise"
                title="运动管理"
                subtitle="轻运动、有氧、抗阻和注意事项"
                :items="decorateExerciseCards(day)"
                @select="selectCard(day, $event, '运动管理')"
              />

              <section class="reminder-card reminder-card--day">
                <h2>作息/控糖提醒</h2>
                <ul>
                  <li v-for="tip in day.reminders" :key="`${day.title}-${tip}`">{{ tip }}</li>
                </ul>
              </section>
            </article>
          </section>

          <section v-if="selectedCard" class="selected-card-detail">
            <div>
              <span>{{ selectedCard.dayTitle }} · {{ selectedCard.group }}</span>
              <h2>{{ selectedCard.title }}</h2>
              <p>{{ selectedCard.content }}</p>
            </div>
            <button type="button" @click="selectedCard = null">收起详情</button>
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
      :initial-value="lifePlanStore.currentGenerateOptions"
      @close="showGenerateDialog = false"
      @submit="submitGenerate"
    />
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  Activity,
  Apple,
  ArrowLeft,
  CheckCircle2,
  ChevronRight,
  CircleAlert,
  Clock,
  Dumbbell,
  FileText,
  Footprints,
  History,
  LoaderCircle,
  Moon,
  RefreshCw,
  Sun,
  Utensils
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useLifePlanStore } from '@/stores/lifePlan'
import { formatPlanTime, getRiskMeta, normalizePlan } from '../utils'
import PlanGenerateDialog from '../components/PlanGenerateDialog.vue'
import '../styles/life-plan.css'

const cardMeta = {
  breakfast: { icon: Sun, iconBg: '#FEF3E2', iconColor: '#E8A840' },
  lunch: { icon: Utensils, iconBg: '#E5F6EE', iconColor: '#5BBF8A' },
  dinner: { icon: Moon, iconBg: '#E8EEF9', iconColor: '#7A9BD4' },
  snack: { icon: Apple, iconBg: '#F3EAF8', iconColor: '#B07CD4' },
  light: { icon: Footprints, iconBg: '#FEF3E2', iconColor: '#E8A840' },
  aerobic: { icon: Dumbbell, iconBg: '#E4F3FB', iconColor: '#4FAAC4' },
  resistance: { icon: Activity, iconBg: '#E5F6EE', iconColor: '#5BBF8A' },
  notice: { icon: CheckCircle2, iconBg: '#FEF8EC', iconColor: '#B8862A' }
}

const PlanSection = defineComponent({
  name: 'PlanSection',
  props: {
    type: { type: String, required: true },
    title: { type: String, required: true },
    subtitle: { type: String, required: true },
    items: { type: Array, required: true }
  },
  emits: ['select'],
  setup(props, { emit }) {
    return () => h('section', { class: 'plan-section' }, [
      h('div', { class: ['section-header', `section-header--${props.type}`] }, [
        h('div', { class: 'section-header__left' }, [
          h('span', { class: 'section-header__icon' }, [h(props.type === 'diet' ? Utensils : Dumbbell)]),
          h('div', [h('h2', props.title), h('p', props.subtitle)])
        ]),
        h('span', { class: 'checkin-pill' }, [h(CheckCircle2), '今日执行'])
      ]),
      h('div', { class: 'plan-card-list' }, props.items.map((item) => h('button', {
        type: 'button',
        class: 'plan-item-card',
        onClick: () => emit('select', item)
      }, [
        h('span', {
          class: 'plan-item-card__icon',
          style: { background: item.iconBg, color: item.iconColor }
        }, [h(item.icon)]),
        h('span', { class: 'plan-item-card__content' }, [
          h('strong', item.title),
          h('span', item.content)
        ]),
        h('span', { class: 'plan-item-card__side' }, [h('small', item.time), h(ChevronRight)])
      ])))
    ])
  }
})

const router = useRouter()
const authStore = useAuthStore()
const lifePlanStore = useLifePlanStore()
const showGenerateDialog = ref(false)
const selectedDayIndex = ref(0)
const selectedCard = ref(null)

const currentPlan = computed(() => lifePlanStore.currentPlan ? normalizePlan(lifePlanStore.currentPlan) : null)
const riskMeta = computed(() => getRiskMeta(currentPlan.value))
const dayCards = computed(() => currentPlan.value?.dailySchedule?.length ? currentPlan.value.dailySchedule : [])
const visibleDays = computed(() => {
  const selected = dayCards.value[selectedDayIndex.value] || dayCards.value[0]
  return selected ? [selected] : []
})
const daySwitchButtons = computed(() => {
  const palette = [
    ['#4A8A6A', '#DFF5E7'],
    ['#3D8BA6', '#E4F3FB'],
    ['#B8862A', '#FEF3E2'],
    ['#7A6AC9', '#EEEAFB'],
    ['#D06B7A', '#FCE8EC'],
    ['#4E8F88', '#E0F3EF'],
    ['#7A9BD4', '#E8EEF9']
  ]
  return Array.from({ length: 7 }, (_, index) => ({
    index,
    label: String(index + 1),
    color: palette[index][0],
    bg: palette[index][1]
  }))
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

watch(dayCards, (days) => {
  if (!days.length || selectedDayIndex.value >= days.length) {
    selectedDayIndex.value = 0
  }
})

async function loadCurrentPlan() {
  try {
    await lifePlanStore.fetchCurrentPlan()
  } catch {
    // Store already exposes a user-facing error.
  }
}

function decorateDietCards(day) {
  return day.dietCards.map((item) => ({ ...item, ...(cardMeta[item.key] || cardMeta.lunch) }))
}

function decorateExerciseCards(day) {
  return day.exerciseCards.map((item) => ({ ...item, ...(cardMeta[item.key] || cardMeta.aerobic) }))
}

function selectDay(index) {
  selectedDayIndex.value = Math.min(index, Math.max(dayCards.value.length - 1, 0))
  selectedCard.value = null
}

function selectCard(day, item, group) {
  selectedCard.value = {
    dayTitle: day.title,
    group,
    title: item.title,
    content: item.content
  }
}

function ensureLoggedIn() {
  authStore.restoreSession('patient')
  if (!authStore.isLoggedIn) {
    showToast('请先登录后再生成生活方案')
    router.push('/login')
    return false
  }
  return true
}

function openGenerateDialog() {
  if (!ensureLoggedIn()) return
  showGenerateDialog.value = true
}

async function regeneratePlan() {
  if (!ensureLoggedIn()) return

  try {
    selectedCard.value = null
    await lifePlanStore.generateLifePlan(lifePlanStore.currentGenerateOptions)
    selectedDayIndex.value = 0
    showToast('生活方案已更新')
  } catch {
    showToast(lifePlanStore.generateError || '方案生成失败，请稍后重试')
  }
}

async function submitGenerate(payload) {
  try {
    selectedCard.value = null
    await lifePlanStore.generateLifePlan(payload)
    selectedDayIndex.value = 0
    showGenerateDialog.value = false
    showToast('生活方案已更新')
  } catch {
    // Error is displayed in the dialog.
  }
}
</script>
