<template>
  <MobileShell>
    <div class="patient-layout">
      <transition name="auto-adjust-toast">
        <div
          v-if="showAutoAdjustNotice"
          class="auto-adjust-toast"
          role="button"
          tabindex="0"
          @click="openAutoAdjustedPlan"
          @keydown.enter.prevent="openAutoAdjustedPlan"
          @keydown.space.prevent="openAutoAdjustedPlan"
        >
          <span class="auto-adjust-toast__icon">
            <Sparkles />
          </span>
          <span class="auto-adjust-toast__content">
            <strong>生活方案已自动调整</strong>
            <small>点击跳转到方案定制页查看调整原因</small>
          </span>
          <ChevronRight class="auto-adjust-toast__arrow" />
          <button
            type="button"
            class="auto-adjust-toast__close"
            aria-label="关闭自动调整提醒"
            @click.stop="dismissAutoAdjustNotice"
          >
            <X />
          </button>
        </div>
      </transition>
      <main class="patient-layout__body mobile-scroll">
        <RouterView />
      </main>
      <BottomNav />
    </div>
  </MobileShell>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronRight, Sparkles, X } from 'lucide-vue-next'
import MobileShell from '@/components/mobile/MobileShell.vue'
import BottomNav from '@/components/mobile/BottomNav.vue'
import { getLatestInterventionReview } from '@/api/interventionReview'
import { getTokenForScope } from '@/utils/token'

const AUTO_ADJUST_NOTICE_PREFIX = 'diabetes_auto_adjust_notice_seen_'
const AUTO_ADJUST_POLL_INTERVAL = 30000

const route = useRoute()
const router = useRouter()
const latestAutoAdjustReview = ref(null)
let autoAdjustTimer = null

const latestAutoAdjustReviewId = computed(() => getReviewId(latestAutoAdjustReview.value))
const showAutoAdjustNotice = computed(() => {
  const reviewId = latestAutoAdjustReviewId.value
  if (!reviewId) return false
  return !hasStoredFlag(`${AUTO_ADJUST_NOTICE_PREFIX}${reviewId}`)
})

onMounted(() => {
  loadAutoAdjustNotice()
  autoAdjustTimer = window.setInterval(loadAutoAdjustNotice, AUTO_ADJUST_POLL_INTERVAL)
})

onBeforeUnmount(() => {
  if (autoAdjustTimer) {
    window.clearInterval(autoAdjustTimer)
    autoAdjustTimer = null
  }
})

watch(() => route.fullPath, () => {
  loadAutoAdjustNotice()
})

async function loadAutoAdjustNotice() {
  if (!getTokenForScope('patient')) {
    latestAutoAdjustReview.value = null
    return
  }

  try {
    const response = await getLatestInterventionReview()
    const review = response?.data ?? response
    latestAutoAdjustReview.value = isEffectiveAutoAdjustment(review) ? review : null
  } catch {
    latestAutoAdjustReview.value = null
  }
}

function openAutoAdjustedPlan() {
  markAutoAdjustNoticeSeen()
  router.push('/app/life-plan')
}

function dismissAutoAdjustNotice() {
  markAutoAdjustNoticeSeen()
  latestAutoAdjustReview.value = null
}

function markAutoAdjustNoticeSeen() {
  const reviewId = latestAutoAdjustReviewId.value
  if (!reviewId) return
  localStorage.setItem(`${AUTO_ADJUST_NOTICE_PREFIX}${reviewId}`, '1')
}

function isEffectiveAutoAdjustment(review) {
  if (!review || !getReviewId(review)) return false
  const shouldUpdate = review.should_update_plan ?? review.shouldUpdatePlan
  const generatedPlanId = review.generated_plan_id ?? review.generatedPlanId
  const callStatus = review.call_status ?? review.callStatus
  return callStatus === 'success' && shouldUpdate === true && Boolean(generatedPlanId)
}

function getReviewId(review) {
  return review?.review_id ?? review?.reviewId ?? ''
}

function hasStoredFlag(key) {
  try {
    return localStorage.getItem(key) === '1'
  } catch {
    return false
  }
}
</script>

<style scoped>
.patient-layout {
  position: relative;
  min-height: 100%;
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  background: var(--figma-bg-page);
}

.auto-adjust-toast {
  position: absolute;
  z-index: 80;
  top: 12px;
  left: 14px;
  right: 14px;
  min-height: 64px;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 18px 28px;
  align-items: center;
  gap: 10px;
  padding: 10px 10px 10px 12px;
  border: 1px solid rgba(174, 232, 199, 0.46);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 42px rgba(57, 96, 78, 0.18);
  backdrop-filter: blur(12px);
  cursor: pointer;
}

.auto-adjust-toast__icon {
  width: 38px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #BCEBCF 0%, #C7E8F7 100%);
  color: #FFFFFF;
}

.auto-adjust-toast__icon svg,
.auto-adjust-toast__arrow,
.auto-adjust-toast__close svg {
  width: 16px;
  height: 16px;
}

.auto-adjust-toast__content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.auto-adjust-toast__content strong {
  overflow: hidden;
  color: var(--figma-text-strong);
  font-size: 14px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auto-adjust-toast__content small {
  color: var(--figma-text-muted);
  font-size: 11px;
  font-weight: 600;
  line-height: 1.35;
}

.auto-adjust-toast__arrow {
  color: #8DB7A0;
}

.auto-adjust-toast__close {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(174, 232, 199, 0.28);
  color: #4A7A62;
}

.auto-adjust-toast-enter-active,
.auto-adjust-toast-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.auto-adjust-toast-enter-from,
.auto-adjust-toast-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.patient-layout__body {
  flex: 1;
  overflow-y: auto;
}
</style>
