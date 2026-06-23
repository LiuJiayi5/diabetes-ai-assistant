<template>
  <div class="module-entry">
    <div class="entry-panel">
      <h2>{{ isAdmin ? '管理员登录' : '患者登录' }}</h2>
      <p>所属模块：用户认证模块</p>
      <p>后续接入：Spring Boot 认证接口、JWT 生成与校验。</p>
      <p>是否涉及 Dify：否。</p>
      <p>后续可扩展：登录表单、验证码、登录状态恢复。</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isAdmin = computed(() => route.meta?.role === 'admin')
</script>
