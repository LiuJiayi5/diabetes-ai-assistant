<template>
  <div class="module-entry">
    <div class="entry-panel">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
      <p>当前可先通过左侧菜单查看已开放的用户、资讯、首页内容和生活方案管理功能。</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => route.meta?.title || '管理端模块')
const description = computed(() => {
  const text = title.value
  if (text.includes('健康档案')) return '用于集中查看和维护患者健康档案信息。'
  if (text.includes('健康数据')) return '用于查看患者日常健康指标与记录。'
  if (text.includes('风险')) return '用于查看糖尿病风险评估结果与风险分层。'
  if (text.includes('AI')) return '用于管理 AI 医生咨询相关运营内容。'
  if (text.includes('打卡')) return '用于查看患者生活打卡与行为分析结果。'
  return '该功能正在完善为正式管理页面。'
})
</script>
