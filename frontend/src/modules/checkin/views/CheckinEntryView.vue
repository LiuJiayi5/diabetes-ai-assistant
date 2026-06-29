<template>
  <CheckinPageShell title="今日打卡" subtitle="跟随当前生活方案完成每日执行" @refresh="loadToday">
    <section class="hero-card">
      <div class="hero-copy">
        <p class="eyebrow">今日进度</p>
        <h2>{{ completedCount }}/{{ tasks.length }} 项已完成</h2>
        <p>{{ todayText }}</p>
      </div>
      <div class="hero-ring" :style="{ '--percent': completionPercent }">
        <span>{{ completionPercent }}%</span>
      </div>
    </section>

    <section v-if="planInfo.plan_id" class="plan-card">
      <div class="plan-card__head">
        <div>
          <p class="eyebrow">当前生活方案</p>
          <h3>{{ planInfo.plan_title || '个性化控糖生活方案' }}</h3>
        </div>
        <span class="day-pill" :class="{ 'day-pill--expired': planInfo.is_plan_expired }">
          {{ planDayText }}
        </span>
      </div>

      <p class="plan-goal">{{ planInfo.plan_goal || '控糖管理' }}</p>
      <p class="plan-summary">{{ planInfo.today_focus || planInfo.plan_summary || '今天按方案完成饮食与运动记录。' }}</p>

      <div class="schedule-grid" v-if="todayDiet || todayExercise">
        <article v-if="todayDiet" class="schedule-mini schedule-mini--diet">
          <Salad :size="16" />
          <div>
            <strong>今日饮食</strong>
            <p>{{ todayDiet }}</p>
          </div>
        </article>
        <article v-if="todayExercise" class="schedule-mini schedule-mini--exercise">
          <Dumbbell :size="16" />
          <div>
            <strong>今日运动</strong>
            <p>{{ todayExercise }}</p>
          </div>
        </article>
      </div>

      <button class="detail-link" type="button" @click="goPlanDetail">
        <ClipboardList :size="16" />
        <span>查看方案详情</span>
        <ArrowRight :size="15" />
      </button>
    </section>

    <section v-if="loading" class="state-card">
      <LoaderCircle class="spin" :size="24" />
      <p>正在读取今日打卡任务</p>
    </section>

    <section v-else-if="tasks.length === 0" class="state-card">
      <CalendarX2 :size="34" />
      <h3>暂无今日任务</h3>
      <p>{{ message || '当前暂无有效生活方案，请先生成生活方案后再进行打卡。' }}</p>
      <RouterLink class="primary-link" to="/app/life-plan">去查看生活方案</RouterLink>
    </section>

    <section v-else class="task-list">
      <article v-for="task in tasks" :key="task.checkin_id" class="task-card">
        <div class="task-head">
          <div class="task-icon" :class="`task-icon--${task.task_type}`">
            <component :is="taskIcon(task.task_type)" :size="19" :stroke-width="2" />
          </div>
          <div>
            <h3>{{ task.task_name || taskTypeText(task.task_type) }}</h3>
            <p>{{ taskTypeText(task.task_type) }} · {{ statusText(task.status) }}</p>
          </div>
          <span class="status-pill" :class="`status-pill--${task.status}`">{{ statusText(task.status) }}</span>
        </div>

        <p class="task-advice">{{ taskAdvice(task) }}</p>

        <div class="status-actions">
          <button
            v-for="item in statusOptions"
            :key="item.value"
            type="button"
            :class="{ active: draftMap[task.checkin_id]?.status === item.value }"
            @click="setDraftStatus(task.checkin_id, item.value)"
          >
            <component :is="item.icon" :size="15" />
            <span>{{ item.label }}</span>
          </button>
        </div>

        <textarea
          v-model="draftMap[task.checkin_id].note"
          class="note-input"
          rows="2"
          maxlength="120"
          placeholder="可补充今天的饮食、运动感受"
        />

        <button class="submit-button" type="button" :disabled="savingId === task.checkin_id" @click="saveTask(task)">
          <LoaderCircle v-if="savingId === task.checkin_id" class="spin" :size="16" />
          <CheckCircle2 v-else :size="17" />
          <span>保存打卡</span>
        </button>
      </article>
    </section>

    <p class="medical-note">AI 建议仅供参考，不能替代线下诊疗。</p>
  </CheckinPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import {
  ArrowRight,
  CalendarX2,
  CheckCircle2,
  Circle,
  ClipboardList,
  Dumbbell,
  LoaderCircle,
  Salad,
  XCircle
} from 'lucide-vue-next'
import { getTodayCheckins, submitCheckin } from '@/api/checkin'
import { pushWithBack } from '@/utils/navigation'
import CheckinPageShell from '../components/CheckinPageShell.vue'

const router = useRouter()
const loading = ref(false)
const savingId = ref(null)
const tasks = ref([])
const message = ref('')
const planInfo = ref({})
const draftMap = reactive({})

const statusOptions = [
  { label: '待完成', value: 'pending', icon: Circle },
  { label: '已完成', value: 'completed', icon: CheckCircle2 },
  { label: '未完成', value: 'missed', icon: XCircle }
]

const completedCount = computed(() => tasks.value.filter((task) => task.status === 'completed').length)
const completionPercent = computed(() => {
  if (!tasks.value.length) return 0
  return Math.round((completedCount.value / tasks.value.length) * 100)
})

const todayText = computed(() => new Date().toLocaleDateString('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}))

const planDayText = computed(() => {
  const day = Number(planInfo.value.plan_day || 0)
  const total = Number(planInfo.value.total_plan_days || 0)
  if (planInfo.value.is_plan_expired) return total ? `已完成 ${total} 天周期` : '周期已结束'
  if (day > 0 && total > 0) return `第 ${day} / ${total} 天`
  return '执行中'
})

const todayDiet = computed(() => scheduleText(['diet', 'diet_advice', 'diet_plan', 'dietPlan', 'meal', 'meals']))
const todayExercise = computed(() => scheduleText(['exercise', 'exercise_advice', 'exercise_plan', 'exercisePlan', 'sport', 'training']))

function unwrap(response) {
  return response?.data ?? response
}

function hydrateDrafts(list) {
  list.forEach((task) => {
    draftMap[task.checkin_id] = {
      status: task.status || 'pending',
      note: task.note || ''
    }
  })
}

async function loadToday() {
  loading.value = true
  try {
    const data = unwrap(await getTodayCheckins())
    tasks.value = data?.list || []
    message.value = data?.message || ''
    planInfo.value = data || {}
    hydrateDrafts(tasks.value)
  } catch (error) {
    showFailToast(error?.response?.data?.message || '今日打卡任务读取失败')
  } finally {
    loading.value = false
  }
}

function setDraftStatus(checkinId, status) {
  draftMap[checkinId].status = status
}

async function saveTask(task) {
  savingId.value = task.checkin_id
  try {
    const payload = {
      checkin_id: task.checkin_id,
      status: draftMap[task.checkin_id].status,
      note: draftMap[task.checkin_id].note
    }
    const updated = unwrap(await submitCheckin(payload))
    const index = tasks.value.findIndex((item) => item.checkin_id === task.checkin_id)
    if (index >= 0) {
      tasks.value[index] = {
        ...tasks.value[index],
        ...updated,
        plan_advice: updated?.plan_advice || tasks.value[index]?.plan_advice
      }
    }
    hydrateDrafts([tasks.value[index] || updated])
    showSuccessToast('打卡已保存')
  } catch (error) {
    showFailToast(error?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    savingId.value = null
  }
}

function goPlanDetail() {
  if (planInfo.value.plan_id) {
    pushWithBack(router, `/app/life-plan/${planInfo.value.plan_id}`, '/app/life-plan')
  } else {
    router.push('/app/life-plan')
  }
}

function taskTypeText(type) {
  const map = {
    diet: '饮食打卡',
    exercise: '运动打卡'
  }
  return map[type] || '生活打卡'
}

function statusText(status) {
  const map = {
    pending: '待完成',
    completed: '已完成',
    missed: '未完成'
  }
  return map[status] || '待完成'
}

function taskIcon(type) {
  return type === 'exercise' ? Dumbbell : Salad
}

function taskAdvice(task) {
  if (task?.plan_advice) return task.plan_advice
  if (task?.task_type === 'diet') {
    return todayDiet.value || planInfo.value.today_focus || '按今日方案控制主食比例，搭配优质蛋白和蔬菜，记录饮食感受。'
  }
  if (task?.task_type === 'exercise') {
    return todayExercise.value || planInfo.value.today_focus || '按今日方案完成适合自己的活动安排，如有不适请及时停止并记录。'
  }
  return planInfo.value.today_focus || '按今日方案完成打卡并记录身体感受。'
}

function scheduleText(keys) {
  const schedule = planInfo.value.today_schedule || {}
  for (const key of keys) {
    const text = valueText(schedule[key])
    if (text) return text
  }
  return ''
}

function valueText(value) {
  if (value == null) return ''
  if (typeof value === 'string') return value.trim()
  if (Array.isArray(value)) return value.map(valueText).filter(Boolean).join('；')
  if (typeof value === 'object') return Object.values(value).map(valueText).filter(Boolean).join('；')
  return String(value)
}

onMounted(loadToday)
</script>

<style scoped>
.hero-card {
  min-height: 132px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  padding: 20px;
  border-radius: var(--figma-radius-card-lg);
  background: linear-gradient(135deg, #AEE8C7 0%, #BDEDD9 45%, #BFE9F2 100%);
  box-shadow: var(--figma-shadow-hero);
  overflow: hidden;
}

.hero-copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 5px;
  color: rgba(36, 50, 61, 0.65);
  font-size: 11px;
  font-weight: 700;
}

.hero-card h2 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 21px;
  font-weight: 700;
  line-height: 1.35;
}

.hero-card p:last-child {
  margin: 6px 0 0;
  color: rgba(36, 50, 61, 0.68);
  font-size: 12px;
}

.hero-ring {
  width: 78px;
  height: 78px;
  flex: 0 0 78px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: conic-gradient(#4FB783 calc(var(--percent, 0) * 1%), rgba(255,255,255,0.55) 0);
  box-shadow: inset 0 0 0 9px rgba(255,255,255,0.55);
}

.hero-ring span {
  color: #3C8D66;
  font-size: 18px;
  font-weight: 700;
}

.plan-card,
.task-card,
.state-card {
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.plan-card {
  margin-bottom: 14px;
  padding: 16px;
}

.plan-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.plan-card h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 15px;
  font-weight: 800;
  line-height: 1.45;
}

.day-pill {
  flex: 0 0 auto;
  padding: 5px 10px;
  border-radius: var(--figma-radius-pill);
  background: #E5F6EE;
  color: #3C8D66;
  font-size: 11px;
  font-weight: 700;
}

.day-pill--expired {
  background: #FFF4D7;
  color: #B8862A;
}

.plan-goal,
.plan-summary,
.schedule-mini p,
.task-advice {
  color: var(--figma-text-secondary);
  line-height: 1.65;
}

.plan-goal {
  margin: 8px 0 0;
  font-size: 12px;
  font-weight: 700;
}

.plan-summary {
  margin: 7px 0 0;
  font-size: 12px;
}

.schedule-grid {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.schedule-mini {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 9px;
  padding: 10px;
  border-radius: 16px;
}

.schedule-mini--diet {
  background: #F2FAF5;
  color: #4FB783;
}

.schedule-mini--exercise {
  background: #F0F8FC;
  color: #4FAAC4;
}

.schedule-mini strong {
  display: block;
  color: var(--figma-text-strong);
  font-size: 12px;
}

.schedule-mini p {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 2px 0 0;
  font-size: 11px;
}

.detail-link {
  width: 100%;
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 12px;
  border-radius: var(--figma-radius-pill);
  background: #F7FCF9;
  color: #3C8D66;
  font-size: 13px;
  font-weight: 700;
}

.task-list {
  display: grid;
  gap: 12px;
}

.task-card {
  padding: 16px;
}

.task-head {
  display: grid;
  grid-template-columns: 42px 1fr auto;
  gap: 10px;
  align-items: center;
}

.task-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 14px;
}

.task-icon--diet {
  background: #E5F6EE;
  color: #4FB783;
}

.task-icon--exercise {
  background: #E4F3FB;
  color: #4FAAC4;
}

.task-head h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 15px;
  font-weight: 700;
}

.task-head p {
  margin: 2px 0 0;
  color: var(--figma-text-muted);
  font-size: 11px;
}

.task-advice {
  margin: 12px 0 0;
  padding: 10px 12px;
  border-radius: 14px;
  background: #F7FCF9;
  color: #1F5F43;
  font-size: 12px;
  font-weight: 500;
}

.status-pill {
  padding: 3px 9px;
  border-radius: var(--figma-radius-pill);
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
}

.status-pill--completed {
  background: rgba(111, 207, 151, 0.15);
  color: #4FB783;
}

.status-pill--missed {
  background: rgba(239, 143, 143, 0.12);
  color: #E87878;
}

.status-pill--pending {
  background: #F7E9CC;
  color: #B8862A;
}

.status-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 14px;
}

.status-actions button {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: var(--figma-radius-pill);
  background: #F7FCF9;
  color: var(--figma-text-muted);
  font-size: 12px;
  font-weight: 500;
}

.status-actions button.active {
  background: var(--figma-secondary-green);
  color: #4FB783;
}

.note-input {
  width: 100%;
  margin-top: 12px;
  padding: 12px 14px;
  border: 1px solid transparent;
  border-radius: var(--figma-radius-input);
  background: #F7FCF9;
  color: var(--figma-text-primary);
  font-size: 13px;
  line-height: 1.5;
  resize: none;
}

.note-input:focus {
  border-color: var(--figma-primary-green);
  background: #FFFFFF;
  outline: none;
}

.submit-button,
.primary-link {
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 600;
  box-shadow: var(--figma-shadow-button);
}

.submit-button {
  width: 100%;
  margin-top: 12px;
}

.submit-button:disabled {
  opacity: 0.7;
}

.state-card {
  min-height: 210px;
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

.state-card p {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
}

.primary-link {
  width: 180px;
  margin-top: 4px;
}

.medical-note {
  margin: 14px 0 0;
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
