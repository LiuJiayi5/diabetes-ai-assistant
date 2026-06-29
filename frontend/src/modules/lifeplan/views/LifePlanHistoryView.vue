<template>
  <div class="life-plan-page">
    <header class="life-plan-topbar">
      <button type="button" class="life-back-button" @click="router.push('/app/life-plan')">
        <ArrowLeft />
      </button>
      <h1>历史方案</h1>
      <span class="topbar-placeholder" />
    </header>

    <main class="life-plan-scroll mobile-scroll">
      <section v-if="lifePlanStore.loading" class="state-card">
        <LoaderCircle class="spin" />
        <p>正在加载历史方案...</p>
      </section>

      <section v-else-if="lifePlanStore.error" class="state-card state-card--error">
        <CircleAlert />
        <p>{{ lifePlanStore.error }}</p>
        <button type="button" @click="loadPlans">重新加载</button>
      </section>

      <section v-else-if="!plans.length" class="empty-card">
        <span class="empty-card__icon">
          <History />
        </span>
        <h2>暂无历史方案</h2>
        <p>生成新的生活方案后会在这里展示。</p>
        <button type="button" class="regenerate-button" @click="router.push('/app/life-plan')">返回生成</button>
      </section>

      <button
        v-for="plan in plans"
        v-else
        :key="plan.id"
        type="button"
        class="history-card"
        @click="openPlan(plan)"
      >
        <span class="history-card__icon">
          <FileText />
        </span>
        <span class="history-card__body">
          <span class="history-card__title">{{ plan.title }}</span>
          <span class="history-card__meta">
            {{ plan.goal }} · {{ plan.status || 'history' }} · {{ plan.call_status || 'unknown' }}
          </span>
          <span class="history-card__summary">{{ plan.summary }}</span>
          <span class="history-card__time">{{ formatPlanTime(plan.create_time || plan.update_time) }}</span>
        </span>
        <ChevronRight />
      </button>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ChevronRight, CircleAlert, FileText, History, LoaderCircle } from 'lucide-vue-next'
import { useLifePlanStore } from '@/stores/lifePlan'
import { pushWithBack } from '@/utils/navigation'
import { formatPlanTime, normalizePlan } from '../utils'
import '../styles/life-plan.css'

const router = useRouter()
const lifePlanStore = useLifePlanStore()
const plans = computed(() => lifePlanStore.plans.map(normalizePlan))

onMounted(loadPlans)

async function loadPlans() {
  try {
    await lifePlanStore.fetchLifePlans()
  } catch {
    // Store exposes the error.
  }
}

function openPlan(plan) {
  pushWithBack(router, `/app/life-plan/${plan.id}`, '/app/life-plan/history')
}
</script>
