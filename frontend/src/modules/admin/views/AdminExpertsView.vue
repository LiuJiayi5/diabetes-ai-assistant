<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">AI 医师卡片管理</h1>
        <p class="admin-page-desc">维护首页 AI 医师展示卡片，作为患者端 AI 咨询入口展示。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" @click="openCreate">
        <Plus :size="16" /> 新增展示卡片
      </el-button>
    </div>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">展示卡片列表</span>
        <span class="admin-count-pill">共 {{ cards.length }} 条</span>
      </div>
      <el-table v-loading="loading" :data="pagedCards" row-key="content_id" empty-text="暂无展示卡片">
        <el-table-column label="ID" width="82">
          <template #default="{ row }"><el-tag effect="plain">#{{ row.content_id }}</el-tag></template>
        </el-table-column>
        <el-table-column label="头像" width="82">
          <template #default="{ row }">
            <el-avatar :size="38" :src="asset(row.image_url)"><Bot :size="18" /></el-avatar>
          </template>
        </el-table-column>
        <el-table-column label="卡片信息" min-width="280">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ row.title || 'AI 控糖助手' }}</strong>
            <span class="admin-table-subtitle">{{ row.subtitle || '暂无简介' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort_order" label="排序" width="80" />
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag :type="row.status === 'enabled' ? 'success' : 'info'" round>
              {{ row.status === 'enabled' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="166" align="right">
          <template #default="{ row }">
            <span class="admin-actions">
              <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button link :type="row.status === 'enabled' ? 'danger' : 'success'" @click="toggle(row)">
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
          :total="cards.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="pagination.page = 1"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑展示卡片' : '新增展示卡片'" width="540px">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model.trim="form.title" placeholder="如：AI 控糖助手" /></el-form-item>
        <el-form-item label="简介"><el-input v-model.trim="form.subtitle" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="头像/图片">
          <ImageUploader
            v-model="form.image_url"
            title="上传展示图片"
            hint="点击选择或拖拽头像图片"
            @error="ElMessage.error"
          />
        </el-form-item>
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
import { Bot, Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminDeleteHomeContent, adminGetContentManagement, adminSaveHomeContent } from '@/api/admin'
import ImageUploader from '@/components/ImageUploader.vue'
import { createPagination, resolveAdminError, sortByStatusThenOrder, totalPages } from '@/modules/admin/utils'
import { resolveAssetUrl } from '@/utils/assets'

const contents = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive(defaultForm())
const pagination = reactive(createPagination(10))

const cards = computed(() => sortByStatusThenOrder(
  contents.value.filter((item) => item.content_type === 'ai_doctor_card')
))

const pagedCards = computed(() => {
  const maxPage = totalPages({ ...pagination, total: cards.value.length })
  if (pagination.page > maxPage) pagination.page = maxPage
  const start = (pagination.page - 1) * pagination.page_size
  return cards.value.slice(start, start + pagination.page_size)
})

function defaultForm() {
  return { content_type: 'ai_doctor_card', title: '', subtitle: '', image_url: '', link_type: 'chat', link_value: 'chat', sort_order: 1, status: 'enabled' }
}

async function load() {
  loading.value = true
  try {
    const response = await adminGetContentManagement({ page: 1, page_size: 10 })
    contents.value = response?.home_contents || response?.data?.home_contents || []
  } catch (error) {
    ElMessage.error(resolveAdminError(error, '展示卡片加载失败'))
  } finally {
    loading.value = false
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
  if (!form.title.trim()) {
    ElMessage.warning('请填写展示名称')
    return
  }
  if (!form.image_url) {
    ElMessage.warning('请先上传展示图片')
    return
  }
  try {
    const saved = await adminSaveHomeContent(form)
    if (editing.value) Object.assign(editing.value, saved || { ...form })
    else contents.value.push(saved || { ...form, content_id: Date.now() })
    dialogVisible.value = false
    ElMessage.success('展示卡片已保存')
  } catch (error) {
    ElMessage.error(resolveAdminError(error, '展示卡片保存失败'))
  }
}

function remove(row) {
  ElMessageBox.confirm(`确认删除“${row.title || '展示卡片'}”？删除后患者端不再展示。`, '确认删除展示卡片', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await adminDeleteHomeContent(row.content_id)
      contents.value = contents.value.filter((item) => item.content_id !== row.content_id)
      ElMessage.success('展示卡片已删除')
    } catch (error) {
      ElMessage.error(resolveAdminError(error, '展示卡片删除失败'))
    }
  }).catch(() => {})
}

async function toggle(row) {
  const next = row.status === 'enabled' ? 'disabled' : 'enabled'
  try {
    const saved = await adminSaveHomeContent({ ...row, status: next })
    Object.assign(row, saved || { status: next })
    ElMessage.success(next === 'enabled' ? '展示卡片已启用' : '展示卡片已停用')
  } catch (error) {
    ElMessage.error(resolveAdminError(error, '状态更新失败'))
  }
}

function asset(value) {
  return resolveAssetUrl(value)
}

onMounted(load)
</script>

<style scoped>
.dialog-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
