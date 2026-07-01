<template>
  <section class="smart-reco-panel" :class="`smart-reco-panel--${scenario}`">
    <header class="smart-reco-panel__head">
      <div>
        <span>{{ eyebrowText }}</span>
        <h2>{{ title }}</h2>
        <p v-if="profileSummary">{{ profileSummary }}</p>
      </div>
    </header>

    <div v-if="loading" class="smart-reco-state">
      <LoaderCircle class="spin" />
      <span>正在生成个性化推荐...</span>
    </div>

    <div
      v-else-if="items.length"
      class="smart-reco-stack"
      :class="{
        'is-animating-next': animationDirection === 'next',
        'is-animating-prev': animationDirection === 'prev',
        'is-dragging': drag.active
      }"
      :style="{ '--drag-x': `${drag.offsetX}px` }"
      @pointerdown="startDrag"
      @pointermove="moveDrag"
      @pointerup="endDrag"
      @pointercancel="cancelDrag"
      @pointerleave="endDrag"
    >
      <button
        v-for="layer in visibleLayers"
        :key="`${layer.layer}-${layer.item.recommendation_id || layer.item.article?.article_id || layer.index}`"
        type="button"
        class="smart-reco-card"
        :class="[`smart-reco-card--${layer.layer}`, { 'is-active': layer.layer === 'active' }]"
        :aria-hidden="layer.layer !== 'active'"
        @click.stop="handleCardClick(layer, $event)"
      >
        <span class="smart-reco-card__cover">
          <img v-if="articleCover(layer.item)" :src="articleCover(layer.item)" alt="" />
          <BookOpen v-else />
        </span>
        <span class="smart-reco-card__body">
          <strong>{{ layer.item.article?.title }}</strong>
          <small>{{ layer.item.reason }}</small>
          <span class="smart-reco-card__signals">
            <em v-for="signal in shortSignals(layer.item.source_signals)" :key="signal">{{ signal }}</em>
          </span>
        </span>
        <span class="smart-reco-card__meta">
          <Sparkles v-if="layer.item.knowledge_enhanced" />
          <BookOpen v-else />
        </span>
      </button>
      <button
        v-if="items.length > 1"
        type="button"
        class="smart-reco-next"
        aria-label="切换下一篇推荐文章"
        :disabled="isAnimating"
        @pointerdown.stop
        @click.stop="nextRecommendation"
      >
        <RefreshCw />
      </button>
    </div>

    <div v-else class="smart-reco-state">
      <BookOpen />
      <span>暂无推荐内容，完善档案和生活方案后会更精准。</span>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpen, LoaderCircle, RefreshCw, Sparkles } from 'lucide-vue-next'
import { resolveAssetUrl } from '@/utils/assets'
import { loadSmartRecommendationBundle } from '../utils/smartRecommendationCache'
import '../styles/articles.css'

const props = defineProps({
  scenario: { type: String, default: 'home' },
  title: { type: String, default: '为你推荐' },
  limit: { type: Number, default: 4 },
  showMore: { type: Boolean, default: true }
})

const router = useRouter()
const loading = ref(false)
const items = ref([])
const profile = ref(null)
const activeIndex = ref(0)
const isAnimating = ref(false)
const animationDirection = ref('')
const suppressClick = ref(false)
const drag = reactive({
  active: false,
  pointerId: null,
  startX: 0,
  startY: 0,
  offsetX: 0,
  moved: false,
  startedOnActiveCard: false
})
let animationTimer = null

const profileSummary = computed(() => profile.value?.summary || '')
const activeItem = computed(() => items.value[activeIndex.value] || items.value[0] || {})
const visibleLayers = computed(() => {
  const count = Math.min(items.value.length, 3)
  const layerNames = ['active', 'next', 'next-next']
  return Array.from({ length: count }, (_, offset) => {
    const index = resolveIndex(activeIndex.value + offset)
    return {
      layer: layerNames[offset],
      index,
      item: items.value[index]
    }
  }).filter((layer) => layer.item)
})
const eyebrowText = computed(() => {
  if (props.scenario === 'life_plan') return '配合今天的方案'
  if (props.scenario === 'intervention_review') return '根据近期执行情况更新'
  if (props.scenario === 'article_detail') return '继续阅读'
  return '按你的情况推荐'
})

onMounted(load)
onBeforeUnmount(() => {
  window.clearTimeout(animationTimer)
})
watch(() => [props.scenario, props.limit], load)
watch(items, () => {
  resetDrag()
  clearAnimation()
})

async function load() {
  loading.value = true
  try {
    const bundle = await loadSmartRecommendationBundle({
      scenario: props.scenario,
      limit: props.limit
    })
    items.value = bundle.items
    activeIndex.value = 0
    profile.value = bundle.profile
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

function shortSignals(signals = []) {
  return (Array.isArray(signals) ? signals : []).slice(0, 2)
}

function articleCover(item) {
  return resolveAssetUrl(item?.article?.cover_image || item?.article?.coverImage || '')
}

function nextRecommendation() {
  if (drag.active) resetDrag()
  rotateStack('next')
}

function prevRecommendation() {
  if (drag.active) resetDrag()
  rotateStack('prev')
}

function rotateStack(direction = 'next') {
  if (items.value.length < 2 || isAnimating.value) return
  resetDrag()
  isAnimating.value = true
  animationDirection.value = direction
  window.clearTimeout(animationTimer)
  animationTimer = window.setTimeout(() => {
    activeIndex.value = resolveIndex(activeIndex.value + (direction === 'next' ? 1 : -1))
    clearAnimation()
  }, 360)
}

function resolveIndex(index) {
  const length = items.value.length
  if (!length) return 0
  return ((index % length) + length) % length
}

function clearAnimation() {
  window.clearTimeout(animationTimer)
  isAnimating.value = false
  animationDirection.value = ''
}

function startDrag(event) {
  if (items.value.length < 2 || isAnimating.value || event.button > 0) return
  drag.active = true
  drag.pointerId = event.pointerId
  drag.startX = event.clientX
  drag.startY = event.clientY
  drag.offsetX = 0
  drag.moved = false
  drag.startedOnActiveCard = Boolean(event.target?.closest?.('.smart-reco-card--active'))
  event.currentTarget?.setPointerCapture?.(event.pointerId)
}

function moveDrag(event) {
  if (!drag.active || drag.pointerId !== event.pointerId) return
  const diffX = event.clientX - drag.startX
  const diffY = event.clientY - drag.startY
  if (Math.abs(diffY) > Math.abs(diffX) && Math.abs(diffY) > 18) return
  drag.offsetX = Math.max(Math.min(diffX, 76), -96)
  drag.moved = Math.abs(drag.offsetX) > 12
}

function endDrag(event) {
  if (!drag.active || drag.pointerId !== event.pointerId) return
  const offset = drag.offsetX
  const threshold = Math.max(40, Math.min(72, (event.currentTarget?.clientWidth || 320) * 0.18))
  const wasMoved = drag.moved
  event.currentTarget?.releasePointerCapture?.(event.pointerId)
  const shouldOpenActiveCard = !wasMoved && drag.startedOnActiveCard
  resetDrag()
  if (Math.abs(offset) < threshold) {
    if (shouldOpenActiveCard && !isAnimating.value) {
      temporarilySuppressClick()
      openRecommendation(activeItem.value)
    } else if (wasMoved) {
      temporarilySuppressClick()
    }
    return
  }
  temporarilySuppressClick()
  if (offset < 0) {
    nextRecommendation()
  } else {
    prevRecommendation()
  }
}

function cancelDrag(event) {
  if (!drag.active || drag.pointerId !== event.pointerId) return
  event.currentTarget?.releasePointerCapture?.(event.pointerId)
  resetDrag()
}

function resetDrag() {
  drag.active = false
  drag.pointerId = null
  drag.startX = 0
  drag.startY = 0
  drag.offsetX = 0
  drag.moved = false
  drag.startedOnActiveCard = false
}

function handleCardClick(layer, event) {
  if (layer.layer !== 'active' || isAnimating.value || drag.moved || suppressClick.value) return
  openRecommendation(layer.item)
}

function temporarilySuppressClick() {
  suppressClick.value = true
  window.setTimeout(() => {
    suppressClick.value = false
  }, 80)
}

function openRecommendation(item) {
  const articleId = item.article?.article_id
    ?? item.article?.articleId
    ?? item.article?.id
    ?? item.article_id
    ?? item.articleId
    ?? item.id
  if (!articleId) return
  router.push({
    path: `/app/articles/${articleId}`,
    query: {
      recommendation_id: item.recommendation_id,
      scenario: props.scenario
    }
  })
}
</script>
