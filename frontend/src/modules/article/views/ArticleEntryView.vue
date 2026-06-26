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
      <section class="article-carousel" aria-label="健康资讯轮播">
        <button type="button" class="article-carousel__nav article-carousel__nav--prev" @click="prevBanner">
          <ChevronLeft />
        </button>
        <button type="button" class="article-carousel__slide" @click="openBanner(activeBanner)">
          <img v-if="activeBannerImage && !bannerImageFailed" :src="activeBannerImage" alt="" @error="bannerImageFailed = true" />
          <div class="article-carousel__overlay">
            <h2>{{ activeBanner.title }}</h2>
            <p>{{ activeBanner.subtitle }}</p>
          </div>
        </button>
        <button type="button" class="article-carousel__nav article-carousel__nav--next" @click="nextBanner">
          <ChevronRight />
        </button>
        <div class="article-carousel__dots">
          <button
            v-for="(banner, index) in displayBanners"
            :key="banner.content_id || index"
            type="button"
            :class="{ 'is-active': index === activeBannerIndex }"
            :aria-label="`切换到第 ${index + 1} 张轮播图`"
            @click="activeBannerIndex = index"
          />
        </div>
      </section>

      <label class="articles-search">
        <Search />
        <input
          ref="searchInput"
          v-model="keyword"
          type="search"
          placeholder="搜索饮食、运动、血糖、并发症"
        >
      </label>

      <section class="featured-section">
        <h2>精选栏目</h2>
        <div class="featured-grid">
          <button
            v-for="category in featuredCategories"
            :key="category.code"
            type="button"
            class="featured-card"
            :style="{ background: category.bg }"
            @click="openCategory(category)"
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
          <div>
            <h2>{{ keyword.trim() || showFavoritesOnly ? '搜索结果' : '全部资讯' }}</h2>
            <span>{{ listHint }}</span>
          </div>
          <button
            v-if="!keyword.trim() && !showFavoritesOnly"
            type="button"
            class="article-more-button"
            @click="router.push('/app/articles/all')"
          >
            更多
          </button>
        </div>

        <div v-if="articlesStore.loading" class="article-state-card">
          <LoaderCircle class="spin" />
          <p>正在加载健康资讯...</p>
        </div>

        <div v-else-if="!displayArticles.length" class="article-state-card">
          <BookOpen />
          <h3>{{ keyword.trim() ? '暂无相关资讯' : '暂无资讯' }}</h3>
          <p>{{ keyword.trim() ? '换个关键词试试' : '健康资讯上架后会在这里展示' }}</p>
        </div>

        <template v-else>
          <ArticleCard
            v-for="article in displayArticles"
            :key="article.article_id"
            :article="article"
            @open="openArticle"
          />
        </template>
      </section>

      <section class="article-info-note">
        <strong>健康科普说明</strong>
        <p>系统提供的健康资讯仅供科普参考，不能作为诊断依据。如出现明显身体不适或指标异常，请及时线下就医。</p>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { Bookmark, BookOpen, ChevronLeft, ChevronRight, LoaderCircle, Search } from 'lucide-vue-next'
import { FEATURED_CATEGORIES, sampleArticlesByCategory, searchArticles, useArticlesStore } from '@/stores/articles'
import { resolveAssetUrl } from '@/utils/assets'
import ArticleCard from '../components/ArticleCard.vue'
import '../styles/articles.css'

const router = useRouter()
const articlesStore = useArticlesStore()
const searchInput = ref(null)
const keyword = ref('')
const showFavoritesOnly = ref(false)
const featuredCategories = FEATURED_CATEGORIES
const activeBannerIndex = ref(0)
const bannerImageFailed = ref(false)
let carouselTimer = null

const fallbackBanners = computed(() => {
  return articlesStore.recommendedArticles.slice(0, 3).map((article, index) => ({
    content_id: `article-${article.article_id}`,
    title: article.title,
    subtitle: article.summary,
    image_url: article.cover_image,
    link_type: 'article',
    link_value: article.article_id,
    sort_order: index + 1
  }))
})

const displayBanners = computed(() => {
  const banners = articlesStore.banners.length ? articlesStore.banners : fallbackBanners.value
  return banners.slice(0, 5)
})

const activeBanner = computed(() => {
  return displayBanners.value[activeBannerIndex.value] || {
    title: '控糖知识随时看',
    subtitle: '饮食、运动、习惯和糖尿病科普内容',
    image_url: ''
  }
})

const activeBannerImage = computed(() => resolveAssetUrl(activeBanner.value?.image_url))

const baseArticles = computed(() => {
  if (!showFavoritesOnly.value) return articlesStore.articles
  return articlesStore.articles.filter((article) => articlesStore.isFavorite(article.article_id))
})

const displayArticles = computed(() => {
  const term = keyword.value.trim()
  if (term) return searchArticles(baseArticles.value, term)
  return sampleArticlesByCategory(baseArticles.value, 10)
})

const listHint = computed(() => {
  const term = keyword.value.trim()
  if (term) return `找到 ${displayArticles.value.length} 篇相关内容`
  if (showFavoritesOnly.value) return `${displayArticles.value.length} 篇收藏`
  return '展示精选样例，更多内容可进入完整列表'
})

onMounted(async () => {
  await Promise.all([
    articlesStore.fetchCategories(),
    articlesStore.fetchArticles(),
    articlesStore.fetchHomeContents()
  ])
  startCarousel()
})

onBeforeUnmount(stopCarousel)

watch(displayBanners, () => {
  activeBannerIndex.value = 0
  startCarousel()
})

watch(activeBannerImage, () => {
  bannerImageFailed.value = false
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

function openCategory(category) {
  router.push(`/app/articles/category/${category.code}`)
}

function openArticle(article) {
  router.push(`/app/articles/${article.article_id}`)
}

function startCarousel() {
  stopCarousel()
  if (displayBanners.value.length <= 1) return
  carouselTimer = window.setInterval(nextBanner, 4200)
}

function stopCarousel() {
  if (carouselTimer) {
    window.clearInterval(carouselTimer)
    carouselTimer = null
  }
}

function nextBanner() {
  if (!displayBanners.value.length) return
  activeBannerIndex.value = (activeBannerIndex.value + 1) % displayBanners.value.length
}

function prevBanner() {
  if (!displayBanners.value.length) return
  activeBannerIndex.value = (activeBannerIndex.value - 1 + displayBanners.value.length) % displayBanners.value.length
}

function openBanner(banner) {
  if (!banner) return
  if (banner.link_type === 'article' && banner.link_value) {
    router.push(`/app/articles/${banner.link_value}`)
    return
  }
  if (banner.link_type === 'life_plan') {
    router.push('/app/life-plan')
    return
  }
  router.push('/app/articles')
}
</script>
