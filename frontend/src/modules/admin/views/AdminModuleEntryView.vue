<template>
  <div class="module-entry">
    <div class="entry-panel">
      <h2>{{ title }}</h2>
      <p>所属模块：管理端模块入口</p>
      <p>后续接入：{{ backend }}</p>
      <p>是否涉及 Dify：{{ ai || '否，普通业务由 Spring Boot + MySQL 实现。' }}</p>
      <p>后续可扩展：表格、筛选、详情、状态管理。</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => route.meta?.title || '管理端模块')
const backend = computed(() => route.meta?.backend || '待配置 Spring Boot 接口')
const ai = computed(() => route.meta?.ai)
</script>
