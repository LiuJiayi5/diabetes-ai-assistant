<template>
  <div class="article-detail-page">
    <header class="article-detail-topbar">
      <button type="button" class="article-back-button" @click="router.push('/app/articles')">
        <ArrowLeft />
      </button>
      <h1>{{ categoryMeta.title }}</h1>
      <span />
    </header>

    <main class="article-detail-scroll mobile-scroll">
      <section class="article-category-hero" :style="{ background: categoryMeta.bg }">
        <strong>{{ categoryMeta.title }}</strong>
        <p>{{ categoryMeta.subtitle }}</p>
        <div>
          <span v-for="tag in categoryMeta.tags" :key="tag">{{ tag }}</span>
        </div>
      </section>

      <section class="article-list-section article-list-section--flat">
        <div class="article-list-title">
          <div>
            <h2>{{ categoryMeta.title }}资讯</h2>
            <span>共 {{ categoryArticles.length }} 篇内容</span>
          </div>
        </div>

        <div v-if="articlesStore.loading" class="article-state-card">
          <LoaderCircle class="spin" />
          <p>正在加载健康资讯...</p>
        </div>

        <div v-else-if="!categoryArticles.length" class="article-state-card">
          <BookOpen />
          <h3>暂无该分类资讯</h3>
          <p>内容上架后会在这里展示</p>
        </div>

        <template v-else>
          <ArticleCard
            v-for="article in categoryArticles"
            :key="article.article_id"
            :article="article"
            @open="openArticle"
          />
        </template>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, BookOpen, LoaderCircle } from 'lucide-vue-next'
import { getCategoryMeta, useArticlesStore } from '@/stores/articles'
import { pushWithBack } from '@/utils/navigation'
import ArticleCard from '../components/ArticleCard.vue'
import '../styles/articles.css'

const route = useRoute()
const router = useRouter()
const articlesStore = useArticlesStore()

const categoryMeta = computed(() => getCategoryMeta(route.params.category))

const categoryArticles = computed(() => {
  return articlesStore.articles.filter((article) => article.category_code === categoryMeta.value.code)
})

onMounted(async () => {
  if (!articlesStore.articles.length) {
    await articlesStore.fetchArticles()
  }
})

function openArticle(article) {
  pushWithBack(router, `/app/articles/${article.article_id}`, `/app/articles/category/${categoryMeta.value.code}`)
}
</script>
