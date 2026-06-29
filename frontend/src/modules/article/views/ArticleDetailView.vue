<template>
  <div class="article-detail-page">
    <header class="article-detail-topbar">
      <button type="button" class="article-back-button" @click="goBack">
        <ArrowLeft />
      </button>
      <h1>资讯详情</h1>
      <button
        type="button"
        class="article-favorite-button"
        :class="{ 'is-active': isFavorite }"
        @click="toggleFavorite"
      >
        <Bookmark :fill="isFavorite ? 'currentColor' : 'none'" />
      </button>
    </header>

    <main class="article-detail-scroll mobile-scroll">
      <section v-if="articlesStore.detailLoading" class="article-state-card">
        <LoaderCircle class="spin" />
        <p>正在加载资讯详情...</p>
      </section>

      <section v-else-if="!article" class="article-state-card">
        <BookOpen />
        <h3>资讯不存在</h3>
        <p>{{ articlesStore.detailError || '请返回资讯列表重新选择' }}</p>
        <button type="button" class="article-primary-button" @click="router.push('/app/articles')">返回资讯列表</button>
      </section>

      <template v-else>
        <article class="article-detail-card">
          <div class="article-detail-cover" :style="{ background: article.bg, color: article.color }">
            <img v-if="coverUrl && !imageFailed" :src="coverUrl" alt="资讯封面" @error="imageFailed = true" />
            <BookOpen v-else />
          </div>
          <div class="article-detail-meta">
            <span class="article-tag" :style="{ background: article.tagBg, color: article.tagColor }">
              {{ article.category }}
            </span>
            <span>
              <Eye />
              {{ article.view_count || 0 }}
            </span>
            <span>{{ formatTime(article.created_at) }}</span>
          </div>
          <h2>{{ article.title }}</h2>
          <p class="article-detail-summary">{{ article.summary }}</p>
          <div class="article-detail-tags">
            <span
              v-for="tag in article.tags"
              :key="tag"
              :style="{ background: article.tagBg, color: article.tagColor }"
            >
              {{ tag }}
            </span>
          </div>
          <div class="article-detail-content">
            <section v-for="section in sections" :key="`${section.heading}-${section.body}`" class="article-detail-section">
              <h3 v-if="section.heading">{{ section.heading }}</h3>
              <p>{{ section.body }}</p>
            </section>
          </div>
        </article>

        <section v-if="articlesStore.detailError" class="article-inline-note article-inline-note--detail">
          {{ articlesStore.detailError }}
        </section>

        <section class="article-info-note">
          <strong>健康科普说明</strong>
          <p>系统提供的健康资讯仅供科普参考，不能作为诊断依据。如出现明显身体不适或指标异常，请及时线下就医。</p>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { ArrowLeft, Bookmark, BookOpen, Eye, LoaderCircle } from 'lucide-vue-next'
import { useArticlesStore } from '@/stores/articles'
import { resolveAssetUrl } from '@/utils/assets'
import { backPathFromRoute } from '@/utils/navigation'
import '../styles/articles.css'

const route = useRoute()
const router = useRouter()
const articlesStore = useArticlesStore()

const article = computed(() => articlesStore.detail)
const coverUrl = computed(() => resolveAssetUrl(article.value?.cover_image))
const imageFailed = ref(false)
const isFavorite = computed(() => article.value && articlesStore.isFavorite(article.value.article_id))
const sections = computed(() => {
  const content = article.value?.content || article.value?.summary || ''
  return String(content)
    .split(/\n+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .flatMap((paragraph) => {
      const match = paragraph.match(/^(.{2,18})[:：](.+)$/)
      if (!match) return [{ heading: '', body: paragraph }]
      return [{ heading: match[1].trim(), body: match[2].trim() }]
    })
})

onMounted(async () => {
  if (!articlesStore.articles.length) {
    await articlesStore.fetchArticles()
  }
  await articlesStore.fetchArticleDetail(route.params.articleId)
})

watch(coverUrl, () => {
  imageFailed.value = false
})

function goBack() {
  router.push(backPathFromRoute(route, '/app/articles'))
}

function toggleFavorite() {
  if (!article.value) return
  const wasFavorite = articlesStore.isFavorite(article.value.article_id)
  articlesStore.toggleFavorite(article.value.article_id)
  showToast(wasFavorite ? '已取消收藏' : '已收藏')
}

function formatTime(value) {
  if (!value) return '发布时间待同步'
  return String(value).replace('T', ' ').slice(0, 16)
}
</script>
