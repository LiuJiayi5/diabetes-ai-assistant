<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">轮播图管理</h1>
        <p class="admin-page-desc">维护患者端首页与资讯页轮播展示图片、跳转目标和启用状态。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" @click="openCreate">
        <Plus :size="16" /> 新增轮播图
      </el-button>
    </div>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">轮播图列表</span>
        <span class="admin-count-pill">共 {{ banners.length }} 条</span>
      </div>

      <el-table v-loading="loading" :data="pagedBanners" row-key="content_id" empty-text="暂无轮播图">
        <el-table-column label="ID" width="82">
          <template #default="{ row }"><el-tag effect="plain">#{{ row.content_id }}</el-tag></template>
        </el-table-column>
        <el-table-column label="图片" width="118">
          <template #default="{ row }">
            <div class="banner-thumb">
              <img v-if="row.image_url" :src="asset(row.image_url)" alt="" />
              <ImageIcon v-else :size="20" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="轮播内容" min-width="260">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ row.title || '未命名轮播图' }}</strong>
            <span class="admin-table-subtitle">{{ row.subtitle || '暂无副标题' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="跳转" width="150">
          <template #default="{ row }">
            <span>{{ linkTypeLabel(row.link_type) }}</span>
            <span class="admin-table-subtitle">{{ row.link_value || '无' }}</span>
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
          :total="banners.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="pagination.page = 1"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑轮播图' : '新增轮播图'" width="560px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model.trim="form.title" /></el-form-item>
        <el-form-item label="副标题"><el-input v-model.trim="form.subtitle" /></el-form-item>
        <el-form-item label="轮播图片">
          <ImageUploader
            v-model="form.image_url"
            title="上传轮播图"
            hint="点击选择或拖拽轮播图片，建议使用横向图片"
            @error="ElMessage.error"
          />
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
import { createPagination, resolveAdminError, totalPages } from '@/modules/admin/utils'
import { resolveAssetUrl } from '@/utils/assets'

const contents = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(null)
const form = reactive(defaultForm())
const pagination = reactive(createPagination(10))

const banners = computed(() => contents.value
  .filter((item) => item.content_type === 'banner')
  .sort((a, b) => Number(a.sort_order || 0) - Number(b.sort_order || 0)))

const pagedBanners = computed(() => {
  if (pagination.page > totalPages({ ...pagination, total: banners.value.length })) {
    pagination.page = totalPages({ ...pagination, total: banners.value.length })
  }
  const start = (pagination.page - 1) * pagination.page_size
  return banners.value.slice(start, start + pagination.page_size)
})

function defaultForm() {
  return { content_type: 'banner', title: '', subtitle: '', image_url: '', link_type: 'none', link_value: '', sort_order: 1, status: 'enabled' }
}

async function load() {
  loading.value = true
  try {
    const response = await adminGetContentManagement({ page: 1, page_size: 10 })
    contents.value = response?.home_contents || response?.data?.home_contents || []
  } catch (error) {
    ElMessage.error(resolveAdminError(error, '轮播图加载失败'))
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
    if (editing.value) Object.assign(editing.value, saved || { ...form })
    else contents.value.push(saved || { ...form, content_id: Date.now() })
    dialogVisible.value = false
    ElMessage.success('轮播图已保存')
  } catch (error) {
    ElMessage.error(resolveAdminError(error, '轮播图保存失败'))
  }
}

function remove(row) {
  ElMessageBox.confirm(`确认删除“${row.title || '未命名轮播图'}”？删除后患者端不再展示。`, '确认删除轮播图', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await adminDeleteHomeContent(row.content_id)
      contents.value = contents.value.filter((item) => item.content_id !== row.content_id)
      const maxPage = totalPages({ ...pagination, total: banners.value.length })
      if (pagination.page > maxPage) pagination.page = maxPage
      ElMessage.success('轮播图已删除')
    } catch (error) {
      ElMessage.error(resolveAdminError(error, '轮播图删除失败'))
    }
  }).catch(() => {})
}

async function toggle(row) {
  const next = row.status === 'enabled' ? 'disabled' : 'enabled'
  try {
    const saved = await adminSaveHomeContent({ ...row, status: next })
    Object.assign(row, saved || { status: next })
    ElMessage.success(next === 'enabled' ? '轮播图已启用' : '轮播图已停用')
  } catch (error) {
    ElMessage.error(resolveAdminError(error, '状态更新失败'))
  }
}

function linkTypeLabel(value) {
  const map = {
    none: '无跳转',
    article: '资讯详情',
    life_plan: '生活方案',
    chat: 'AI 咨询'
  }
  return map[value] || '无跳转'
}

function asset(value) {
  return resolveAssetUrl(value)
}

onMounted(load)
</script>

<style scoped>
.banner-thumb {
  width: 82px;
  height: 46px;
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
