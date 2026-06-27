<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">健康资讯管理</h1>
        <p class="admin-page-desc">维护患者端可见的糖尿病科普文章、封面和推荐排序。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" @click="router.push('/admin/articles/create')">
        <Plus :size="16" /> 新增资讯
      </el-button>
    </div>

    <div class="admin-stat-grid">
      <section v-for="stat in stats" :key="stat.label" class="admin-card admin-stat-card">
        <span class="admin-stat-icon" :style="{ background: stat.bg, color: stat.color }">
          <component :is="stat.icon" :size="18" />
        </span>
        <div class="admin-stat-value">{{ stat.value }}</div>
        <div class="admin-stat-label">{{ stat.label }}</div>
      </section>
    </div>

    <section class="admin-card admin-filter-card">
      <div class="admin-filter-grid">
        <label>
          <span class="admin-label">搜索</span>
          <el-input v-model.trim="query.keyword" clearable placeholder="按标题、摘要搜索">
            <template #prefix><Search :size="16" /></template>
          </el-input>
        </label>
        <label>
          <span class="admin-label">分类</span>
          <el-select v-model="query.category" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option v-for="item in articleCategories" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </label>
        <label>
          <span class="admin-label">状态</span>
          <el-select v-model="query.article_status" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="草稿" value="draft" />
            <el-option label="已上架" value="published" />
            <el-option label="已下架" value="offline" />
          </el-select>
        </label>
        <span></span>
        <div class="admin-form-actions">
          <el-button class="admin-primary-btn" type="primary" @click="submitQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">资讯列表</span>
        <span class="admin-count-pill">共 {{ pagination.total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="articles" row-key="article_id" empty-text="暂无资讯">
        <el-table-column label="ID" width="82">
          <template #default="{ row }"><el-tag effect="plain">#{{ row.article_id }}</el-tag></template>
        </el-table-column>
        <el-table-column label="封面" width="90">
          <template #default="{ row }">
            <div class="cover-box">
              <img v-if="row.cover_image" :src="asset(row.cover_image)" alt="" />
              <ImageIcon v-else :size="18" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文章" min-width="260">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ row.title }}</strong>
            <span class="admin-table-subtitle">{{ row.summary || '暂无摘要' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain" round>{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="articleStatusType(row.status)" round>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="view_count" label="阅读量" width="90" />
        <el-table-column label="推荐" width="86">
          <template #default="{ row }">
            <el-tag v-if="Number(row.is_recommended)" type="warning" round>推荐</el-tag>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="update_time" label="更新时间" width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="190" align="right">
          <template #default="{ row }">
            <span class="admin-actions">
              <el-button link type="primary" @click="preview(row)">预览</el-button>
              <el-button link type="primary" @click="router.push(`/admin/articles/${row.article_id}/edit`)">编辑</el-button>
              <el-dropdown trigger="click">
                <el-button link>更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="toggleStatus(row)">
                      {{ row.status === 'published' ? '下架文章' : '上架文章' }}
                    </el-dropdown-item>
                    <el-dropdown-item @click="toggleRecommend(row)">
                      {{ Number(row.is_recommended) ? '取消推荐' : '设为推荐' }}
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="removeArticle(row)">删除文章</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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
          @current-change="loadContent"
          @size-change="handleSizeChange"
        />
      </div>

      <div v-if="error" class="admin-tip">
        <Info :size="16" />
        <span>{{ error }}</span>
      </div>
    </section>

    <el-dialog v-model="previewVisible" title="资讯预览" width="680px">
      <article v-if="currentArticle" class="article-preview">
        <div class="tag-row">
          <el-tag type="primary" effect="plain" round>{{ categoryLabel(currentArticle.category) }}</el-tag>
          <el-tag :type="articleStatusType(currentArticle.status)" round>{{ statusLabel(currentArticle.status) }}</el-tag>
          <el-tag v-if="Number(currentArticle.is_recommended)" type="warning" round>首页推荐</el-tag>
        </div>
        <h2>{{ currentArticle.title }}</h2>
        <p>{{ currentArticle.summary }}</p>
        <div>{{ currentArticle.content }}</div>
      </article>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpen, CheckCircle, FileText, Image as ImageIcon, Info, Plus, Search, Star } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminDeleteArticle, adminGetContentManagement, adminSaveArticle } from '@/api/admin'
import { articleCategories } from '@/modules/admin/constants'
import {
  assignPage,
  categoryLabel,
  createPagination,
  ensurePageAfterDelete,
  pageParams,
  resolveAdminError,
  sortByStatusThenOrder,
  statusLabel,
  unwrapPage
} from '@/modules/admin/utils'
import { resolveAssetUrl } from '@/utils/assets'

const router = useRouter()
const articles = ref([])
const statsArticles = ref([])
const loading = ref(false)
const error = ref('')
const previewVisible = ref(false)
const currentArticle = ref(null)
const pagination = reactive(createPagination(10))
const query = reactive({
  keyword: '',
  category: '',
  article_status: ''
})

const stats = computed(() => [
  { label: '资讯总数', value: pagination.total || statsArticles.value.length, icon: BookOpen, bg: 'rgba(37,99,235,0.10)', color: '#2563EB' },
  { label: '已上架', value: statsArticles.value.filter((a) => a.status === 'published').length, icon: CheckCircle, bg: 'rgba(34,197,94,0.10)', color: '#22C55E' },
  { label: '草稿', value: statsArticles.value.filter((a) => a.status === 'draft').length, icon: FileText, bg: 'rgba(148,163,184,0.15)', color: '#94A3B8' },
  { label: '首页推荐', value: statsArticles.value.filter((a) => Number(a.is_recommended)).length, icon: Star, bg: 'rgba(245,158,11,0.10)', color: '#F59E0B' }
])

async function loadContent() {
  loading.value = true
  error.value = ''
  try {
    const response = await adminGetContentManagement({ ...query, ...pageParams(pagination) })
    const page = unwrapPage(response, 'articles')
    articles.value = sortByStatusThenOrder(page.list)
    assignPage(pagination, page)
  } catch (err) {
    error.value = resolveAdminError(err, '资讯管理数据加载失败')
    articles.value = []
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const response = await adminGetContentManagement({ page: 1, page_size: 100 })
    statsArticles.value = unwrapPage(response, 'articles').list
  } catch {
    statsArticles.value = []
  }
}

function submitQuery() {
  pagination.page = 1
  loadContent()
}

function resetQuery() {
  query.keyword = ''
  query.category = ''
  query.article_status = ''
  submitQuery()
}

function handleSizeChange() {
  pagination.page = 1
  loadContent()
}

function articleStatusType(status) {
  if (status === 'published') return 'success'
  if (status === 'offline') return 'danger'
  return 'info'
}

function preview(article) {
  currentArticle.value = article
  previewVisible.value = true
}

async function saveArticlePatch(article, patch, successMessage) {
  try {
    const payload = { ...article, ...patch }
    const saved = await adminSaveArticle(payload)
    Object.assign(article, saved || payload)
    loadStats()
    ElMessage.success(successMessage)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '保存失败')
  }
}

function toggleRecommend(article) {
  const next = Number(article.is_recommended) ? 0 : 1
  saveArticlePatch(article, { is_recommended: next }, next ? '已设为首页推荐' : '已取消首页推荐')
}

function toggleStatus(article) {
  const next = article.status === 'published' ? 'offline' : 'published'
  ElMessageBox.confirm(
    next === 'published' ? '上架后用户端可以看到此文章。' : '下架后用户端将无法看到此文章。',
    `确认${next === 'published' ? '上架' : '下架'}文章？`,
    { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
  ).then(() => {
    saveArticlePatch(article, { status: next }, '文章状态已更新')
  }).catch(() => {})
}

function removeArticle(article) {
  ElMessageBox.confirm(`确认删除「${article.title}」？删除后无法恢复。`, '确认删除文章？', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(() => {
    adminDeleteArticle(article.article_id).then(() => {
      articles.value = articles.value.filter((item) => item.article_id !== article.article_id)
      ensurePageAfterDelete(pagination)
      if (!articles.value.length && pagination.page > 1) loadContent()
      loadStats()
      ElMessage.success('文章已删除')
    }).catch((error) => {
      ElMessage.error(error?.response?.data?.message || '文章删除失败')
    })
  }).catch(() => {})
}

function asset(value) {
  return resolveAssetUrl(value)
}

onMounted(() => {
  loadContent()
  loadStats()
})
</script>

<style scoped>
.cover-box {
  width: 56px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--admin-border-solid);
  border-radius: 8px;
  background: #f1f5ff;
  color: #cbd5e1;
}

.cover-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.muted {
  color: var(--admin-text-muted);
}

.article-preview h2 {
  margin: 14px 0 8px;
  color: var(--admin-text-title);
}

.article-preview p,
.article-preview div {
  color: var(--admin-text-secondary);
  line-height: 1.8;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.admin-tip {
  margin: 14px;
}
</style>
