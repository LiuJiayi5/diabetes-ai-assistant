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
            <button type="button" class="regenerate-button detail-button" @click="openCurrentPlanDetail">
              <FileSearch />
              查看方案详情
            </button>
          </div>
        </section>

        <section v-if="currentPlan && lifePlanStore.generating" class="generating-notice">
          <LoaderCircle class="spin" />
          <span>正在生成新方案，完成后会刷新当前内容</span>
        </section>

        <section v-if="currentPlan && visibleLatestReview" class="intervention-card" :class="`intervention-card--${reviewMeta.tone}`">
          <button
            type="button"
            class="intervention-card__close"
            aria-label="关闭自动复盘提示"
            @click="dismissReviewCard"
          >
            <X />
          </button>
          <div class="intervention-card__head">
            <span class="intervention-card__icon">
              <Sparkles />
            </span>
            <div>
              <small>系统自动复盘</small>
              <h2>{{ reviewMeta.title }}</h2>
            </div>
          </div>
          <p class="intervention-card__notice">{{ visibleLatestReview.patient_notice || reviewMeta.notice }}</p>
          <p class="intervention-card__explain">{{ visibleLatestReview.explanation }}</p>
          <div v-if="reviewTags.length" class="intervention-tags">
            <span v-for="tag in reviewTags" :key="tag">{{ formatReviewTag(tag) }}</span>
          </div>
          <div v-if="changedItems.length" class="intervention-card__section">
            <span>已调整</span>
            <p>{{ changedItems.map(formatReviewTag).join('、') }}</p>
          </div>
          <div v-if="visibleLatestReview.safety_warning" class="intervention-card__warning">
            <ShieldAlert />
            <span>{{ visibleLatestReview.safety_warning }}</span>
          </div>
        </section>

        <section v-if="!currentPlan" class="empty-card">
          <span class="empty-card__icon">
            <FileText />
          </span>
          <h2>暂未生成方案</h2>
          <p>完善健康档案、健康指标和风险评估后，可以生成个性化控糖生活方案。</p>
          <button type="button" class="regenerate-button" :disabled="lifePlanStore.generating" @click="openGenerateDialog">
            <LoaderCircle v-if="lifePlanStore.generating" class="spin" />
            {{ lifePlanStore.generating ? '正在生成方案...' : '填写偏好并生成' }}
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
              :class="{
                'is-active': selectedDayIndex === day.index,
                'is-past': day.relation === 'past',
                'is-today': day.relation === 'today',
                'is-future': day.relation === 'future'
              }"
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
                :execution="sectionExecution('diet')"
                :active-key="selectedCard?.selectKey"
                @action="handleSectionAction('diet')"
                @select="selectCard(day, $event, '饮食管理')"
              />

              <PlanSection
                type="exercise"
                title="运动管理"
                subtitle="轻运动、有氧、抗阻和注意事项"
                :items="decorateExerciseCards(day)"
                :execution="sectionExecution('exercise')"
                :active-key="selectedCard?.selectKey"
                @action="handleSectionAction('exercise')"
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

          <section class="reminder-card">
            <h2>健康提示</h2>
            <ul>
              <li v-for="tip in reminderTips" :key="tip">{{ tip }}</li>
            </ul>
          </section>

          <SmartRecommendationPanel v-if="recommendationReady" :scenario="readingScenario" title="今日方案配套阅读" :limit="4" />
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
  FileSearch,
  FileText,
  Footprints,
  History,
  LoaderCircle,
  Moon,
  ShieldAlert,
  Sparkles,
  Sun,
  Utensils,
  X
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useLifePlanStore } from '@/stores/lifePlan'
import { getCheckinHistory } from '@/api/checkin'
import { getLatestInterventionReview } from '@/api/interventionReview'
import { pushWithBack } from '@/utils/navigation'
import { formatPlanTime, getRiskMeta, normalizePlan } from '../utils'
import PlanGenerateDialog from '../components/PlanGenerateDialog.vue'
import SmartRecommendationPanel from '@/modules/article/components/SmartRecommendationPanel.vue'
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
    execution: {
      type: Object,
      default: () => ({ label: '待完成', status: 'future', disabled: true })
    },
    items: { type: Array, required: true },
    activeKey: { type: String, default: '' }
  },
  emits: ['select', 'action'],
  setup(props, { emit }) {
    const statusIcon = () => {
      if (props.execution?.status === 'today' || props.execution?.status === 'completed') return CheckCircle2
      if (props.execution?.status === 'missed') return CircleAlert
      return Clock
    }
    return () => h('section', { class: 'plan-section' }, [
      h('div', { class: ['section-header', `section-header--${props.type}`] }, [
        h('div', { class: 'section-header__left' }, [
          h('span', { class: 'section-header__icon' }, [h(props.type === 'diet' ? Utensils : Dumbbell)]),
          h('div', [h('h2', props.title), h('p', props.subtitle)])
        ]),
        h('button', {
          type: 'button',
          class: ['checkin-pill', `checkin-pill--${props.execution?.status || 'future'}`],
          disabled: props.execution?.disabled,
          title: props.execution?.disabled ? '尚未到达该计划日' : props.execution?.hint,
          onClick: () => emit('action')
        }, [h(statusIcon()), props.execution?.label || '待完成'])
      ]),
      h('div', { class: 'plan-card-list' }, props.items.map((item) => {
        const expanded = props.activeKey && props.activeKey === item.selectKey
        return h('div', { class: ['plan-card-item-wrap', { 'is-expanded': expanded }] }, [
          h('button', {
            type: 'button',
            class: ['plan-item-card', { 'is-active': expanded }],
            onClick: () => emit('select', item)
          }, [
            h('span', {
              class: 'plan-item-card__icon',
              style: { background: item.iconBg, color: item.iconColor }
            }, [h(item.icon)]),
            h('span', { class: 'plan-item-card__content' }, [
              h('strong', item.title),
              h('span', item.preview || '点开查看这一段内容')
            ]),
            h('span', { class: 'plan-item-card__side' }, [h('small', item.time), h(ChevronRight)])
          ]),
          expanded ? h('div', { class: 'plan-item-card__detail' }, [
            h('div', { class: 'plan-item-card__detail-head' }, [
              h('span', item.time || '今日')
            ]),
            h('p', item.content),
            h('small', props.type === 'diet' ? '按方案完成后，可到今日打卡记录饮食执行情况。' : '运动前后留意身体状态，完成后可到今日打卡记录。')
          ]) : null
        ])
      }))
    ])
  }
})

const router = useRouter()
const authStore = useAuthStore()
const lifePlanStore = useLifePlanStore()
const showGenerateDialog = ref(false)
const selectedDayIndex = ref(0)
const selectedCard = ref(null)
const checkinRecords = ref([])
const latestReview = ref(null)
const latestReviewLoaded = ref(false)
const dismissedReviewVersion = ref(0)

const currentPlan = computed(() => lifePlanStore.currentPlan ? normalizePlan(lifePlanStore.currentPlan) : null)
const riskMeta = computed(() => getRiskMeta(currentPlan.value))
const dayCards = computed(() => currentPlan.value?.dailySchedule?.length ? currentPlan.value.dailySchedule : [])
const visibleDays = computed(() => {
  const selected = dayCards.value[selectedDayIndex.value] || dayCards.value[0]
  return selected ? [selected] : []
})
const visibleLatestReview = computed(() => {
  dismissedReviewVersion.value
  const review = latestReview.value
  if (!getReviewId(review)) return null
  if (!isEffectiveAutoAdjustment(review)) return null
  return isReviewCardDismissed(review) ? null : review
})
const reviewMeta = computed(() => {
  const level = visibleLatestReview.value?.intervention_level
  if (level === 'high_risk_alert') {
    return { tone: 'alert', title: '需要优先关注安全提醒', notice: '系统已识别到需要谨慎处理的健康信号。' }
  }
  if (level === 'moderate_adjustment') {
    return { tone: 'adjust', title: '后续计划已自动优化', notice: '系统已根据近期执行情况调整后续安排。' }
  }
  if (level === 'minor_adjustment') {
    return { tone: 'light', title: '后续计划已轻微微调', notice: '系统做了轻量调整，帮助你更容易坚持。' }
  }
  return { tone: 'observe', title: '当前方案继续观察', notice: '近期执行状态暂不需要调整。' }
})
const reviewTags = computed(() => Array.isArray(visibleLatestReview.value?.main_problem_tags) ? visibleLatestReview.value.main_problem_tags.slice(0, 4) : [])
const changedItems = computed(() => Array.isArray(visibleLatestReview.value?.changed_items) ? visibleLatestReview.value.changed_items.slice(0, 4) : [])
const readingScenario = computed(() => latestReview.value ? 'intervention_review' : 'life_plan')
const recommendationReady = computed(() => latestReviewLoaded.value)
const planStartDate = computed(() => parsePlanDate(currentPlan.value?.create_time || currentPlan.value?.createTime || currentPlan.value?.update_time || currentPlan.value?.updateTime))
const selectedDayState = computed(() => daySwitchButtons.value[selectedDayIndex.value] || null)
const checkinStatusByDate = computed(() => {
  const map = new Map()
  checkinRecords.value
    .filter((record) => String(record.plan_id ?? record.planId ?? '') === String(currentPlan.value?.id ?? ''))
    .forEach((record) => {
      const date = record.checkin_date ?? record.checkinDate
      const type = record.task_type ?? record.taskType
      if (!date || !type) return
      const key = String(date).slice(0, 10)
      const dayStatus = map.get(key) || {}
      dayStatus[type] = record.status === 'completed' ? 'completed' : 'missed'
      map.set(key, dayStatus)
    })
  return map
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
  const todayKey = formatDateKey(new Date())
  return Array.from({ length: 7 }, (_, index) => ({
    index,
    label: String(index + 1),
    color: palette[index][0],
    bg: palette[index][1],
    date: dateForPlanDay(index),
    dateKey: formatDateKey(dateForPlanDay(index)),
    relation: relationForDate(formatDateKey(dateForPlanDay(index)), todayKey)
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
    selectCurrentPlanDay()
    await loadPlanCheckins()
    await loadLatestReview()
  } catch {
    // Store already exposes a user-facing error.
  }
}

function decorateDietCards(day) {
  return day.dietCards.map((item) => ({
    ...item,
    ...(cardMeta[item.key] || cardMeta.lunch),
    selectKey: cardSelectKey(day, item, 'diet'),
    preview: mealPreview(item)
  }))
}

function decorateExerciseCards(day) {
  return day.exerciseCards.map((item) => ({
    ...item,
    ...(cardMeta[item.key] || cardMeta.aerobic),
    selectKey: cardSelectKey(day, item, 'exercise'),
    preview: exercisePreview(item)
  }))
}

function selectDay(index) {
  selectedDayIndex.value = Math.min(index, Math.max(dayCards.value.length - 1, 0))
  selectedCard.value = null
}

async function loadLatestReview() {
  latestReviewLoaded.value = false
  try {
    const response = await getLatestInterventionReview()
    const review = response?.data ?? response
    latestReview.value = review?.review_id || review?.reviewId ? review : null
  } catch {
    latestReview.value = null
  } finally {
    latestReviewLoaded.value = true
  }
}

function sectionExecution(type) {
  const day = selectedDayState.value
  if (!day || day.relation === 'future') {
    return { label: '待完成', status: 'future', disabled: true }
  }
  if (day.relation === 'today') {
    return { label: '今日执行', status: 'today', disabled: false, hint: '前往今日打卡' }
  }

  const status = checkinStatusByDate.value.get(day.dateKey)?.[type]
  const completed = status === 'completed'
  return {
    label: completed ? '已完成' : '未完成',
    status: completed ? 'completed' : 'missed',
    disabled: false,
    hint: '查看当天打卡记录'
  }
}

function handleSectionAction(type) {
  const day = selectedDayState.value
  if (!day) return
  if (day.relation === 'future') return
  if (day.relation === 'today') {
    router.push('/app/checkin')
    return
  }
  router.push({
    path: '/app/checkin/history',
    query: {
      start_date: day.dateKey,
      end_date: day.dateKey,
      task_type: type
    }
  })
}

function selectCard(day, item, group) {
  const next = {
    selectKey: item.selectKey || cardSelectKey(day, item, group),
    dayTitle: day.title,
    group,
    title: item.title,
    content: item.content
  }
  selectedCard.value = selectedCard.value?.selectKey === next.selectKey ? null : next
}

function cardSelectKey(day, item, group) {
  return `${day.title}-${group}-${item.key || item.title}-${item.time || ''}`
}

function mealPreview(item) {
  const map = {
    breakfast: '点开查看今日早餐搭配与份量',
    lunch: '点开查看午餐主食、蛋白和蔬菜搭配',
    dinner: '点开查看晚餐清淡搭配与控糖重点',
    snack: '点开查看加餐选择和适合时间'
  }
  return map[item.key] || '点开查看这一餐的具体吃法'
}

function exercisePreview(item) {
  const map = {
    light: '点开查看轻运动动作和执行时机',
    aerobic: '点开查看有氧运动时长与强度',
    resistance: '点开查看抗阻或拉伸动作安排',
    notice: '点开查看运动前后的安全提醒'
  }
  return map[item.key] || '点开查看这一段的运动安排'
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

function openCurrentPlanDetail() {
  const planId = currentPlan.value?.id
  if (!planId) {
    showToast('暂无可查看的方案详情')
    return
  }
  pushWithBack(router, `/app/life-plan/${planId}`, '/app/life-plan')
}

async function submitGenerate(payload) {
  try {
    selectedCard.value = null
    await lifePlanStore.generateLifePlan(payload)
    selectCurrentPlanDay()
    await loadPlanCheckins()
    await loadLatestReview()
    showGenerateDialog.value = false
    showToast('生活方案已更新')
  } catch {
    // Error is displayed in the dialog.
  }
}

function formatReviewTag(value) {
  const text = String(value || '').trim()
  const dictionary = {
    exercise_adherence_low: '运动完成率偏低',
    diet_record_unstable: '饮食记录不稳定',
    execution_stable: '执行较稳定',
    exercise_duration: '运动时长',
    dinner_staple_reminder: '晚餐主食提醒',
    breakfast_structure: '早餐结构',
    water_reminder: '饮水提醒'
  }
  return dictionary[text] || text.replace(/_/g, ' ')
}

function dismissReviewCard() {
  const reviewId = getReviewId(latestReview.value)
  if (!reviewId) return
  localStorage.setItem(reviewDismissKey(reviewId), '1')
  dismissedReviewVersion.value += 1
}

function isReviewCardDismissed(review) {
  const reviewId = getReviewId(review)
  return reviewId ? localStorage.getItem(reviewDismissKey(reviewId)) === '1' : false
}

function isEffectiveAutoAdjustment(review) {
  const shouldUpdate = review?.should_update_plan ?? review?.shouldUpdatePlan
  const generatedPlanId = review?.generated_plan_id ?? review?.generatedPlanId
  const callStatus = review?.call_status ?? review?.callStatus
  return callStatus === 'success' && shouldUpdate === true && Boolean(generatedPlanId)
}

function getReviewId(review) {
  return review?.review_id ?? review?.reviewId ?? ''
}

function reviewDismissKey(reviewId) {
  return `diabetes_intervention_review_card_dismissed_${reviewId}`
}

async function loadPlanCheckins() {
  const start = planStartDate.value
  const plan = currentPlan.value
  if (!start || !plan?.id) {
    checkinRecords.value = []
    return
  }

  const end = addDays(start, Math.max(dayCards.value.length || 7, 1) - 1)
  try {
    const response = await getCheckinHistory({
      page: 1,
      page_size: 100,
      start_date: formatDateKey(start),
      end_date: formatDateKey(end)
    })
    const data = response?.data ?? response
    checkinRecords.value = data?.list || data?.records || []
  } catch {
    checkinRecords.value = []
  }
}

function selectCurrentPlanDay() {
  const start = planStartDate.value
  const total = dayCards.value.length || 7
  if (!start) {
    selectedDayIndex.value = 0
    return
  }
  const index = Math.min(Math.max(daysBetween(start, new Date()), 0), total - 1)
  selectedDayIndex.value = index
}

function dateForPlanDay(index) {
  return addDays(planStartDate.value || new Date(), index)
}

function relationForDate(dateKey, todayKey) {
  if (dateKey < todayKey) return 'past'
  if (dateKey > todayKey) return 'future'
  return 'today'
}

function parsePlanDate(value) {
  if (!value) return null
  const date = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return null
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

function addDays(date, days) {
  const base = date ? new Date(date) : new Date()
  base.setHours(0, 0, 0, 0)
  base.setDate(base.getDate() + days)
  return base
}

function daysBetween(start, end) {
  const startTime = addDays(start, 0).getTime()
  const endTime = addDays(end, 0).getTime()
  return Math.floor((endTime - startTime) / 86400000)
}

function formatDateKey(date) {
  const value = addDays(date, 0)
  return [
    value.getFullYear(),
    String(value.getMonth() + 1).padStart(2, '0'),
    String(value.getDate()).padStart(2, '0')
  ].join('-')
}
</script>
