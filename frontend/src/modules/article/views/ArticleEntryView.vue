<template>
  <div class="articles-page">
    <header class="articles-topbar">
      <h1>健康资讯</h1>
      <div class="articles-topbar__actions">
        <button type="button" aria-label="搜索资讯" @click="focusSearch">
          <Search />
        </button>
        <button
          type="button"
          aria-label="我的收藏"
          :class="{ 'is-active': showFavoritesOnly }"
          @click="toggleFavoriteFilter"
        >
          <Bookmark :fill="showFavoritesOnly ? 'currentColor' : 'none'" />
        </button>
      </div>
    </header>

    <main class="articles-scroll mobile-scroll">
      <section class="articles-hero">
        <div class="articles-hero__content">
          <span>每日健康知识</span>
          <h2>控糖知识随时看</h2>
          <p>饮食、运动、习惯和糖尿病科普内容</p>
        </div>
        <div class="articles-hero__icon" aria-hidden="true">
          <svg viewBox="0 0 48 48" fill="none">
            <rect x="9" y="12" width="24" height="26" rx="5" fill="rgba(90,180,140,0.25)" />
            <path d="M16 18h15M16 24h15M16 30h10" stroke="#5BBF8A" stroke-width="3" stroke-linecap="round" />
            <circle cx="34" cy="16" r="7" fill="#9FDEB8" />
            <path d="M31 16l2 2 4-5" stroke="#FFFFFF" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M12 36c7-5 14-5 21 0" stroke="#7FC8E8" stroke-width="3" stroke-linecap="round" />
          </svg>
        </div>
      </section>

      <label class="articles-search">
        <Search />
        <input
          ref="searchInput"
          v-model.trim="keyword"
          type="search"
          placeholder="搜索饮食、运动、血糖、并发症"
          @keyup.enter="refreshArticles"
        >
      </label>

      <ArticleCategoryTabs v-model="activeCategory" :categories="articlesStore.categories" />

      <section class="featured-section">
        <h2>精选栏目</h2>
        <div class="featured-grid">
          <button
            v-for="category in featuredCategories"
            :key="category.title"
            type="button"
            class="featured-card"
            :style="{ background: category.bg }"
            @click="activeCategory = category.title"
          >
            <span class="featured-card__glow" />
            <strong>{{ category.title }}</strong>
            <span>{{ category.subtitle }}</span>
            <span class="featured-card__tags">
              <em v-for="tag in category.tags" :key="tag">{{ tag }}</em>
            </span>
          </button>
        </div>
      </section>

      <section class="article-list-section">
        <div class="article-list-title">
          <h2>{{ activeCategory === '全部' ? '全部资讯' : activeCategory }}</h2>
          <span>{{ filteredArticles.length }} 篇</span>
        </div>

        <div v-if="articlesStore.loading" class="article-state-card">
          <LoaderCircle class="spin" />
          <p>正在加载健康资讯...</p>
        </div>

        <div v-else-if="!filteredArticles.length" class="article-state-card">
          <BookOpen />
          <h3>{{ keyword ? '没有找到相关资讯' : '暂无资讯' }}</h3>
          <p>{{ keyword ? '换个关键词或分类再试试' : '健康资讯上架后会在这里展示' }}</p>
        </div>

        <template v-else>
          <p v-if="articlesStore.usingFallback" class="article-inline-note">
            后端资讯接口暂未接入，当前展示本地示例内容。
          </p>
          <ArticleCard
            v-for="article in filteredArticles"
            :key="article.article_id"
            :article="article"
            @open="openArticle"
          />
        </template>
      </section>

      <section class="article-info-note">
        <strong>健康科普说明</strong>
        <p>健康资讯为普通内容管理模块，不直接调用 Dify。科普内容仅供参考，不能替代线下诊疗。</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { Bookmark, BookOpen, LoaderCircle, Search } from 'lucide-vue-next'
import { FEATURED_CATEGORIES, useArticlesStore } from '@/stores/articles'
import ArticleCard from '../components/ArticleCard.vue'
import ArticleCategoryTabs from '../components/ArticleCategoryTabs.vue'
import '../styles/articles.css'

const router = useRouter()
const articlesStore = useArticlesStore()
const searchInput = ref(null)
const keyword = ref('')
const activeCategory = ref('全部')
const showFavoritesOnly = ref(false)
const featuredCategories = FEATURED_CATEGORIES

const filteredArticles = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return articlesStore.articles.filter((article) => {
    const matchCategory = activeCategory.value === '全部' || article.category === activeCategory.value
    const matchKeyword = !normalizedKeyword || [
      article.title,
      article.summary,
      article.category,
      ...(article.tags || [])
    ].join(' ').toLowerCase().includes(normalizedKeyword)
    const matchFavorite = !showFavoritesOnly.value || articlesStore.isFavorite(article.article_id)
    return matchCategory && matchKeyword && matchFavorite
  })
})

onMounted(async () => {
  await Promise.all([
    articlesStore.fetchCategories(),
    articlesStore.fetchArticles()
  ])
})

watch(activeCategory, () => {
  showFavoritesOnly.value = false
})

function focusSearch() {
  searchInput.value?.focus()
}

function toggleFavoriteFilter() {
  showFavoritesOnly.value = !showFavoritesOnly.value
  if (showFavoritesOnly.value && !articlesStore.favorites.length) {
    showToast('还没有收藏资讯')
  }
}

function refreshArticles() {
  articlesStore.fetchArticles({
    category: activeCategory.value === '全部' ? undefined : activeCategory.value,
    keyword: keyword.value || undefined
  })
}

function openArticle(article) {
  router.push(`/app/articles/${article.article_id}`)
}
</script>
