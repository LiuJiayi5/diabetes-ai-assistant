<template>
  <div class="life-plan-page">
    <header class="life-plan-topbar">
      <button type="button" class="life-back-button" @click="goBack">
        <ArrowLeft />
      </button>
      <h1>方案详情</h1>
      <button type="button" class="adjust-button" @click="router.push('/app/life-plan/history')">历史</button>
    </header>

    <main class="life-plan-scroll mobile-scroll">
      <section v-if="lifePlanStore.detailLoading" class="state-card">
        <LoaderCircle class="spin" />
        <p>正在加载方案详情...</p>
      </section>

      <section v-else-if="lifePlanStore.detailError" class="state-card state-card--error">
        <CircleAlert />
        <p>{{ lifePlanStore.detailError }}</p>
        <button type="button" @click="loadDetail">重新加载</button>
      </section>

      <template v-else-if="detail">
        <section class="detail-hero">
          <span>{{ formatPlanTime(detail.update_time || detail.create_time) }}</span>
          <h2>{{ detail.title }}</h2>
          <p>{{ detail.summary }}</p>
        </section>

        <section class="detail-card" v-if="detail.riskSummary">
          <h2>风险摘要</h2>
          <p>{{ detail.riskSummary }}</p>
        </section>

        <section class="detail-card">
          <h2>饮食方案</h2>
          <p><strong>早餐：</strong>{{ toText(detail.dietPlan.breakfast) }}</p>
          <p><strong>午餐：</strong>{{ toText(detail.dietPlan.lunch) }}</p>
          <p><strong>晚餐：</strong>{{ toText(detail.dietPlan.dinner) }}</p>
          <p><strong>加餐：</strong>{{ toText(detail.dietPlan.snack) }}</p>
          <ul v-if="dietPrinciples.length">
            <li v-for="item in dietPrinciples" :key="item">{{ item }}</li>
          </ul>
        </section>

        <section class="detail-card">
          <h2>运动方案</h2>
          <p><strong>类型：</strong>{{ toText(detail.exercisePlan.exercise_type) }}</p>
          <p><strong>频率：</strong>{{ toText(detail.exercisePlan.frequency) }}</p>
          <p><strong>时长：</strong>{{ toText(detail.exercisePlan.duration) }}</p>
          <p><strong>强度：</strong>{{ toText(detail.exercisePlan.intensity) }}</p>
          <p><strong>注意：</strong>{{ toText(detail.exercisePlan.precautions) }}</p>
        </section>

        <section class="detail-card" v-if="workRestText">
          <h2>作息方案</h2>
          <p>{{ workRestText }}</p>
        </section>

        <section class="detail-card" v-if="Object.keys(scheduleGroups).length">
          <h2>每日安排</h2>
          <div v-for="(items, day) in scheduleGroups" :key="day" class="day-group">
            <strong>{{ day }}</strong>
            <p v-for="item in items" :key="`${day}-${item.time || item.task_name || item.name}`">
              {{ item.time || item.period || '全天' }} · {{ item.task_name || item.name || item.content || toText(item) }}
            </p>
          </div>
        </section>

        <section class="detail-card" v-if="detail.checkinTasks.length">
          <h2>打卡任务</h2>
          <ul>
            <li v-for="task in detail.checkinTasks" :key="task.task_name || task.name">
              {{ task.task_name || task.name }}：{{ task.description || task.task_type }}
            </li>
          </ul>
        </section>

        <section class="detail-card detail-card--notice">
          <h2>健康提示</h2>
          <ul>
            <li v-for="tip in tips" :key="tip">{{ tip }}</li>
            <li>{{ detail.medicalWarning }}</li>
          </ul>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CircleAlert, LoaderCircle } from 'lucide-vue-next'
import { useLifePlanStore } from '@/stores/lifePlan'
import { backPathFromRoute } from '@/utils/navigation'
import { formatPlanTime, groupScheduleByDay, normalizePlan, toText } from '../utils'
import '../styles/life-plan.css'

const route = useRoute()
const router = useRouter()
const lifePlanStore = useLifePlanStore()

const detail = computed(() => lifePlanStore.detail ? normalizePlan(lifePlanStore.detail) : null)
const dietPrinciples = computed(() => {
  const principles = detail.value?.dietPlan?.diet_principles
  return Array.isArray(principles) ? principles : []
})
const workRestText = computed(() => toText(detail.value?.workRestPlan, ''))
const scheduleGroups = computed(() => groupScheduleByDay(detail.value?.dailySchedule || []))
const tips = computed(() => detail.value?.healthTips?.length ? detail.value.healthTips : ['保持规律睡眠，避免长时间久坐'])

onMounted(loadDetail)

async function loadDetail() {
  try {
    await lifePlanStore.fetchLifePlanDetail(route.params.planId)
  } catch {
    // Store exposes the error.
  }
}

function goBack() {
  router.push(backPathFromRoute(route, '/app/life-plan'))
}
</script>
