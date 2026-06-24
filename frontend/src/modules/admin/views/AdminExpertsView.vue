<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">专家展示管理</h1>
        <p class="admin-page-desc">维护首页专家或 AI 医师展示卡片，仅作为展示入口，不创建真实医生角色。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" @click="openCreate">
        <Plus :size="16" /> 新增展示卡片
      </el-button>
    </div>

    <section class="admin-tip">
      <Info :size="16" />
      <span>本系统不设置真实医生业务角色，专家/AI 医师卡片主要作为 AI 医生咨询入口和首页展示内容。</span>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">展示卡片列表</span>
        <span class="admin-count-pill">{{ cards.length }} 条</span>
      </div>
      <el-table :data="cards" row-key="content_id" empty-text="暂无展示卡片">
        <el-table-column label="ID" width="100"><template #default="{ row }">#{{ row.content_id }}</template></el-table-column>
        <el-table-column label="图标" width="90">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.image_url"><Bot :size="18" /></el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="名称" min-width="160" />
        <el-table-column prop="subtitle" label="简介" min-width="260" show-overflow-tooltip />
        <el-table-column prop="sort_order" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 'enabled' ? 'success' : 'info'" round>{{ row.status === 'enabled' ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 'enabled' ? 'danger' : 'success'" @click="toggle(row)">{{ row.status === 'enabled' ? '停用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑展示卡片' : '新增展示卡片'" width="520px">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="form.title" placeholder="如：AI 控糖助手" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.subtitle" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="头像/图片 URL"><el-input v-model="form.image_url" /></el-form-item>
        <div class="dialog-grid">
          <el-form-item label="跳转类型">
            <el-select v-model="form.link_type">
              <el-option label="AI 咨询" value="chat" />
              <el-option label="无跳转" value="none" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sort_order" :min="0" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="admin-primary-btn" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Bot, Info, Plus } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { adminGetContentManagement, adminSaveHomeContent } from '@/api/admin'
import { adminMockHomeContents } from '@/modules/admin/mockData'

const contents = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive(defaultForm())
const cards = computed(() => contents.value.filter((item) => item.content_type === 'ai_doctor_card'))

function defaultForm() {
  return { content_type: 'ai_doctor_card', title: '', subtitle: '', image_url: '', link_type: 'chat', link_value: 'chat', sort_order: 1, status: 'enabled' }
}

async function load() {
  try {
    const response = await adminGetContentManagement()
    contents.value = response?.home_contents || response?.data?.home_contents || adminMockHomeContents
  } catch {
    contents.value = adminMockHomeContents
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, row)
  dialogVisible.value = true
}

async function save() {
  try { await adminSaveHomeContent(form) } catch {}
  if (editing.value) Object.assign(editing.value, form)
  else contents.value.push({ ...form, content_id: Date.now() })
  dialogVisible.value = false
  ElMessage.success('展示卡片已保存')
}

function toggle(row) {
  row.status = row.status === 'enabled' ? 'disabled' : 'enabled'
  adminSaveHomeContent(row).catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.admin-tip {
  margin-bottom: 16px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
