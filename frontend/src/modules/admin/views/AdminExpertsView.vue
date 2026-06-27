<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">AI 专家身份管理</h1>
        <p class="admin-page-desc">维护患者端可选择的 AI 专家身份，每个咨询会话都会绑定其中一位专家。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" @click="openCreate">
        <Plus :size="16" /> 新增专家
      </el-button>
    </div>

    <section class="admin-card admin-filter-card">
      <div class="admin-filter-grid expert-filter-grid">
        <label class="admin-field">
          <span class="admin-label">关键词</span>
          <el-input v-model.trim="filters.keyword" placeholder="姓名、职称、科室或专长" clearable @keyup.enter="loadExperts(1)" />
        </label>
        <label class="admin-field">
          <span class="admin-label">状态</span>
          <el-select v-model="filters.status" clearable placeholder="全部状态">
            <el-option label="启用" value="enabled" />
            <el-option label="停用" value="disabled" />
          </el-select>
        </label>
        <div class="admin-form-actions">
          <el-button class="admin-primary-btn" type="primary" @click="loadExperts(1)">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">专家列表</span>
        <span class="admin-count-pill">共 {{ pagination.total }} 位</span>
      </div>
      <el-table v-loading="loading" :data="experts" row-key="expert_id" empty-text="暂无专家">
        <el-table-column label="专家" min-width="230">
          <template #default="{ row }">
            <div class="expert-cell">
              <el-avatar :size="42" :src="avatar(row.avatar_url)">
                {{ initial(row.expert_name) }}
              </el-avatar>
              <div>
                <strong>{{ row.expert_name }}</strong>
                <small>{{ row.title || 'AI 专家' }} · {{ row.department || '智能咨询' }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="specialty" label="专长" min-width="260" show-overflow-tooltip />
        <el-table-column label="会话数" width="92">
          <template #default="{ row }">{{ row.session_count || 0 }}</template>
        </el-table-column>
        <el-table-column prop="sort_order" label="排序" width="82" />
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="row.status === 'enabled' ? 'success' : 'info'" round>
              {{ row.status === 'enabled' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="178" align="right">
          <template #default="{ row }">
            <span class="admin-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link :type="row.status === 'enabled' ? 'warning' : 'success'" @click="toggle(row)">
                {{ row.status === 'enabled' ? '停用' : '启用' }}
              </el-button>
              <el-button link type="danger" @click="remove(row)">删除</el-button>
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.page_size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadExperts"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑专家' : '新增专家'" width="680px">
      <el-form label-position="top">
        <div class="dialog-grid">
          <el-form-item label="专家姓名"><el-input v-model.trim="form.expert_name" placeholder="如：林清禾" /></el-form-item>
          <el-form-item label="职称"><el-input v-model.trim="form.title" placeholder="如：主任医师" /></el-form-item>
          <el-form-item label="科室"><el-input v-model.trim="form.department" placeholder="如：内分泌代谢科" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sort_order" :min="0" /></el-form-item>
        </div>
        <el-form-item label="专家头像">
          <ImageUploader
            v-model="form.avatar_url"
            title="上传专家头像"
            hint="点击选择或拖拽头像图片，可拖动调整显示位置"
            avatar-crop
            @error="ElMessage.error"
          />
        </el-form-item>
        <el-form-item label="专长"><el-input v-model.trim="form.specialty" placeholder="用于患者端展示和Dify专家身份识别" /></el-form-item>
        <el-form-item label="专家人设">
          <el-input v-model.trim="form.persona" type="textarea" :rows="5" placeholder="告诉Dify这个专家是谁、擅长什么、回答风格是什么" />
        </el-form-item>
        <el-form-item label="开场白">
          <el-input v-model.trim="form.opening_message" type="textarea" :rows="3" placeholder="患者进入该专家会话时展示" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button label="enabled">启用</el-radio-button>
            <el-radio-button label="disabled">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="admin-primary-btn" type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from 'lucide-vue-next'
import { createAdminAiExpert, deleteAdminAiExpert, getAdminAiExperts, updateAdminAiExpert } from '@/api/aiChat'
import { assignPage, createPagination, resolveAdminError } from '@/modules/admin/utils'
import ImageUploader from '@/components/ImageUploader.vue'
import { resolveAvatarUrl } from '@/utils/assets'

const experts = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const pagination = reactive(createPagination(10))
const filters = reactive({ keyword: '', status: '' })
const form = reactive(defaultForm())

function defaultForm() {
  return {
    expert_name: '',
    title: '',
    department: '',
    avatar_url: '',
    specialty: '',
    persona: '',
    opening_message: '',
    sort_order: 0,
    status: 'enabled'
  }
}

async function loadExperts(page = pagination.page) {
  loading.value = true
  try {
    pagination.page = page
    const response = await getAdminAiExperts({
      page,
      page_size: pagination.page_size,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined
    })
    assignPage(pagination, response.data)
    experts.value = response.data?.list || []
  } catch (error) {
    ElMessage.error(resolveAdminError(error, '专家加载失败'))
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  loadExperts(1)
}

function handleSizeChange() {
  loadExperts(1)
}

function openCreate() {
  editing.value = null
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  Object.assign(form, {
    expert_name: row.expert_name || '',
    title: row.title || '',
    department: row.department || '',
    avatar_url: row.avatar_url || '',
    specialty: row.specialty || '',
    persona: row.persona || '',
    opening_message: row.opening_message || '',
    sort_order: row.sort_order || 0,
    status: row.status || 'enabled'
  })
  dialogVisible.value = true
}

async function save() {
  if (!form.expert_name.trim()) {
    ElMessage.warning('请填写专家姓名')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateAdminAiExpert(editing.value.expert_id, form)
    } else {
      await createAdminAiExpert(form)
    }
    dialogVisible.value = false
    ElMessage.success('专家已保存')
    loadExperts(editing.value ? pagination.page : 1)
  } catch (error) {
    ElMessage.error(resolveAdminError(error, '专家保存失败'))
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  try {
    await updateAdminAiExpert(row.expert_id, { ...row, status: row.status === 'enabled' ? 'disabled' : 'enabled' })
    ElMessage.success('状态已更新')
    loadExperts(pagination.page)
  } catch (error) {
    ElMessage.error(resolveAdminError(error, '状态更新失败'))
  }
}

function remove(row) {
  ElMessageBox.confirm(`确认删除或停用「${row.expert_name}」？已有会话的专家会自动停用以保留历史绑定。`, '确认操作', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteAdminAiExpert(row.expert_id)
      ElMessage.success('操作已完成')
      loadExperts(pagination.page)
    } catch (error) {
      ElMessage.error(resolveAdminError(error, '删除失败'))
    }
  }).catch(() => {})
}

function initial(name) {
  return (name || 'AI').slice(0, 1)
}

function avatar(value) {
  return resolveAvatarUrl(value)
}

onMounted(() => loadExperts(1))
</script>

<style scoped>
.expert-filter-grid {
  grid-template-columns: minmax(240px, 1fr) 180px auto;
}

.expert-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.expert-cell strong,
.expert-cell small {
  display: block;
}

.expert-cell strong {
  color: var(--admin-text-title);
  font-size: 14px;
}

.expert-cell small {
  margin-top: 4px;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

:deep(.image-uploader) {
  max-width: 360px;
}

@media (max-width: 900px) {
  .expert-filter-grid,
  .dialog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
