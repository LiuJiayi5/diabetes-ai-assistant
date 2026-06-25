<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <el-button text @click="router.push('/admin/life-plans')">
          <ArrowLeft :size="16" /> 返回生活方案记录
        </el-button>
        <h1 class="admin-page-title">方案生成日志</h1>
        <p class="admin-page-desc">查看方案生成状态、输入摘要和失败原因，便于管理员排查生成记录。</p>
      </div>
      <el-button v-if="plan" plain @click="router.push(`/admin/life-plans/${plan.plan_id}`)">查看方案详情</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else-if="plan">
      <section class="admin-card detail-card">
        <h3>调用概览</h3>
        <div class="overview-grid">
          <div class="admin-soft-block"><span>方案ID</span><strong>#{{ plan.plan_id }}</strong></div>
          <div class="admin-soft-block"><span>用户</span><strong>{{ plan.username || plan.user_id }}</strong></div>
          <div class="admin-soft-block"><span>调用状态</span><el-tag :type="plan.call_status === 'success' ? 'success' : 'danger'">{{ statusText }}</el-tag></div>
          <div class="admin-soft-block"><span>调用时间</span><strong>{{ plan.create_time }}</strong></div>
        </div>
      </section>

      <section v-if="plan.error_message" class="admin-tip error-tip">
        <AlertTriangle :size="18" />
        <div>
          <strong>错误原因</strong>
          <p>{{ plan.error_message }}</p>
        </div>
      </section>

      <section class="admin-card detail-card">
        <h3>调用流程时间线</h3>
        <div class="timeline">
          <div v-for="node in nodes" :key="node.name" class="timeline-item">
            <span :class="['timeline-dot', node.status]">
              <CheckCircle v-if="node.status === 'success'" :size="18" />
              <XCircle v-else-if="node.status === 'failed'" :size="18" />
              <Loader2 v-else-if="node.status === 'running'" :size="18" />
            </span>
            <div>
              <strong>{{ node.name }}</strong>
              <p>{{ node.desc }}</p>
            </div>
          </div>
        </div>
      </section>

      <section class="admin-card detail-card">
        <h3>请求摘要 input_summary</h3>
        <p class="summary-text">{{ plan.input_summary || '暂无 input_summary。' }}</p>
      </section>
    </template>

    <div v-else class="admin-card admin-empty">未找到日志记录</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AlertTriangle, ArrowLeft, CheckCircle, Loader2, XCircle } from 'lucide-vue-next'
import { adminMockLifePlans } from '@/modules/admin/mockData'
import { adminGetLifePlanDetail } from '@/api/admin'
import { unwrapPage } from '@/modules/admin/utils'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const plan = ref(null)

const statusText = computed(() => {
  if (plan.value?.call_status === 'success') return '生成成功'
  if (plan.value?.call_status === 'running') return '生成中'
  return '生成失败'
})

const nodes = computed(() => {
  const failed = plan.value?.call_status === 'failed'
  const running = plan.value?.call_status === 'running'
  return [
    { name: '开始节点', desc: '接收管理员可查看的方案生成记录。', status: 'success' },
    { name: '读取用户上下文', desc: '后端根据 Token 和数据库读取健康档案、指标与风险评估。', status: failed ? 'failed' : 'success' },
    { name: '生成生活方案', desc: '由后端智能分析服务统一生成，前端只展示生成结果。', status: failed ? 'failed' : running ? 'running' : 'success' },
    { name: '保存方案和打卡任务', desc: '写入 life_plans.plan_json 与 checkin_tasks_json。', status: failed ? 'failed' : 'success' },
    { name: '返回管理端展示', desc: '管理端仅查看记录、状态和失败原因。', status: failed ? 'failed' : 'success' }
  ]
})

async function loadPlan() {
  loading.value = true
  try {
    const response = await adminGetLifePlanDetail(route.params.planId)
    plan.value = response?.plan_id ? response : unwrapPage(response).list?.[0]
  } catch {
    plan.value = adminMockLifePlans.find((item) => String(item.plan_id) === String(route.params.planId))
  } finally {
    loading.value = false
  }
}

onMounted(loadPlan)
</script>

<style scoped>
.detail-card {
  padding: 18px 20px;
  margin-bottom: 16px;
}

.detail-card h3 {
  margin: 0 0 14px;
  color: var(--admin-text-title);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.overview-grid span {
  display: block;
  color: var(--admin-text-muted);
  font-size: 12px;
  margin-bottom: 6px;
}

.overview-grid strong {
  color: var(--admin-text-title);
  font-size: 14px;
}

.error-tip {
  margin-bottom: 16px;
  border-color: rgba(239,68,68,0.2);
  background: rgba(239,68,68,0.06);
}

.error-tip strong,
.error-tip p {
  color: var(--admin-danger-text);
  margin: 0;
}

.timeline {
  position: relative;
  display: grid;
  gap: 0;
}

.timeline-item {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 12px;
  padding-bottom: 18px;
  position: relative;
}

.timeline-item:not(:last-child)::after {
  content: "";
  position: absolute;
  left: 9px;
  top: 22px;
  bottom: 2px;
  width: 2px;
  background: var(--admin-border-solid);
}

.timeline-dot {
  position: relative;
  z-index: 1;
  color: #cbd5e1;
}

.timeline-dot.success {
  color: var(--admin-success);
}

.timeline-dot.failed {
  color: var(--admin-danger);
}

.timeline-dot.running {
  color: var(--admin-primary);
}

.timeline-item strong {
  color: var(--admin-text-title);
  font-size: 14px;
}

.timeline-item p,
.summary-text {
  margin: 4px 0 0;
  color: var(--admin-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}
</style>
