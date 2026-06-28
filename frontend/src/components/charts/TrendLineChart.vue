<template>
  <div ref="chartRef" class="trend-line-chart" :style="{ height }"></div>
</template>

<script setup>
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  unit: { type: String, default: '' },
  color: { type: String, default: '#16A34A' },
  height: { type: String, default: '240px' },
  points: {
    type: Array,
    default: () => []
  },
  valueKey: { type: String, default: 'value' },
  timeKey: { type: String, default: 'recorded_at' }
})

const chartRef = ref(null)
let chartInstance = null

function formatLabel(value) {
  if (!value) return ''
  const text = String(value)
  return text.length >= 10 ? text.slice(5, 10) : text
}

function renderChart() {
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const labels = props.points.map((item) => formatLabel(item[props.timeKey] || item.recordedAt || item.create_time))
  const values = props.points.map((item) => Number(item[props.valueKey] ?? item.riskScore ?? item.value))

  chartInstance.setOption({
    title: props.title
      ? {
          text: props.title,
          left: 0,
          top: 0,
          textStyle: { fontSize: 14, fontWeight: 600, color: '#14532D' }
        }
      : undefined,
    grid: { left: 40, right: 16, top: props.title ? 42 : 24, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      valueFormatter: (value) => `${value}${props.unit ? ` ${props.unit}` : ''}`
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#BBF7D0' } },
      axisLabel: { color: '#64748B', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#ECFDF5' } },
      axisLabel: { color: '#64748B', fontSize: 11 }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        data: values,
        symbolSize: 8,
        lineStyle: { width: 3, color: props.color },
        itemStyle: { color: props.color },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: `${props.color}33` },
            { offset: 1, color: `${props.color}05` }
          ])
        }
      }
    ]
  })
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})

watch(
  () => props.points,
  () => renderChart(),
  { deep: true }
)
</script>

<style scoped>
.trend-line-chart {
  width: 100%;
}
</style>
