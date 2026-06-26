<template>
  <div class="article-detail-page">
    <header class="article-detail-topbar">
      <button type="button" class="article-back-button" @click="router.push('/app/articles')">
        <ArrowLeft />
      </button>
      <h1>全部资讯</h1>
      <span />
    </header>

    <main class="article-detail-scroll mobile-scroll">
      <label class="articles-search articles-search--inner">
        <Search />
        <input v-model="keyword" type="search" placeholder="搜索标题、摘要、分类或正文关键词">
      </label>

      <section class="article-list-section article-list-section--flat">
        <div class="article-list-title">
          <div>
            <h2>{{ keyword.trim() ? '搜索结果' : '全部资讯' }}</h2>
            <span>{{ listHint }}</span>
          </div>
        </div>

        <div v-if="articlesStore.loading" class="article-state-card">
          <LoaderCircle class="spin" />
          <p>正在加载健康资讯...</p>
        </div>

        <div v-else-if="!filteredArticles.length" class="article-state-card">
          <BookOpen />
          <h3>暂无相关资讯</h3>
          <p>换个关键词试试</p>
        </div>

        <template v-else>
          <ArticleCard
            v-for="article in pagedArticles"
            :key="article.article_id"
            :article="article"
            @open="openArticle"
          />

          <div v-if="pageCount > 1" class="article-pagination">
            <button type="button" :disabled="currentPage === 1" @click="currentPage -= 1">上一页</button>
            <span>{{ currentPage }} / {{ pageCount }}</span>
            <button type="button" :disabled="currentPage === pageCount" @click="currentPage += 1">下一页</button>
          </div>
        </template>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, BookOpen, LoaderCircle, Search } from 'lucide-vue-next'
import { searchArticles, useArticlesStore } from '@/stores/articles'
import ArticleCard from '../components/ArticleCard.vue'
import '../styles/articles.css'

const router = useRouter()
const articlesStore = useArticlesStore()
const keyword = ref('')
const currentPage = ref(1)
const pageSize = 10

const filteredArticles = computed(() => {
  const term = keyword.value.trim()
  return term ? searchArticles(articlesStore.articles, term) : articlesStore.articles
})

const pageCount = computed(() => Math.max(1, Math.ceil(filteredArticles.value.length / pageSize)))

const pagedArticles = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredArticles.value.slice(start, start + pageSize)
})

const listHint = computed(() => {
  const total = filteredArticles.value.length
  if (keyword.value.trim()) return `找到 ${total} 篇相关内容`
  return `共 ${total} 篇，每页 ${pageSize} 篇`
})

onMounted(async () => {
  if (!articlesStore.articles.length) {
    await articlesStore.fetchArticles()
  }
})

watch([keyword, filteredArticles], () => {
  currentPage.value = 1
})

watch(pageCount, () => {
  if (currentPage.value > pageCount.value) currentPage.value = pageCount.value
})

function openArticle(article) {
  router.push(`/app/articles/${article.article_id}`)
}
</script>
