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
            <span>减重</span>
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
          <h2>暂无生活方案</h2>
          <p>生成新的生活方案后，会在这里展示当前 active 方案。</p>
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
          <SectionHeader
            type="diet"
            title="饮食管理"
            subtitle="定制专属饮食计划"
            :icon="Utensils"
          />
          <PlanItemCard
            v-for="item in dietItems"
            :key="item.title"
            v-bind="item"
            @click="openDetail"
          />

          <SectionHeader
            type="exercise"
            title="运动管理"
            subtitle="科学运动指导"
            :icon="Dumbbell"
          />
          <PlanItemCard
            v-for="item in exerciseItems"
            :key="item.title"
            v-bind="item"
            @click="openDetail"
          />

          <section class="reminder-card">
            <h2>作息与控糖提醒</h2>
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
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  Apple,
  ArrowLeft,
  BarChart2,
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
  Utensils,
  Waves
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

const currentPlan = computed(() => lifePlanStore.currentPlan ? normalizePlan(lifePlanStore.currentPlan) : null)
const riskMeta = computed(() => getRiskMeta(currentPlan.value))

const dietItems = computed(() => {
  const diet = currentPlan.value?.dietPlan || {}
  return [
    { title: '早餐建议', content: toText(diet.breakfast, '燕麦粥（无糖），一个水煮蛋和少量坚果'), time: '07:30-08:00', icon: Sun, iconBg: '#FEF3E2', iconColor: '#E8A840' },
    { title: '午餐建议', content: toText(diet.lunch, '清蒸鱼，半碗糙米饭和一份绿叶蔬菜'), time: '12:00-12:30', icon: Utensils, iconBg: '#E5F6EE', iconColor: '#5BBF8A' },
    { title: '晚餐建议', content: toText(diet.dinner, '鸡胸肉（少油清炖），一份凉拌黄瓜和半碗杂粮粥'), time: '18:00-18:30', icon: Moon, iconBg: '#E8EEF9', iconColor: '#7A9BD4' },
    { title: '加餐建议', content: toText(diet.snack, '低糖水果（如苹果或梨）和一小杯无糖酸奶'), time: '15:00-15:30', icon: Apple, iconBg: '#F3EAF8', iconColor: '#B07CD4' }
  ]
})

const exerciseItems = computed(() => {
  const exercise = currentPlan.value?.exercisePlan || {}
  return [
    { title: '晨练运动', content: toText(exercise.morning || exercise.exercise_type, '三十分钟的慢走，一周五次'), time: '07:00-07:30', icon: Footprints, iconBg: '#FEF3E2', iconColor: '#E8A840' },
    { title: '晚间运动', content: toText(exercise.evening || exercise.duration, '四十五分钟有氧运动，如游泳或骑行，一周三次'), time: '18:00-18:45', icon: Waves, iconBg: '#E4F3FB', iconColor: '#4FAAC4' },
    { title: '周末运动', content: toText(exercise.weekend || exercise.precautions, '一小时综合训练，包括力量训练和有氧运动，每周一次'), time: '09:00-10:00', icon: BarChart2, iconBg: '#E5F6EE', iconColor: '#5BBF8A' }
  ]
})

const reminderTips = computed(() => {
  const tips = currentPlan.value?.healthTips || []
  return tips.length ? tips : [
    '保持规律睡眠，尽量 23:00 前入睡',
    '避免长时间久坐，每 1 小时起身活动',
    '餐后 2 小时可记录血糖变化',
    currentPlan.value?.medicalWarning || 'AI 建议仅供参考，不能替代线下诊疗'
  ]
})

const SectionHeader = defineComponent({
  props: {
    type: String,
    title: String,
    subtitle: String,
    icon: [Object, Function]
  },
  setup(props) {
    return () => h('section', { class: ['section-header', `section-header--${props.type}`] }, [
      h('div', { class: 'section-header__left' }, [
        h('span', { class: 'section-header__icon' }, [h(props.icon)]),
        h('div', [h('h2', props.title), h('p', props.subtitle)])
      ]),
      h('span', { class: 'checkin-pill' }, [h(CheckCircle2), '已打卡'])
    ])
  }
})

const PlanItemCard = defineComponent({
  props: {
    title: String,
    content: String,
    time: String,
    icon: [Object, Function],
    iconBg: String,
    iconColor: String
  },
  emits: ['click'],
  setup(props, { emit }) {
    return () => h('button', { class: 'plan-item-card', type: 'button', onClick: () => emit('click') }, [
      h('span', { class: 'plan-item-card__icon', style: { background: props.iconBg, color: props.iconColor } }, [h(props.icon)]),
      h('span', { class: 'plan-item-card__content' }, [
        h('strong', props.title),
        h('span', props.content)
      ]),
      h('span', { class: 'plan-item-card__side' }, [
        h('span', props.time),
        h(ChevronRight)
      ])
    ])
  }
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
    const plan = await lifePlanStore.generateLifePlan(payload)
    showGenerateDialog.value = false
    showToast('生活方案生成成功')
    const planId = plan?.plan_id || plan?.id || lifePlanStore.currentPlan?.plan_id
    if (planId) {
      router.push(`/app/life-plan/${planId}`)
    }
  } catch {
    // Error is displayed in the dialog.
  }
}

function openDetail() {
  if (currentPlan.value?.id) {
    router.push(`/app/life-plan/${currentPlan.value.id}`)
  }
}
</script>
