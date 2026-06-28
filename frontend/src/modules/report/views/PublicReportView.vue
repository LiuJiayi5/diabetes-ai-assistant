<template>
  <main class="public-report-page">
    <section v-if="loading" class="public-report-state">
      <LoaderCircle class="spin" />
      <p>正在加载报告...</p>
    </section>

    <section v-else-if="report" class="public-report-document">
      <header class="public-report-header">
        <span>RPT{{ String(report.report_id || report.reportId).padStart(4, '0') }}</span>
        <h1>{{ report.report_title || report.reportTitle }}</h1>
        <p>{{ report.report_summary || report.reportSummary }}</p>
        <small>生成时间：{{ formatDateTime(report.create_time || report.createTime) }}</small>
      </header>

      <article class="public-report-markdown" :class="{ 'public-report-markdown--personal': isPersonalReport }">
        <MarkdownContent :content="report.report_markdown || report.reportMarkdown" />
      </article>
    </section>

    <section v-else class="public-report-state">
      <FileText />
      <p>报告不存在或暂时无法访问。</p>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import { FileText, LoaderCircle } from 'lucide-vue-next'
import MarkdownContent from '@/components/MarkdownContent.vue'
import { getPublicReportDetail } from '@/api/report'
import './public-report.css'

const route = useRoute()
const loading = ref(false)
const report = ref(null)

const isPersonalReport = computed(() => (report.value?.report_type || report.value?.reportType) !== 'doctor_summary')

onMounted(loadReport)

async function loadReport() {
  loading.value = true
  try {
    const response = await getPublicReportDetail(route.params.reportId)
    report.value = response.data
  } catch (error) {
    showToast(error?.response?.data?.message || '报告加载失败')
    report.value = null
  } finally {
    loading.value = false
  }
}

function formatDateTime(value) {
  if (!value) return '暂无'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>
