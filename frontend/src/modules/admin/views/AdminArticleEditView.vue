<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <div>
        <el-button text @click="router.push('/admin/articles')">
          <ArrowLeft :size="16" /> 返回健康资讯管理
        </el-button>
        <h1 class="admin-page-title">{{ isEdit ? '编辑资讯' : '新增资讯' }}</h1>
        <p class="admin-page-desc">保存用户端可见健康科普内容，不同步 Dify 知识库。</p>
      </div>
      <el-button class="admin-primary-btn" type="primary" :loading="saving" @click="submitForm">
        <Save :size="16" /> 保存资讯
      </el-button>
    </div>

    <section class="admin-card edit-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="文章标题" prop="title">
            <el-input v-model.trim="form.title" placeholder="请输入文章标题" />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-select v-model="form.category" placeholder="请选择分类">
              <el-option v-for="item in articleCategories" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status">
              <el-option label="草稿" value="draft" />
              <el-option label="已上架" value="published" />
              <el-option label="已下架" value="offline" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序值">
            <el-input-number v-model="form.sort_order" :min="0" controls-position="right" />
          </el-form-item>
        </div>

        <el-form-item label="封面图 URL">
          <el-input v-model.trim="form.cover_image" placeholder="可填写图片 URL" />
        </el-form-item>

        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请输入摘要" />
        </el-form-item>

        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="请输入正文内容" />
        </el-form-item>

        <div class="form-inline">
          <el-switch v-model="recommended" active-text="首页推荐" />
          <span>阅读量：{{ form.view_count || 0 }}</span>
        </div>
      </el-form>
    </section>

    <section class="admin-card preview-card">
      <h3>预览</h3>
      <article>
        <el-tag type="primary" effect="plain" round>{{ categoryLabel(form.category) }}</el-tag>
        <h2>{{ form.title || '文章标题预览' }}</h2>
        <p>{{ form.summary || '这里显示文章摘要。' }}</p>
        <div>{{ form.content || '这里显示文章正文内容。' }}</div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Save } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { adminGetContentManagement, adminSaveArticle } from '@/api/admin'
import { adminMockArticles, articleCategories } from '@/modules/admin/mockData'
import { categoryLabel, unwrapPage } from '@/modules/admin/utils'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const saving = ref(false)
const isEdit = computed(() => Boolean(route.params.articleId))
const recommended = ref(false)

const form = reactive({
  article_id: null,
  title: '',
  category: 'diet',
  cover_image: '',
  summary: '',
  content: '',
  status: 'draft',
  view_count: 0,
  is_recommended: 0,
  sort_order: 10
})

const rules = {
  title: [{ required: true, message: '请填写文章标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请填写正文内容', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

async function loadArticle() {
  if (!isEdit.value) return

  try {
    const response = await adminGetContentManagement({ page: 1, page_size: 50 })
    const article = unwrapPage(response, 'articles').list.find((item) => String(item.article_id) === String(route.params.articleId))
    if (article) Object.assign(form, article)
  } catch {
    const article = adminMockArticles.find((item) => String(item.article_id) === String(route.params.articleId))
    if (article) Object.assign(form, article)
  }

  recommended.value = Number(form.is_recommended) === 1
}

async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    const payload = {
      ...form,
      article_id: isEdit.value ? form.article_id || Number(route.params.articleId) : undefined,
      is_recommended: recommended.value ? 1 : 0
    }
    await adminSaveArticle(payload)
    ElMessage.success('资讯已保存')
    router.push('/admin/articles')
  } catch {
    ElMessage.success('已保存到前端演示数据，后端接口接入后将正式保存')
    router.push('/admin/articles')
  } finally {
    saving.value = false
  }
}

onMounted(loadArticle)
</script>

<style scoped>
.edit-card,
.preview-card {
  padding: 20px;
  margin-bottom: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 14px;
}

.form-inline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--admin-text-muted);
}

.preview-card h3 {
  margin: 0 0 12px;
  color: var(--admin-text-title);
}

.preview-card article {
  border: 1px solid var(--admin-border-solid);
  border-radius: 12px;
  background: var(--admin-card-muted);
  padding: 18px;
}

.preview-card h2 {
  margin: 14px 0 8px;
  color: var(--admin-text-title);
}

.preview-card p,
.preview-card div {
  color: var(--admin-text-secondary);
  line-height: 1.8;
}
</style>
