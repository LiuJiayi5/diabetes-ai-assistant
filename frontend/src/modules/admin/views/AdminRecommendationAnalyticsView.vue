<template>
  <div class="admin-page recommendation-admin-page">
    <div class="admin-page-header">
      <div>
        <h1 class="admin-page-title">推荐闭环看板</h1>
        <p class="admin-page-desc">记录患者端个性化推荐、阅读来源、停留时间和阅读进度，并辅助调整文章推荐位。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" :loading="loading" @click="loadDashboard">
        <RefreshCw :size="16" /> 刷新数据
      </el-button>
    </div>

    <div class="admin-stat-grid recommendation-stat-grid">
      <section v-for="stat in stats" :key="stat.label" class="admin-card admin-stat-card">
        <span class="admin-stat-icon" :style="{ background: stat.bg, color: stat.color }">
          <component :is="stat.icon" :size="18" />
        </span>
        <div class="admin-stat-value">{{ stat.value }}</div>
        <div class="admin-stat-label">{{ stat.label }}</div>
      </section>
    </div>

    <section class="admin-card recommendation-overview-card">
      <div>
        <span class="admin-label">场景分布</span>
        <div class="scenario-bars">
          <article v-for="item in scenarioStats" :key="item.scenario" class="scenario-bar">
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.recommendation_count }} 次推荐 · {{ item.read_count }} 次阅读</span>
            </div>
            <em :style="{ width: `${scenarioPercent(item)}%` }"></em>
          </article>
        </div>
      </div>
      <div>
        <span class="admin-label">阅读最多文章</span>
        <div class="top-article-list">
          <article v-for="article in topArticles" :key="article.article_id">
            <strong>{{ article.title }}</strong>
            <span>{{ article.read_count }} 次阅读 / {{ article.recommendation_count }} 次推荐</span>
          </article>
          <p v-if="!topArticles.length" class="muted">暂无阅读行为</p>
        </div>
      </div>
    </section>

    <section class="admin-card admin-filter-card">
      <div class="admin-filter-grid recommendation-filter-grid">
        <label>
          <span class="admin-label">搜索</span>
          <el-input v-model.trim="query.keyword" clearable placeholder="患者、手机号、文章或推荐理由">
            <template #prefix><Search :size="16" /></template>
          </el-input>
        </label>
        <label>
          <span class="admin-label">场景</span>
          <el-select v-model="query.scenario" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="首页推荐" value="home" />
            <el-option label="方案配套" value="life_plan" />
            <el-option label="复盘更新" value="intervention_review" />
            <el-option label="详情续读" value="article_detail" />
          </el-select>
        </label>
        <label>
          <span class="admin-label">推荐引擎</span>
          <el-select v-model="query.knowledge_enhanced" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="知识库增强" value="true" />
            <el-option label="本地规则" value="false" />
          </el-select>
        </label>
        <div class="admin-form-actions">
          <el-button class="admin-primary-btn" type="primary" @click="submitQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
      </div>
    </section>

    <section class="admin-card admin-table-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">推荐记录</span>
        <span class="admin-count-pill">共 {{ pagination.total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="recommendations" row-key="recommendation_id" empty-text="暂无推荐记录">
        <el-table-column label="患者 / 文章" min-width="280">
          <template #default="{ row }">
            <div class="admin-table-main">
              <span class="recommendation-avatar">{{ row.user?.username?.slice(0, 1) || '患' }}</span>
              <span class="admin-table-main__body">
                <strong class="admin-table-title">{{ row.article?.title }}</strong>
                <span class="admin-table-subtitle">{{ row.user?.username }} · {{ row.user?.phone || '无手机号' }}</span>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="112">
          <template #default="{ row }">
            <el-tag effect="plain" round>{{ row.scenario_label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="引擎" width="126">
          <template #default="{ row }">
            <el-tag :type="row.knowledge_enhanced ? 'success' : 'info'" round>
              {{ row.knowledge_enhanced ? '知识库增强' : '本地规则' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阅读表现" width="150">
          <template #default="{ row }">
            <div class="read-metric-cell">
              <strong>{{ row.read_count || 0 }} 次</strong>
              <span>{{ row.avg_progress_percent || 0 }}% · {{ row.avg_read_seconds || 0 }} 秒</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="推荐理由" min-width="310" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="recommendation-reason">{{ row.reason }}</span>
          </template>
        </el-table-column>
        <el-table-column label="信号" min-width="220">
          <template #default="{ row }">
            <span class="signal-chip-row">
              <el-tag v-for="signal in shortSignals(row.source_signals)" :key="signal" effect="plain" round>{{ signal }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="推荐时间" width="158" show-overflow-tooltip />
        <el-table-column label="操作" width="170" align="right">
          <template #default="{ row }">
            <span class="admin-actions">
              <el-button link type="primary" @click="openArticle(row.article)">编辑</el-button>
              <el-button link type="primary" @click="toggleArticleRecommend(row)">
                {{ Number(row.article?.is_recommended) ? '取消推荐' : '设为推荐' }}
              </el-button>
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
          @current-change="loadDashboard"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <section class="admin-card admin-table-card read-events-card">
      <div class="admin-card-title-row">
        <span class="admin-section-title">最近阅读行为</span>
        <span class="admin-count-pill">共 {{ readPagination.total }} 条</span>
      </div>
      <el-table :data="readEvents" row-key="event_id" empty-text="暂无阅读行为">
        <el-table-column label="患者" min-width="160">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ row.user?.username }}</strong>
            <span class="admin-table-subtitle">{{ row.user?.phone || '无手机号' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="文章" min-width="260">
          <template #default="{ row }">
            <strong class="admin-table-title">{{ row.article?.title }}</strong>
            <span class="admin-table-subtitle">{{ row.source_scenario_label }} · 推荐ID {{ row.recommendation_id || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="阅读进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.progress_percent || 0)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column label="停留时长" width="110">
          <template #default="{ row }">{{ row.read_seconds || 0 }} 秒</template>
        </el-table-column>
        <el-table-column prop="create_time" label="阅读时间" width="158" show-overflow-tooltip />
      </el-table>
    </section>

    <div v-if="error" class="admin-tip">
      <Info :size="16" />
      <span>{{ error }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BarChart3, BookOpenCheck, BrainCircuit, Clock3, Eye, Info, RefreshCw, Search } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { adminGetRecommendationDashboard, adminGetContentManagement, adminSaveArticle } from '@/api/admin'
import { assignPage, createPagination, pageParams, resolveAdminError, unwrapPage } from '@/modules/admin/utils'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const dashboard = ref(null)
const recommendations = ref([])
const readEvents = ref([])
const pagination = reactive(createPagination(10))
const readPagination = reactive(createPagination(10))
const query = reactive({
  keyword: '',
  scenario: '',
  knowledge_enhanced: ''
})

const overview = computed(() => dashboard.value?.overview || {})
const scenarioStats = computed(() => overview.value.scenario_stats || [])
const topArticles = computed(() => overview.value.top_articles || [])
const stats = computed(() => [
  { label: '推荐记录', value: overview.value.total_recommendations || 0, icon: BarChart3, bg: 'rgba(37,99,235,0.10)', color: '#2563EB' },
  { label: '阅读行为', value: overview.value.total_reads || 0, icon: Eye, bg: 'rgba(14,165,233,0.10)', color: '#0EA5E9' },
  { label: '平均进度', value: `${overview.value.avg_progress_percent || 0}%`, icon: BookOpenCheck, bg: 'rgba(34,197,94,0.10)', color: '#22C55E' },
  { label: '平均停留', value: `${overview.value.avg_read_seconds || 0}s`, icon: Clock3, bg: 'rgba(245,158,11,0.12)', color: '#F59E0B' },
  { label: '知识库增强', value: `${overview.value.knowledge_enhanced_rate || 0}%`, icon: BrainCircuit, bg: 'rgba(139,92,246,0.12)', color: '#8B5CF6' }
])

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const params = {
      ...pageParams(pagination),
      keyword: query.keyword,
      scenario: query.scenario
    }
    if (query.knowledge_enhanced !== '') {
      params.knowledge_enhanced = query.knowledge_enhanced
    }
    const response = await adminGetRecommendationDashboard(params)
    dashboard.value = response
    const recommendationPage = unwrapPage(response, 'recommendations')
    recommendations.value = recommendationPage.list
    assignPage(pagination, recommendationPage)
    const eventPage = unwrapPage(response, 'read_events')
    readEvents.value = eventPage.list
    assignPage(readPagination, eventPage)
  } catch (err) {
    error.value = resolveAdminError(err, '推荐闭环数据加载失败')
    recommendations.value = []
    readEvents.value = []
  } finally {
    loading.value = false
  }
}

function submitQuery() {
  pagination.page = 1
  loadDashboard()
}

function resetQuery() {
  query.keyword = ''
  query.scenario = ''
  query.knowledge_enhanced = ''
  submitQuery()
}

function handleSizeChange() {
  pagination.page = 1
  loadDashboard()
}

function scenarioPercent(item) {
  const max = Math.max(...scenarioStats.value.map((row) => Number(row.recommendation_count || 0)), 1)
  return Math.max(8, Math.round(Number(item.recommendation_count || 0) * 100 / max))
}

function shortSignals(signals = []) {
  return Array.isArray(signals) ? signals.slice(0, 3) : []
}

function openArticle(article) {
  if (!article?.article_id) return
  router.push(`/admin/articles/${article.article_id}/edit`)
}

async function toggleArticleRecommend(row) {
  const articleId = row.article?.article_id
  if (!articleId) return
  try {
    const detailResponse = await adminGetContentManagement({ page: 1, page_size: 1, keyword: row.article.title })
    const article = unwrapPage(detailResponse, 'articles').list.find((item) => Number(item.article_id) === Number(articleId))
    if (!article) {
      ElMessage.warning('未找到文章详情，无法调整推荐状态')
      return
    }
    const next = Number(article.is_recommended) ? 0 : 1
    await adminSaveArticle({ ...article, is_recommended: next })
    row.article.is_recommended = next
    ElMessage.success(next ? '已设为推荐文章' : '已取消推荐文章')
  } catch (err) {
    ElMessage.error(resolveAdminError(err, '推荐状态调整失败'))
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.recommendation-stat-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.recommendation-overview-card {
  margin-bottom: 18px;
  padding: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.scenario-bars,
.top-article-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.scenario-bar {
  position: relative;
  overflow: hidden;
  padding: 13px 14px;
  border-radius: 12px;
  background: #F8FAFF;
}

.scenario-bar div {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.scenario-bar strong,
.top-article-list strong {
  color: var(--admin-text-title);
  font-size: 13px;
}

.scenario-bar span,
.top-article-list span {
  color: var(--admin-text-muted);
  font-size: 12px;
}

.scenario-bar em {
  position: absolute;
  left: 0;
  bottom: 0;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, #5C8EF8, #7EB5FF);
}

.top-article-list article {
  padding: 12px 14px;
  border-radius: 12px;
  background: #F8FAFF;
}

.top-article-list strong,
.top-article-list span {
  display: block;
}

.top-article-list span {
  margin-top: 4px;
}

.recommendation-filter-grid {
  grid-template-columns: minmax(260px, 1fr) 170px 170px auto;
}

.recommendation-avatar {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border-radius: 11px;
  background: #EAF1FF;
  color: #3B76E6;
  font-size: 13px;
  font-weight: 800;
}

.read-metric-cell strong,
.read-metric-cell span {
  display: block;
}

.read-metric-cell strong {
  color: var(--admin-text-title);
}

.read-metric-cell span,
.recommendation-reason {
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.signal-chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.read-events-card {
  margin-top: 18px;
}

.muted {
  color: var(--admin-text-muted);
}

.admin-tip {
  margin-top: 14px;
}

@media (max-width: 1200px) {
  .recommendation-stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .recommendation-overview-card,
  .recommendation-filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
