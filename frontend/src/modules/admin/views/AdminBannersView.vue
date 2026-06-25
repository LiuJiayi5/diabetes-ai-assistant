<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">轮播图管理</h1>
        <p class="admin-page-desc">维护患者端首页轮播展示图片、跳转链接与排序。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" @click="openCreate">
        <Plus :size="16" /> 新增轮播图
      </el-button>
    </div>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">轮播图列表</span>
        <span class="admin-count-pill">{{ banners.length }} 条</span>
      </div>
      <el-table :data="banners" row-key="content_id" empty-text="暂无轮播图">
        <el-table-column label="ID" width="100"><template #default="{ row }">#{{ row.content_id }}</template></el-table-column>
        <el-table-column label="图片" width="110">
          <template #default="{ row }">
            <div class="banner-thumb"><img v-if="row.image_url" :src="asset(row.image_url)" alt="" /><ImageIcon v-else :size="20" /></div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="跳转类型" width="110">
          <template #default="{ row }">{{ linkTypeLabel(row.link_type) }}</template>
        </el-table-column>
        <el-table-column prop="link_value" label="跳转目标" width="120" />
        <el-table-column prop="sort_order" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 'enabled' ? 'success' : 'info'" round>{{ row.status === 'enabled' ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 'enabled' ? 'danger' : 'success'" @click="toggle(row)">{{ row.status === 'enabled' ? '停用' : '启用' }}</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑轮播图' : '新增轮播图'" width="520px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="轮播图片">
          <ImageUploader v-model="form.image_url" title="上传轮播图" hint="点击选择或拖拽轮播图片，建议使用横向图片" @error="ElMessage.error" />
        </el-form-item>
        <div class="dialog-grid">
          <el-form-item label="跳转类型">
            <el-select v-model="form.link_type">
              <el-option label="无跳转" value="none" />
              <el-option label="资讯详情" value="article" />
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
        <el-button class="admin-primary-btn" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Image as ImageIcon, Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminDeleteHomeContent, adminGetContentManagement, adminSaveHomeContent } from '@/api/admin'
import ImageUploader from '@/components/ImageUploader.vue'
import { resolveAssetUrl } from '@/utils/assets'

const contents = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive(defaultForm())
const banners = computed(() => contents.value.filter((item) => item.content_type === 'banner'))

function defaultForm() {
  return { content_type: 'banner', title: '', subtitle: '', image_url: '', link_type: 'none', link_value: '', sort_order: 1, status: 'enabled' }
}

async function load() {
  try {
    const response = await adminGetContentManagement()
    contents.value = response?.home_contents || response?.data?.home_contents || []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '轮播图加载失败')
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
    ElMessage.warning('请填写轮播图标题')
    return
  }
  if (!form.image_url) {
    ElMessage.warning('请先上传轮播图片')
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
    ElMessage.success('轮播图已保存')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '轮播图保存失败')
  }
}

function remove(row) {
  ElMessageBox.confirm(`确认删除「${row.title}」？删除后患者端不再展示。`, '确认删除轮播图？', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await adminDeleteHomeContent(row.content_id)
      contents.value = contents.value.filter((item) => item.content_id !== row.content_id)
      ElMessage.success('轮播图已删除')
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || '轮播图删除失败')
    }
  }).catch(() => {})
}

function linkTypeLabel(value) {
  const map = {
    none: '无跳转',
    article: '资讯详情',
    life_plan: '生活方案',
    chat: 'AI助手'
  }
  return map[value] || '无跳转'
}

async function toggle(row) {
  const next = row.status === 'enabled' ? 'disabled' : 'enabled'
  try {
    const saved = await adminSaveHomeContent({ ...row, status: next })
    Object.assign(row, saved || { status: next })
    ElMessage.success(next === 'enabled' ? '轮播图已启用' : '轮播图已停用')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '状态更新失败')
  }
}

function asset(value) {
  return resolveAssetUrl(value)
}

onMounted(load)
</script>

<style scoped>
.banner-thumb {
  width: 72px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--admin-border-solid);
  border-radius: 8px;
  background: #f1f5ff;
  color: #cbd5e1;
  overflow: hidden;
}

.banner-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dialog-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>
