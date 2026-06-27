<template>
  <CheckinPageShell title="今日打卡" subtitle="记录饮食与运动完成情况" @refresh="loadToday">
    <section class="hero-card">
      <div>
        <p class="eyebrow">今日进度</p>
        <h2>{{ completedCount }}/{{ tasks.length }} 项已完成</h2>
        <p>{{ todayText }}</p>
      </div>
      <div class="hero-ring" :style="{ '--percent': completionPercent }">
        <span>{{ completionPercent }}%</span>
      </div>
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
import { RouterLink } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import {
  CalendarX2,
  CheckCircle2,
  Circle,
  Dumbbell,
  LoaderCircle,
  Salad,
  XCircle
} from 'lucide-vue-next'
import { getTodayCheckins, submitCheckin } from '@/api/checkin'
import CheckinPageShell from '../components/CheckinPageShell.vue'

const loading = ref(false)
const savingId = ref(null)
const tasks = ref([])
const message = ref('')
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
    if (index >= 0) tasks.value[index] = updated
    hydrateDrafts([updated])
    showSuccessToast('打卡已保存')
  } catch (error) {
    showFailToast(error?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    savingId.value = null
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

.eyebrow {
  margin: 0 0 5px;
  color: rgba(36, 50, 61, 0.65);
  font-size: 11px;
  font-weight: 600;
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

.task-list {
  display: grid;
  gap: 12px;
}

.task-card,
.state-card {
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
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
  font-weight: 600;
}

.task-head p {
  margin: 2px 0 0;
  color: var(--figma-text-muted);
  font-size: 11px;
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
