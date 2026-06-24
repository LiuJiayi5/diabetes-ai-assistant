<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <el-button text @click="router.push('/admin/life-plans')">
          <ArrowLeft :size="16" /> 返回生活方案记录
        </el-button>
        <h1 class="admin-page-title">生活方案详情</h1>
        <p class="admin-page-desc">查看方案内容、输入摘要、打卡任务和失败原因。管理端不重新生成方案。</p>
      </div>
      <el-button v-if="plan" plain @click="router.push(`/admin/life-plans/${plan.plan_id}/log`)">查看生成日志</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else-if="plan">
      <section class="admin-card detail-card">
        <div class="plan-title-row">
          <div>
            <h2>{{ plan.plan_title || normalized.planJson?.plan_title || '生活方案' }}</h2>
            <p>#{{ plan.plan_id }} · 用户 {{ plan.username || plan.user_id }} · {{ plan.create_time }}</p>
          </div>
          <div class="tag-row">
            <el-tag :type="riskTagType(plan.risk_level)" round>{{ plan.risk_level || '未知风险' }}</el-tag>
            <el-tag :type="plan.call_status === 'success' ? 'success' : 'danger'" round>
              {{ plan.call_status === 'success' ? '生成成功' : '生成失败' }}
            </el-tag>
          </div>
        </div>
      </section>

      <div class="admin-detail-grid">
        <section class="admin-card detail-card">
          <h3>基础信息</h3>
          <dl>
            <div class="admin-kv"><dt>用户ID</dt><dd>{{ plan.user_id }}</dd></div>
            <div class="admin-kv"><dt>方案目标</dt><dd>{{ plan.plan_goal || '-' }}</dd></div>
            <div class="admin-kv"><dt>方案状态</dt><dd>{{ plan.status || '-' }}</dd></div>
            <div class="admin-kv"><dt>更新时间</dt><dd>{{ plan.update_time || '-' }}</dd></div>
          </dl>
        </section>

        <section class="admin-card detail-card">
          <h3>输入摘要</h3>
          <p class="summary-text">{{ plan.input_summary || '暂无 input_summary。' }}</p>
          <h3 v-if="plan.error_message" class="danger-title">失败原因</h3>
          <p v-if="plan.error_message" class="summary-text danger">{{ plan.error_message }}</p>
        </section>
      </div>

      <section class="admin-card detail-card">
        <h3>方案结构化内容</h3>
        <div class="plan-block-grid">
          <div class="admin-soft-block">
            <h4><Utensils :size="16" /> 饮食计划</h4>
            <p>早餐：{{ toText(normalized.dietPlan.breakfast) }}</p>
            <p>午餐：{{ toText(normalized.dietPlan.lunch) }}</p>
            <p>晚餐：{{ toText(normalized.dietPlan.dinner) }}</p>
            <p>加餐：{{ toText(normalized.dietPlan.snack) }}</p>
          </div>
          <div class="admin-soft-block">
            <h4><Dumbbell :size="16" /> 运动计划</h4>
            <p>类型：{{ toText(normalized.exercisePlan.exercise_type) }}</p>
            <p>频率：{{ toText(normalized.exercisePlan.frequency) }}</p>
            <p>时长：{{ toText(normalized.exercisePlan.duration) }}</p>
            <p>强度：{{ toText(normalized.exercisePlan.intensity) }}</p>
          </div>
          <div class="admin-soft-block">
            <h4><Lightbulb :size="16" /> 健康提示</h4>
            <p v-for="tip in normalized.healthTips" :key="tip">· {{ tip }}</p>
            <p v-if="!normalized.healthTips.length">暂无健康提示</p>
          </div>
          <div class="admin-soft-block">
            <h4><ClipboardCheck :size="16" /> 打卡任务</h4>
            <p v-for="task in normalized.checkinTasks" :key="task.task_name || task.task">
              · {{ task.task_name || task.task }} {{ task.description ? `：${task.description}` : '' }}
            </p>
            <p v-if="!normalized.checkinTasks.length">暂无打卡任务</p>
          </div>
        </div>
      </section>

      <section class="admin-card detail-card">
        <h3>原始 JSON</h3>
        <pre class="admin-code-box">{{ rawJson }}</pre>
      </section>
    </template>

    <div v-else class="admin-card admin-empty">未找到方案记录</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ClipboardCheck, Dumbbell, Lightbulb, Utensils } from 'lucide-vue-next'
import { adminGetLifePlanDetail, adminListLifePlans } from '@/api/admin'
import { adminMockLifePlans } from '@/modules/admin/mockData'
import { safeJsonParse, toText as baseToText, unwrapPage } from '@/modules/admin/utils'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const plan = ref(null)

const normalized = computed(() => {
  const planJson = safeJsonParse(plan.value?.plan_json, plan.value?.plan_json || {})
  const tasks = safeJsonParse(plan.value?.checkin_tasks_json, planJson?.checkin_tasks || [])
  return {
    planJson: planJson || {},
    dietPlan: planJson?.diet_plan || {},
    exercisePlan: planJson?.exercise_plan || {},
    healthTips: Array.isArray(planJson?.health_tips) ? planJson.health_tips : [],
    checkinTasks: Array.isArray(tasks) ? tasks : []
  }
})

const rawJson = computed(() => JSON.stringify({
  plan_json: normalized.value.planJson,
  checkin_tasks_json: normalized.value.checkinTasks
}, null, 2))

async function loadPlan() {
  loading.value = true
  try {
    const response = await adminGetLifePlanDetail(route.params.planId)
    const data = response?.plan_id ? response : unwrapPage(response).list?.[0]
    plan.value = data
  } catch {
    const response = await Promise.resolve({ list: adminMockLifePlans })
    plan.value = response.list.find((item) => String(item.plan_id) === String(route.params.planId))
    if (!plan.value) {
      try {
        const listResponse = await adminListLifePlans({ page: 1, page_size: 50 })
        plan.value = unwrapPage(listResponse).list.find((item) => String(item.plan_id) === String(route.params.planId))
      } catch {}
    }
  } finally {
    loading.value = false
  }
}

function riskTagType(level) {
  if (level === '高风险') return 'danger'
  if (level === '中风险') return 'warning'
  if (level === '低风险') return 'success'
  return 'info'
}

function toText(value) {
  return baseToText(value, '暂无')
}

onMounted(loadPlan)
</script>

<style scoped>
.detail-card {
  padding: 18px 20px;
  margin-bottom: 16px;
}

.plan-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.plan-title-row h2 {
  margin: 0 0 4px;
  color: var(--admin-text-title);
}

.plan-title-row p,
.summary-text {
  margin: 0;
  color: var(--admin-text-secondary);
  line-height: 1.7;
}

.tag-row {
  display: flex;
  gap: 8px;
}

.detail-card h3 {
  margin: 0 0 12px;
  color: var(--admin-text-title);
  font-size: 16px;
}

.danger-title {
  margin-top: 16px !important;
  color: var(--admin-danger-text) !important;
}

.danger {
  color: var(--admin-danger-text);
}

.plan-block-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.admin-soft-block h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 10px;
  color: var(--admin-text-title);
}

.admin-soft-block p {
  margin: 6px 0;
  color: var(--admin-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}
</style>
