<template>
  <div class="figma-page">
    <PageHeader title="评估记录" back-to="/app/account" />

    <div v-if="!loading && list.length === 0" class="figma-empty">暂无评估记录，先去发起一次风险预测吧。</div>

    <FigmaCard
      v-for="item in list"
      :key="item.assessment_id"
      clickable
      @click="router.push(`/app/risk/${item.assessment_id}`)"
    >
      <div class="figma-list-item">
        <div class="figma-list-item__icon figma-list-item__icon--blue">
          <ShieldAlert :size="22" />
        </div>
        <div class="figma-list-item__body">
          <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px">
            <RiskLevelTag :level="item.risk_level" />
            <span v-if="item.risk_score != null" class="history-score">{{ item.risk_score }} 分</span>
          </div>
          <p class="figma-list-item__desc">{{ item.summary || '查看详情' }}</p>
          <p class="figma-list-item__meta">{{ formatDateTime(item.create_time) }}</p>
        </div>
      </div>
    </FigmaCard>

    <div v-if="hasMore" style="margin-top: 8px">
      <button class="figma-btn-secondary" type="button" :disabled="loadingMore" @click="loadMore">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ShieldAlert } from 'lucide-vue-next'
import { showToast } from 'vant'
import PageHeader from '@/components/mobile/PageHeader.vue'
import FigmaCard from '@/components/mobile/FigmaCard.vue'
import RiskLevelTag from '@/components/mobile/RiskLevelTag.vue'
import { getRiskHistory } from '@/api/riskAssessment'
import { assertSuccess } from '@/utils/response'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(true)
const loadingMore = ref(false)
const page = ref(1)
const total = ref(0)
const list = ref([])

const hasMore = computed(() => list.value.length < total.value)

onMounted(() => loadPage(1, true))

async function loadPage(nextPage, reset = false) {
  if (reset) loading.value = true
  else loadingMore.value = true
  try {
    const data = assertSuccess(await getRiskHistory({ page: nextPage, page_size: 10 }))
    total.value = data.total || 0
    page.value = data.page || nextPage
    list.value = reset ? (data.list || []) : [...list.value, ...(data.list || [])]
  } catch (error) {
    showToast(error.message || '加载失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  loadPage(page.value + 1)
}
</script>

<style scoped>
.history-score {
  font-size: 12px;
  color: var(--figma-text-muted);
}
</style>
