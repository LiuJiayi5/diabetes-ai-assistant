<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">首页内容管理</h1>
        <p class="admin-page-desc">配置患者端首页轮播、AI 医师入口和推荐内容展示。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" @click="openCreate">
        <Plus :size="16" /> 新增首页内容
      </el-button>
    </div>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">首页内容列表</span>
        <span class="admin-count-pill">{{ contents.length }} 条</span>
      </div>

      <el-table v-loading="loading" :data="contents" row-key="content_id" empty-text="暂无首页内容">
        <el-table-column label="内容ID" width="110">
          <template #default="{ row }"><el-tag effect="plain">#{{ row.content_id }}</el-tag></template>
        </el-table-column>
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <div class="content-thumb">
              <img v-if="row.image_url" :src="asset(row.image_url)" alt="" />
              <ImageIcon v-else :size="18" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="140">
          <template #default="{ row }">{{ typeLabel(row.content_type) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="subtitle" label="副标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="跳转类型" width="110">
          <template #default="{ row }">{{ linkTypeLabel(row.link_type) }}</template>
        </el-table-column>
        <el-table-column prop="link_value" label="跳转目标" width="120" />
        <el-table-column prop="sort_order" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'enabled' ? 'success' : 'info'" round>{{ row.status === 'enabled' ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 'enabled' ? 'danger' : 'success'" @click="toggle(row)">
              {{ row.status === 'enabled' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑首页内容' : '新增首页内容'" width="560px">
      <el-form :model="form" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="内容类型">
            <el-select v-model="form.content_type">
              <el-option label="轮播图" value="banner" />
              <el-option label="AI 医师展示卡片" value="ai_doctor_card" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" value="enabled" />
              <el-option label="禁用" value="disabled" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="副标题"><el-input v-model="form.subtitle" /></el-form-item>
        <el-form-item label="图片">
          <ImageUploader v-model="form.image_url" title="上传首页图片" hint="点击选择或拖拽图片，建议按内容类型选择合适比例" @error="ElMessage.error" />
        </el-form-item>
        <div class="dialog-grid">
          <el-form-item label="跳转类型">
            <el-select v-model="form.link_type">
              <el-option label="无跳转" value="none" />
              <el-option label="资讯详情" value="article" />
              <el-option label="AI 咨询" value="chat" />
              <el-option label="生活方案" value="life_plan" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序"><el-input-number v-model="form.sort_order" :min="0" /></el-form-item>
        </div>
        <el-form-item v-if="form.link_type === 'article'" label="资讯文章 ID">
          <el-input v-model.trim="form.link_value" placeholder="填写要跳转的文章 ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="admin-primary-btn" type="primary" @click="saveContent">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Image as ImageIcon, Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminDeleteHomeContent, adminGetContentManagement, adminSaveHomeContent } from '@/api/admin'
import ImageUploader from '@/components/ImageUploader.vue'
import { resolveAssetUrl } from '@/utils/assets'

const contents = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive(defaultForm())

function defaultForm() {
  return {
    content_id: undefined,
    content_type: 'banner',
    title: '',
    subtitle: '',
    image_url: '',
    link_type: 'none',
    link_value: '',
    sort_order: 1,
    status: 'enabled'
  }
}

async function loadContent() {
  loading.value = true
  try {
    const response = await adminGetContentManagement({ page: 1, page_size: 20 })
    contents.value = response?.home_contents || response?.data?.home_contents || []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '首页内容加载失败')
  } finally {
    loading.value = false
  }
}

function typeLabel(type) {
  return type === 'ai_doctor_card' ? 'AI 医师卡片' : '轮播图'
}

function linkTypeLabel(value) {
  const map = {
    none: '无跳转',
    article: '资讯详情',
    chat: 'AI 咨询',
    life_plan: '生活方案'
  }
  return map[value] || '无跳转'
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

async function saveContent() {
  if (!form.title) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!form.image_url) {
    ElMessage.warning('请先上传图片')
    return
  }
  if (form.link_type === 'article' && !form.link_value) {
    ElMessage.warning('请填写要跳转的文章 ID')
    return
  }
  try {
    const saved = await adminSaveHomeContent(form)
    if (editing.value) Object.assign(editing.value, saved || form)
    else contents.value.push(saved || { ...form, content_id: Date.now() })
    dialogVisible.value = false
    ElMessage.success('首页内容已保存')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '首页内容保存失败')
  }
}

function remove(row) {
  ElMessageBox.confirm(`确认删除「${row.title}」？删除后患者端不再展示。`, '确认删除首页内容？', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await adminDeleteHomeContent(row.content_id)
      contents.value = contents.value.filter((item) => item.content_id !== row.content_id)
      ElMessage.success('首页内容已删除')
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || '首页内容删除失败')
    }
  }).catch(() => {})
}

async function toggle(row) {
  const next = row.status === 'enabled' ? 'disabled' : 'enabled'
  try {
    const saved = await adminSaveHomeContent({ ...row, status: next })
    Object.assign(row, saved || { status: next })
    ElMessage.success(next === 'enabled' ? '已启用' : '已停用')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '状态更新失败')
  }
}

function asset(value) {
  return resolveAssetUrl(value)
}

onMounted(loadContent)
</script>

<style scoped>
.dialog-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.content-thumb {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--admin-border-solid);
  border-radius: 10px;
  background: #F3F7FB;
  color: #AABBC8;
}

.content-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
