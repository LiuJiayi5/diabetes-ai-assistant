<template>
  <div class="figma-page">
    <PageHeader title="健康数据" back-to="/app/account" />

    <FigmaCard variant="soft">
      <p class="history-tip">这里展示你历次录入的健康指标。日常录入请前往「风险预测」页。</p>
      <button class="figma-btn-secondary" type="button" @click="router.push('/app/risk')">
        去录入并评估
      </button>
    </FigmaCard>

    <h2 class="figma-page__section-title">历史记录</h2>

    <div v-if="!loading && list.length === 0" class="figma-empty">暂无健康数据，先去风险预测页录入吧。</div>

    <FigmaCard v-for="item in list" :key="item.metric_id">
      <div class="figma-list-item">
        <div class="figma-list-item__icon figma-list-item__icon--green">
          <Activity :size="22" />
        </div>
        <div class="figma-list-item__body">
          <h3 class="figma-list-item__title">{{ item.recorded_at }}</h3>
          <p class="figma-list-item__desc">{{ buildMetricSummary(item) }}</p>
          <p class="figma-list-item__meta">
            <span v-if="item.hba1c">糖化 {{ item.hba1c }}%</span>
            <span v-if="item.diet_status"> · {{ item.diet_status }}</span>
          </p>
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
import { Activity } from 'lucide-vue-next'
import { showToast } from 'vant'
import PageHeader from '@/components/mobile/PageHeader.vue'
import FigmaCard from '@/components/mobile/FigmaCard.vue'
import { getMetricHistory } from '@/api/healthMetric'
import { assertSuccess } from '@/utils/response'
import { buildMetricSummary } from '@/utils/health'

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
    const data = assertSuccess(await getMetricHistory({ page: nextPage, page_size: 10 }))
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
.history-tip {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--figma-text-secondary);
}
</style>
