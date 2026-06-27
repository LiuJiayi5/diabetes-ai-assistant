<template>
  <div class="admin-page">
    <div class="admin-page__header">
      <h1>档案详情 · 用户 {{ route.params.userId }}</h1>
      <button class="admin-btn admin-btn--ghost" type="button" @click="router.push('/admin/profiles')">返回列表</button>
    </div>

    <div v-if="loading" class="admin-loading">加载中...</div>
    <div v-else-if="detail" class="admin-table-wrap" style="padding: 20px">
      <div class="admin-detail-grid">
        <div class="admin-detail-item">
          <span class="admin-detail-item__label">档案ID</span>
          <span class="admin-detail-item__value">{{ detail.profile_id ?? '—' }}</span>
        </div>
        <div class="admin-detail-item">
          <span class="admin-detail-item__label">用户ID</span>
          <span class="admin-detail-item__value">{{ detail.user_id ?? '—' }}</span>
        </div>
        <div class="admin-detail-item">
          <span class="admin-detail-item__label">年龄</span>
          <span class="admin-detail-item__value">{{ detail.age ?? '—' }}</span>
        </div>
        <div class="admin-detail-item">
          <span class="admin-detail-item__label">性别</span>
          <span class="admin-detail-item__value">{{ formatGender(detail.gender) }}</span>
        </div>
        <div class="admin-detail-item">
          <span class="admin-detail-item__label">身高</span>
          <span class="admin-detail-item__value">{{ detail.height_cm != null ? `${detail.height_cm} cm` : '—' }}</span>
        </div>
        <div class="admin-detail-item">
          <span class="admin-detail-item__label">基础体重</span>
          <span class="admin-detail-item__value">{{ detail.base_weight_kg != null ? `${detail.base_weight_kg} kg` : '—' }}</span>
        </div>
        <div class="admin-detail-item">
          <span class="admin-detail-item__label">基础腰围</span>
          <span class="admin-detail-item__value">{{ detail.base_waist_cm != null ? `${detail.base_waist_cm} cm` : '—' }}</span>
        </div>
        <div class="admin-detail-item">
          <span class="admin-detail-item__label">更新时间</span>
          <span class="admin-detail-item__value">{{ formatDateTime(detail.update_time) }}</span>
        </div>
      </div>

      <div class="admin-detail-block">
        <h3>家族病史</h3>
        <p>{{ detail.family_history || '—' }}</p>
      </div>
      <div class="admin-detail-block">
        <h3>慢性病史</h3>
        <p>{{ detail.chronic_history || '—' }}</p>
      </div>
      <div class="admin-detail-block">
        <h3>过敏史</h3>
        <p>{{ detail.allergy_history || '—' }}</p>
      </div>
      <div class="admin-detail-block">
        <h3>档案摘要</h3>
        <p>{{ detail.profile_summary || '—' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { adminGetProfileDetail } from '@/api/profile'
import { assertSuccess } from '@/utils/response'
import { formatDateTime } from '@/utils/format'
import { formatGender } from '@/utils/health'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const detail = ref(null)

onMounted(loadDetail)

async function loadDetail() {
  loading.value = true
  try {
    detail.value = assertSuccess(await adminGetProfileDetail(route.params.userId))
  } catch (error) {
    showToast(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
@import '@/styles/admin-page.css';
</style>
