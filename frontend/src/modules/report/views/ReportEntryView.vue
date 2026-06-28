<template>
  <div class="report-page">
    <header class="report-topbar">
      <button type="button" class="icon-button" @click="router.push('/app/account')">
        <ArrowLeft />
      </button>
      <div>
        <p>就医沟通</p>
        <h1>健康报告</h1>
      </div>
      <button type="button" class="icon-button" @click="loadReports">
        <RefreshCw :class="{ spin: loading }" />
      </button>
    </header>

    <main class="report-scroll mobile-scroll">
      <section class="report-hero">
        <div>
          <span>病例式报告中心</span>
          <h2>把健康档案、指标、风险、方案和打卡整理成一份可携带报告</h2>
          <p>PDF/Markdown 用于阅读与打印，FHIR/HL7 用于互操作扩展示范。</p>
        </div>
      </section>

      <section class="generate-panel">
        <div class="panel-title">
          <FileHeart />
          <div>
            <h2>生成新报告</h2>
            <p>建议先完成健康档案、指标录入、风险预测和生活方案。</p>
          </div>
        </div>

        <div class="segmented">
          <button
            v-for="type in reportTypes"
            :key="type.value"
            type="button"
            :class="{ active: form.report_type === type.value }"
            @click="form.report_type = type.value"
          >
            {{ type.label }}
          </button>
        </div>

        <label class="field-row">
          <span>报告周期</span>
          <select v-model.number="form.days">
            <option :value="7">近 7 天</option>
            <option :value="30">近 30 天</option>
            <option :value="90">近 90 天</option>
          </select>
        </label>

        <button type="button" class="primary-button" :disabled="generating" @click="handleGenerate">
          <LoaderCircle v-if="generating" class="spin" />
          <FilePlus2 v-else />
          <span>{{ generating ? '正在生成...' : '生成报告' }}</span>
        </button>
      </section>

      <section class="report-list">
        <div class="section-heading">
          <h2>历史报告</h2>
          <span>{{ reports.length }} 份</span>
        </div>

        <div v-if="loading" class="state-card">
          <LoaderCircle class="spin" />
          <p>正在加载报告...</p>
        </div>

        <div v-else-if="reports.length === 0" class="state-card">
          <FileText />
          <p>暂无报告，先生成一份健康管理报告吧。</p>
        </div>

        <button
          v-for="report in reports"
          v-else
          :key="report.report_id || report.reportId"
          type="button"
          class="report-card"
          @click="openReport(report)"
        >
          <div>
            <span>{{ report.report_type_label || report.reportTypeLabel }}</span>
            <h3>{{ report.report_title || report.reportTitle }}</h3>
            <p>{{ report.report_summary || report.reportSummary || '暂无摘要' }}</p>
          </div>
          <div class="report-qr-mini">
            <img
              v-if="report.qr_code_data_url || report.qrCodeDataUrl"
              :src="report.qr_code_data_url || report.qrCodeDataUrl"
              alt="报告二维码"
            />
            <span v-else>RPT{{ String(report.report_id || report.reportId).padStart(4, '0') }}</span>
          </div>
        </button>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { ArrowLeft, FileHeart, FilePlus2, FileText, LoaderCircle, RefreshCw } from 'lucide-vue-next'
import { generateReport, listReports } from '../api'
import './report.css'

const router = useRouter()
const loading = ref(false)
const generating = ref(false)
const reports = ref([])
const form = reactive({
  report_type: 'personal',
  days: 30
})

const reportTypes = [
  { value: 'personal', label: '个人版' },
  { value: 'doctor_summary', label: '医生速览' }
]

onMounted(loadReports)

async function loadReports() {
  loading.value = true
  try {
    const response = await listReports({ page: 1, page_size: 20 })
    reports.value = response.data?.list || response.data?.records || response.data || []
  } catch (error) {
    showToast(error?.response?.data?.message || '报告列表加载失败')
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  generating.value = true
  try {
    const response = await generateReport(form)
    const report = response.data
    showToast('报告已生成')
    if (report?.report_id || report?.reportId) {
      router.push(`/app/reports/${report.report_id || report.reportId}`)
    } else {
      await loadReports()
    }
  } catch (error) {
    showToast(error?.response?.data?.message || '报告生成失败')
  } finally {
    generating.value = false
  }
}

function openReport(report) {
  const id = report.report_id || report.reportId
  if (id) router.push(`/app/reports/${id}`)
}
</script>
