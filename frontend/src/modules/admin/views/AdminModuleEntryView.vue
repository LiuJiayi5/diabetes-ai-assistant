<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">{{ title }}</h1>
        <p class="admin-page-desc">{{ description }}</p>
      </div>
    </div>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">{{ listTitle }}</span>
        <span class="admin-count-pill">共 0 条</span>
      </div>
      <div class="admin-empty module-empty">
        <strong>暂无可展示记录</strong>
        <p>当前模块暂无数据，后续有业务记录后将在这里集中展示和管理。</p>
      </div>
      <div class="admin-pagination">
        <el-pagination
          :current-page="1"
          :page-size="10"
          :total="0"
          layout="total, prev, pager, next"
          background
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => route.meta?.title || '管理模块')
const listTitle = computed(() => `${title.value.replace('管理', '')}列表`)
const description = computed(() => {
  const text = title.value
  if (text.includes('健康档案')) return '集中查看患者健康档案基础信息和资料完善情况。'
  if (text.includes('健康数据')) return '查看患者日常血糖、血压、体重等健康指标记录。'
  if (text.includes('风险')) return '查看糖尿病风险评估结果和风险分层记录。'
  if (text.includes('AI')) return '查看 AI 咨询相关配置和运营记录。'
  if (text.includes('打卡')) return '查看患者生活打卡记录与行为分析结果。'
  return '集中查看和管理该模块的业务记录。'
})
</script>

<style scoped>
.module-empty {
  min-height: 220px;
}

.module-empty strong {
  display: block;
  color: var(--admin-text-title);
  font-size: 16px;
  margin-bottom: 6px;
}

.module-empty p {
  margin: 0;
  color: var(--admin-text-secondary);
  font-size: 13px;
}
</style>
