<template>
  <section class="home-page">
    <header class="home-topbar">
      <div class="home-brand">
        <div class="home-brand__mark">
          <Leaf class="home-brand__leaf" />
          <span class="home-brand__drop">
            <Droplet />
          </span>
        </div>
        <span>糖尿病预治助手</span>
      </div>

    </header>

    <main class="home-scroll mobile-scroll">
      <section class="home-content-carousel" aria-label="首页内容入口">
        <button
          v-if="displayHomeBanners.length > 1"
          type="button"
          class="home-content-carousel__nav home-content-carousel__nav--prev"
          @click.stop="prevHomeBanner"
        >
          <ChevronLeft />
        </button>
        <button type="button" class="home-content-carousel__slide" @click="openHomeBanner(activeHomeBanner)">
          <img
            v-if="activeHomeBannerImage && !homeBannerImageFailed"
            :src="activeHomeBannerImage"
            alt=""
            @error="homeBannerImageFailed = true"
          />
          <span v-else class="home-content-carousel__fallback" aria-hidden="true">
            <Leaf />
          </span>
          <div class="home-content-carousel__overlay">
            <h1>{{ activeHomeBanner.title }}</h1>
            <p>{{ activeHomeBanner.subtitle }}</p>
          </div>
        </button>
        <button
          v-if="displayHomeBanners.length > 1"
          type="button"
          class="home-content-carousel__nav home-content-carousel__nav--next"
          @click.stop="nextHomeBanner"
        >
          <ChevronRight />
        </button>
        <div v-if="displayHomeBanners.length > 1" class="home-content-carousel__dots">
          <button
            v-for="(banner, index) in displayHomeBanners"
            :key="banner.content_id || index"
            type="button"
            :class="{ 'is-active': index === activeHomeBannerIndex }"
            :aria-label="`切换到第 ${index + 1} 条首页内容`"
            @click.stop="activeHomeBannerIndex = index"
          />
        </div>
      </section>

      <section class="plan-card">
        <div class="plan-card__main">
          <div class="plan-card__title">
            <span class="plan-card__icon">
              <Leaf />
            </span>
            <strong>今日生活方案</strong>
            <em>已生成</em>
          </div>
          <p>饮食、运动、作息建议已为你准备好</p>
        </div>
        <button type="button" @click="router.push('/app/life-plan')">查看方案</button>
      </section>

      <section class="home-entry-grid">
        <button type="button" class="home-entry-card home-entry-card--checkin" @click="router.push('/app/checkin')">
          <span class="home-entry-card__pattern" aria-hidden="true" />
          <span class="home-entry-card__icon">
            <CalendarCheck />
          </span>
          <span class="home-entry-card__content">
            <strong>今日健康打卡</strong>
            <small>记录饮食、运动和完成情况</small>
          </span>
          <em>去打卡</em>
        </button>

        <button type="button" class="home-entry-card home-entry-card--risk" @click="router.push('/app/risk')">
          <span class="home-entry-card__pattern" aria-hidden="true" />
          <span class="home-entry-card__icon">
            <ShieldCheck />
          </span>
          <span class="home-entry-card__content">
            <strong>风险预测</strong>
            <small>结合近期数据评估健康风险</small>
          </span>
          <em>去评估</em>
        </button>
      </section>

      <section class="home-section">
        <div class="home-section__head">
          <h2>专业医师团队</h2>
          <button type="button" @click="router.push('/app/ai-chat')">
            查看全部
            <ArrowRight />
          </button>
        </div>
        <div class="doctor-strip mobile-scroll">
          <article v-for="doctor in doctors" :key="doctor.id || doctor.name" class="doctor-card">
            <div
              class="doctor-card__avatar"
              :style="{ background: `linear-gradient(135deg, ${doctor.color}, ${doctor.color}cc)`, color: doctor.textColor || '#FFFFFF' }"
            >
              <img v-if="doctor.avatar" :src="doctor.avatar" alt="" />
              <span v-else>{{ doctor.initial }}</span>
            </div>
            <p>{{ doctor.title }}</p>
            <strong>{{ doctor.name }}</strong>
            <span>{{ doctor.dept }}</span>
            <button type="button" @click="openDoctor(doctor)">立即咨询</button>
          </article>
        </div>
      </section>

      <section class="home-section">
        <div class="home-section__head">
          <h2>健康科普</h2>
          <button type="button" @click="router.push('/app/articles')">
            更多
            <ArrowRight />
          </button>
        </div>
        <div class="article-list">
          <button
            v-for="article in articles"
            :key="article.title"
            type="button"
            class="article-card"
            @click="openArticle(article)"
          >
            <span class="article-card__icon" :style="{ background: article.color }">
              <component :is="article.icon" />
            </span>
            <span class="article-card__body">
              <strong>{{ article.title }}</strong>
              <span>{{ article.summary }}</span>
              <em>
                <Eye />
                {{ article.views }} 浏览
              </em>
            </span>
          </button>
        </div>
      </section>

      <section class="home-disclaimer">
        AI 建议仅供参考，不能替代线下诊疗
      </section>
    </main>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  ArrowRight,
  BookOpen,
  CalendarCheck,
  ChevronLeft,
  ChevronRight,
  Droplet,
  Eye,
  Leaf,
  ShieldCheck
} from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { getAiExperts } from '@/api/aiChat'
import { useArticlesStore } from '@/stores/articles'
import { resolveAssetUrl, resolveAvatarUrl } from '@/utils/assets'

const router = useRouter()
const articlesStore = useArticlesStore()
const experts = ref([])
const activeHomeBannerIndex = ref(0)
const homeBannerImageFailed = ref(false)
let homeCarouselTimer = null

const fallbackDoctors = [
  { name: '赵晓峰', title: '主任医师', dept: '内分泌科', color: '#6FCF97', initial: '赵' },
  { name: '孙雅琴', title: '副主任医师', dept: '内分泌科', color: '#4FB783', initial: '孙' },
  { name: '周伟', title: '主治医师', dept: '内分泌科', color: '#BEEAF2', textColor: '#4FB783', initial: '周' }
]

const doctorPalette = ['#6FCF97', '#4FB783', '#BEEAF2']
const doctors = computed(() => {
  if (!experts.value.length) return fallbackDoctors
  return experts.value.slice(0, 3).map((expert, index) => ({
    id: expert.expert_id,
    name: expert.expert_name,
    title: expert.title,
    dept: expert.department,
    color: doctorPalette[index % doctorPalette.length],
    textColor: index % doctorPalette.length === 2 ? '#4FB783' : '#FFFFFF',
    initial: String(expert.expert_name || 'AI').slice(0, 1),
    avatar: expert.avatar_url ? resolveAvatarUrl(expert.avatar_url) : ''
  }))
})

const fallbackHomeBanners = [
  {
    content_id: 'home-life-plan',
    title: '稳稳控糖，从今天开始',
    subtitle: '查看今日方案、打卡记录和个性化控糖建议',
    image_url: '',
    link_type: 'life_plan',
    link_value: ''
  },
  {
    content_id: 'home-checkin',
    title: '记录今天，读懂变化',
    subtitle: '饮食、运动、用药和感受都可以在这里沉淀',
    image_url: '',
    link_type: 'checkin',
    link_value: ''
  }
]

const displayHomeBanners = computed(() => {
  const banners = articlesStore.banners.length ? articlesStore.banners : fallbackHomeBanners
  return banners.slice(0, 5)
})

const activeHomeBanner = computed(() => {
  return displayHomeBanners.value[activeHomeBannerIndex.value] || fallbackHomeBanners[0]
})

const activeHomeBannerImage = computed(() => resolveAssetUrl(activeHomeBanner.value?.image_url))

const articles = [
  {
    title: '糖尿病的早期症状及预防措施',
    summary: '糖尿病在早期可能出现多饮、多食、多尿等明显症状...',
    views: 156,
    color: '#DDF7E9',
    icon: BookOpen,
    articleId: 4
  },
  {
    title: '控糖饮食的三个关键原则',
    summary: '合理控制碳水摄入，搭配优质蛋白和蔬菜，帮助稳定血糖水平...',
    views: 120,
    color: '#BEEAF2',
    icon: Leaf,
    articleId: 1
  }
]

function openArticle(article) {
  router.push(`/app/articles/${article.articleId}`)
}

function openDoctor(doctor) {
  if (doctor.id) {
    router.push(`/app/ai-chat/chat?expert_id=${doctor.id}`)
    return
  }
  router.push('/app/ai-chat')
}

async function loadExperts() {
  try {
    const response = await getAiExperts()
    experts.value = response.data || []
  } catch {
    experts.value = []
  }
}

function openHomeBanner(banner) {
  if (!banner) return
  if (banner.link_type === 'article' && banner.link_value) {
    router.push(`/app/articles/${banner.link_value}`)
    return
  }
  if (banner.link_type === 'life_plan') {
    router.push('/app/life-plan')
    return
  }
  if (banner.link_type === 'ai_doctor' || banner.link_type === 'ai_chat') {
    router.push('/app/ai-chat')
    return
  }
  if (banner.link_type === 'checkin') {
    router.push('/app/checkin')
    return
  }
  router.push('/app/articles')
}

function startHomeCarousel() {
  stopHomeCarousel()
  if (displayHomeBanners.value.length <= 1) return
  homeCarouselTimer = window.setInterval(nextHomeBanner, 4200)
}

function stopHomeCarousel() {
  if (homeCarouselTimer) {
    window.clearInterval(homeCarouselTimer)
    homeCarouselTimer = null
  }
}

function nextHomeBanner() {
  if (!displayHomeBanners.value.length) return
  activeHomeBannerIndex.value = (activeHomeBannerIndex.value + 1) % displayHomeBanners.value.length
}

function prevHomeBanner() {
  if (!displayHomeBanners.value.length) return
  activeHomeBannerIndex.value = (activeHomeBannerIndex.value - 1 + displayHomeBanners.value.length) % displayHomeBanners.value.length
}

onMounted(async () => {
  await Promise.all([
    loadExperts(),
    articlesStore.fetchHomeContents()
  ])
  startHomeCarousel()
})

onBeforeUnmount(stopHomeCarousel)

watch(displayHomeBanners, () => {
  activeHomeBannerIndex.value = 0
  startHomeCarousel()
})

watch(activeHomeBannerImage, () => {
  homeBannerImageFailed.value = false
})
</script>

<style scoped>
.home-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: #F4FAF7;
}

.home-topbar {
  flex-shrink: 0;
  padding: 16px 20px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #FFFFFF;
}

.home-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1F2937;
  font-size: 15px;
  font-weight: 600;
}

.home-brand__mark {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: #DDF7E9;
}

.home-brand__leaf {
  width: 20px;
  height: 20px;
  color: #6FCF97;
  stroke-width: 2;
}

.home-brand__drop {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #4FB783;
}

.home-brand__drop svg {
  width: 8px;
  height: 8px;
  color: #FFFFFF;
  fill: #FFFFFF;
}

.home-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px 16px;
}

.home-content-carousel {
  position: relative;
  overflow: hidden;
  height: 156px;
  border-radius: 24px;
  background: linear-gradient(135deg, #6FCF97 0%, #4FB783 52%, #BEEAF2 100%);
  box-shadow: 0 8px 24px rgba(111, 207, 151, 0.18);
}

.home-content-carousel__slide {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  overflow: hidden;
  padding: 0;
  border: 0;
  border-radius: inherit;
  text-align: left;
  background: linear-gradient(135deg, #6FCF97 0%, #4FB783 52%, #BEEAF2 100%);
}

.home-content-carousel__slide img {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scale(1.06);
  transform-origin: center;
}

.home-content-carousel__fallback {
  position: absolute;
  right: 18px;
  top: 16px;
  width: 76px;
  height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.78);
}

.home-content-carousel__fallback svg {
  width: 38px;
  height: 38px;
}

.home-content-carousel__overlay {
  position: absolute;
  inset: 0;
  padding: 18px 48px 24px 22px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background: linear-gradient(180deg, rgba(255,255,255,0.02) 0%, rgba(17,64,53,0.38) 100%);
  color: #FFFFFF;
}

.home-content-carousel__overlay h1 {
  display: -webkit-box;
  max-width: 238px;
  margin: 0 0 7px;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.25;
  text-shadow: 0 2px 10px rgba(36,50,61,0.22);
}

.home-content-carousel__overlay p {
  display: -webkit-box;
  max-width: 238px;
  margin: 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: rgba(255, 255, 255, 0.88);
  font-size: 12px;
  line-height: 1.45;
}

.home-content-carousel__nav {
  position: absolute;
  z-index: 3;
  top: 50%;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(255,255,255,0.76);
  color: #4A7A62;
  transform: translateY(-50%);
  box-shadow: 0 4px 12px rgba(36,50,61,0.12);
}

.home-content-carousel__nav svg {
  width: 17px;
  height: 17px;
}

.home-content-carousel__nav--prev {
  left: 10px;
}

.home-content-carousel__nav--next {
  right: 10px;
}

.home-content-carousel__dots {
  position: absolute;
  z-index: 4;
  right: 18px;
  bottom: 11px;
  display: flex;
  gap: 6px;
}

.home-content-carousel__dots button {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: rgba(255,255,255,0.55);
}

.home-content-carousel__dots button.is-active {
  width: 18px;
  background: #FFFFFF;
}

.plan-card {
  margin-top: 16px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #DDF7E9;
  border-radius: 20px;
  background: #FFFFFF;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.plan-card__main {
  flex: 1;
  min-width: 0;
}

.plan-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.plan-card__icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, #6FCF97, #4FB783);
  color: #FFFFFF;
}

.plan-card__icon svg {
  width: 14px;
  height: 14px;
}

.plan-card strong {
  color: #1F2937;
  font-size: 14px;
  font-weight: 600;
}

.plan-card em {
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(111, 207, 151, 0.15);
  color: #4FB783;
  font-size: 10px;
  font-style: normal;
  font-weight: 500;
}

.plan-card p {
  margin: 0 0 0 36px;
  color: #6B7280;
  font-size: 12px;
  line-height: 1.5;
}

.plan-card > button {
  flex-shrink: 0;
  min-width: 72px;
  height: 32px;
  padding: 0 16px;
  border-radius: 999px;
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 5px 12px rgba(36,158,102,0.22);
}

.home-entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.home-entry-card {
  position: relative;
  min-height: 138px;
  overflow: hidden;
  padding: 14px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  border-radius: 20px;
  border: 1px solid transparent;
  text-align: left;
  box-shadow: 0 1px 5px rgba(31, 41, 55, 0.05);
}

.home-entry-card__pattern {
  position: absolute;
  pointer-events: none;
}

.home-entry-card--checkin {
  border-color: #DDF4FA;
  background: linear-gradient(145deg, #FFFFFF 0%, #EEF9FC 100%);
}

.home-entry-card--checkin .home-entry-card__pattern {
  right: -22px;
  top: -18px;
  width: 88px;
  height: 88px;
  border-radius: 30px;
  background:
    linear-gradient(90deg, rgba(79, 170, 196, 0.16) 1px, transparent 1px),
    linear-gradient(rgba(79, 170, 196, 0.16) 1px, transparent 1px);
  background-size: 14px 14px;
  transform: rotate(10deg);
}

.home-entry-card--risk {
  border-color: #F8E7D1;
  background: linear-gradient(145deg, #FFFFFF 0%, #FFF6EA 100%);
}

.home-entry-card--risk .home-entry-card__pattern {
  right: -18px;
  top: -20px;
  width: 90px;
  height: 90px;
  border-radius: 999px;
  background: radial-gradient(circle at 35% 35%, rgba(245, 158, 11, 0.24), rgba(239, 143, 143, 0.06) 58%, transparent 59%);
}

.home-entry-card__icon {
  position: relative;
  z-index: 1;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 13px;
  color: #FFFFFF;
  box-shadow: 0 6px 14px rgba(31, 41, 55, 0.08);
}

.home-entry-card--checkin .home-entry-card__icon {
  background: linear-gradient(135deg, #4FAAC4, #7FD5B2);
}

.home-entry-card--risk .home-entry-card__icon {
  background: linear-gradient(135deg, #F59E0B, #EF8F8F);
}

.home-entry-card__icon svg {
  width: 17px;
  height: 17px;
  stroke-width: 2;
}

.home-entry-card__content {
  position: relative;
  z-index: 1;
  min-width: 0;
  margin-top: 12px;
}

.home-entry-card strong,
.home-entry-card small,
.home-entry-card em {
  display: block;
}

.home-entry-card strong {
  color: #1F2937;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
}

.home-entry-card small {
  margin-top: 5px;
  color: #6B7280;
  font-size: 11px;
  line-height: 1.55;
}

.home-entry-card em {
  position: relative;
  z-index: 1;
  margin-top: auto;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
}

.home-entry-card--checkin em {
  background: rgba(79, 170, 196, 0.14);
  color: #3D91AA;
}

.home-entry-card--risk em {
  background: rgba(245, 158, 11, 0.14);
  color: #C97912;
}

.home-section {
  margin-top: 16px;
}

.home-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.home-section__head h2 {
  margin: 0;
  color: #1F2937;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.5;
}

.home-section__head button {
  display: flex;
  align-items: center;
  gap: 2px;
  background: transparent;
  color: #6FCF97;
  font-size: 12px;
  font-weight: 500;
}

.home-section__head svg {
  width: 14px;
  height: 14px;
}

.doctor-strip {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.doctor-card {
  flex: 0 0 108px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid #F9FAFB;
  border-radius: 20px;
  background: #FFFFFF;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.doctor-card__avatar {
  width: 56px;
  height: 56px;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  overflow: hidden;
  font-size: 18px;
  font-weight: 700;
}

.doctor-card__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.doctor-card p,
.doctor-card span {
  margin: 0;
  color: #6B7280;
  font-size: 10px;
  line-height: 1.5;
}

.doctor-card strong {
  margin-top: 2px;
  color: #1F2937;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
}

.doctor-card button {
  width: 100%;
  min-height: 27px;
  margin-top: 8px;
  border-radius: 999px;
  background: #DDF7E9;
  color: #4FB783;
  font-size: 10px;
  font-weight: 500;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.article-card {
  width: 100%;
  padding: 12px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  border-radius: 18px;
  background: #FFFFFF;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  text-align: left;
}

.article-card__icon {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: #4FB783;
}

.article-card__icon svg {
  width: 28px;
  height: 28px;
  stroke-width: 1.5;
}

.article-card__body {
  flex: 1;
  min-width: 0;
  padding-top: 2px;
  display: flex;
  flex-direction: column;
}

.article-card strong {
  margin-bottom: 4px;
  color: #1F2937;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.375;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-card__body > span {
  margin-bottom: 6px;
  color: #6B7280;
  font-size: 11px;
  line-height: 1.625;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-card em {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #6B7280;
  font-size: 10px;
  font-style: normal;
  line-height: 1.5;
}

.article-card em svg {
  width: 12px;
  height: 12px;
}

.home-disclaimer {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #DDF7E9;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.60);
  color: rgba(107, 114, 128, 0.70);
  font-size: 10px;
  line-height: 1.5;
  text-align: center;
}
</style>
