<template>
  <div class="report-page">
    <header class="report-topbar">
      <button type="button" class="icon-button" @click="router.push('/app/reports')">
        <ArrowLeft />
      </button>
      <div>
        <p>报告详情</p>
        <h1>{{ report?.report_type_label || report?.reportTypeLabel || '健康报告' }}</h1>
      </div>
      <button type="button" class="icon-button" @click="loadDetail">
        <RefreshCw :class="{ spin: loading }" />
      </button>
    </header>

    <main class="report-scroll mobile-scroll">
      <section v-if="loading" class="state-card">
        <LoaderCircle class="spin" />
        <p>正在加载报告...</p>
      </section>

      <template v-else-if="report">
        <section class="detail-summary">
          <div>
            <span>RPT{{ String(report.report_id || report.reportId).padStart(4, '0') }}</span>
            <h2>{{ report.report_title || report.reportTitle }}</h2>
            <p>{{ report.report_summary || report.reportSummary }}</p>
          </div>
          <div class="score-ring score-ring--large">
            <strong>{{ report.completeness_score ?? report.completenessScore ?? 0 }}</strong>
            <small>完整度</small>
          </div>
        </section>

        <section class="export-panel">
          <button v-for="item in exportItems" :key="item.type" type="button" @click="download(item.type)">
            <component :is="item.icon" />
            <span>{{ item.label }}</span>
          </button>
        </section>

        <section class="trace-panel">
          <img
            v-if="report.qr_code_data_url || report.qrCodeDataUrl"
            :src="report.qr_code_data_url || report.qrCodeDataUrl"
            alt="报告追溯二维码"
          />
          <div>
            <h2>报告追溯码</h2>
            <p>{{ report.trace_url || report.traceUrl }}</p>
          </div>
        </section>

        <section v-if="missingItems.length" class="missing-panel">
          <h2>数据完整度提醒</h2>
          <div>
            <span v-for="item in missingItems" :key="item">{{ item }}</span>
          </div>
        </section>

        <section class="markdown-panel">
          <MarkdownContent :content="report.report_markdown || report.reportMarkdown" />
        </section>
      </template>

      <section v-else class="state-card">
        <FileText />
        <p>报告不存在或已被删除。</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { ArrowLeft, Braces, Download, FileJson, FileText, LoaderCircle, RefreshCw } from 'lucide-vue-next'
import MarkdownContent from '@/components/MarkdownContent.vue'
import { downloadReportExport, getReportDetail } from '../api'
import './report.css'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const report = ref(null)

const exportItems = [
  { type: 'pdf', label: 'PDF', icon: Download },
  { type: 'markdown', label: 'Markdown', icon: FileText },
  { type: 'fhir', label: 'FHIR JSON', icon: FileJson },
  { type: 'hl7', label: 'HL7 v2', icon: Braces }
]

const missingItems = computed(() => report.value?.missing_items || report.value?.missingItems || [])

onMounted(loadDetail)

async function loadDetail() {
  loading.value = true
  try {
    const response = await getReportDetail(route.params.reportId)
    report.value = response.data
  } catch (error) {
    showToast(error?.response?.data?.message || '报告详情加载失败')
    report.value = null
  } finally {
    loading.value = false
  }
}

function download(type) {
  const id = report.value?.report_id || report.value?.reportId
  if (!id) return
  const filenames = {
    pdf: `report-${id}.pdf`,
    markdown: `report-${id}.md`,
    fhir: `report-${id}.fhir.json`,
    hl7: `report-${id}.hl7`
  }
  downloadReportExport(id, type)
    .then((blob) => {
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filenames[type] || `report-${id}.${type}`
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    })
    .catch((error) => {
      showToast(error?.response?.data?.message || '导出失败')
    })
}
</script>
