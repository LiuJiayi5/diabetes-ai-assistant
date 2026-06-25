import { defineStore } from 'pinia'
import { getArticleCategories, getArticleDetail, listArticles } from '@/api/article'

export const ARTICLE_CATEGORIES = [
  '全部',
  '饮食指导',
  '运动指南',
  '日常习惯',
  '糖尿病科普',
  '并发症预防',
  '控糖误区'
]

export const FEATURED_CATEGORIES = [
  {
    title: '饮食指导',
    subtitle: '合理搭配饮食，控制血糖水平',
    tags: ['控制碳水', '低糖水果'],
    bg: 'linear-gradient(135deg, #BCEBCF 0%, #C7E8F7 100%)'
  },
  {
    title: '运动指南',
    subtitle: '适度运动，增强体质',
    tags: ['有氧运动', '间歇训练'],
    bg: 'linear-gradient(135deg, #BFE3F5 0%, #BCEBCF 100%)'
  },
  {
    title: '日常习惯',
    subtitle: '养成健康生活方式',
    tags: ['规律作息', '避免久坐'],
    bg: 'linear-gradient(135deg, #E8E8FC 0%, #EAF5FA 100%)'
  },
  {
    title: '糖尿病科普',
    subtitle: '了解糖尿病相关知识',
    tags: ['糖尿病类型', '常见症状'],
    bg: 'linear-gradient(135deg, #EAF8F4 0%, #E8F4FA 100%)'
  }
]

const CATEGORY_CODE_TO_LABEL = {
  diet: ARTICLE_CATEGORIES[1],
  exercise: ARTICLE_CATEGORIES[2],
  habit: ARTICLE_CATEGORIES[3],
  science: ARTICLE_CATEGORIES[4],
  complication: ARTICLE_CATEGORIES[5],
  mistake: ARTICLE_CATEGORIES[6]
}

const CATEGORY_LABEL_TO_CODE = Object.fromEntries(
  Object.entries(CATEGORY_CODE_TO_LABEL).map(([code, label]) => [label, code])
)

export const MOCK_ARTICLES = [
  {
    article_id: 1,
    title: '控糖饮食的三个关键原则',
    category: '饮食指导',
    summary: '合理控制碳水摄入，搭配优质蛋白和蔬菜，帮助稳定血糖水平。',
    content: '控糖饮食并不是完全不吃主食，而是学会选择低升糖指数的主食，控制每餐总量，并搭配足量蔬菜和优质蛋白。建议减少含糖饮料、甜点和油炸食品，日常记录餐后血糖变化，逐步找到适合自己的饮食节奏。',
    status: 'published',
    view_count: 120,
    is_recommended: true,
    created_at: '2026-06-20 09:30',
    updated_at: '2026-06-20 09:30',
    tags: ['控制碳水', '低糖水果'],
    bg: '#E5F6EE',
    color: '#5BBF8A',
    tagBg: '#EEF8F2',
    tagColor: '#4A8A6A'
  },
  {
    article_id: 2,
    title: '饭后散步对血糖有什么帮助',
    category: '运动指南',
    summary: '适度运动有助于改善餐后血糖波动，建议根据自身情况循序渐进。',
    content: '饭后短时间低强度散步，有助于肌肉利用葡萄糖，改善餐后血糖波动。建议从 10 到 15 分钟开始，选择平缓路线，避免刚吃完就进行高强度运动。如出现明显不适，应停止运动并及时咨询医生。',
    status: 'published',
    view_count: 86,
    is_recommended: true,
    created_at: '2026-06-19 18:20',
    updated_at: '2026-06-19 18:20',
    tags: ['饭后散步', '有氧运动'],
    bg: '#E4F3FB',
    color: '#4FAAC4',
    tagBg: '#EEF6FF',
    tagColor: '#3A8AAC'
  },
  {
    article_id: 3,
    title: '久坐人群如何降低糖尿病风险',
    category: '日常习惯',
    summary: '避免长时间久坐，每小时起身活动，有助于改善代谢状态。',
    content: '久坐会影响身体代谢，增加体重管理和血糖控制压力。建议每坐 45 到 60 分钟起身活动几分钟，可以选择站立办公、拉伸或短距离步行。规律睡眠、稳定情绪和持续运动同样重要。',
    status: 'published',
    view_count: 98,
    is_recommended: false,
    created_at: '2026-06-18 14:10',
    updated_at: '2026-06-18 14:10',
    tags: ['避免久坐', '规律作息'],
    bg: '#EDE8FC',
    color: '#9B8FD4',
    tagBg: '#F3EEFF',
    tagColor: '#7A6DB8'
  },
  {
    article_id: 4,
    title: '糖尿病的早期症状及预防措施',
    category: '糖尿病科普',
    summary: '了解多饮、多食、多尿等常见表现，及早关注身体变化。',
    content: '糖尿病早期可能出现口渴、多尿、容易疲劳、体重变化等表现，也可能没有明显症状。定期体检、关注空腹血糖和糖化血红蛋白，有助于早发现风险。本文仅作健康科普，不能替代线下诊疗。',
    status: 'published',
    view_count: 156,
    is_recommended: true,
    created_at: '2026-06-16 10:00',
    updated_at: '2026-06-16 10:00',
    tags: ['早期症状', '风险因素'],
    bg: '#E5F6EE',
    color: '#5BBF8A',
    tagBg: '#EEF8F2',
    tagColor: '#4A8A6A'
  },
  {
    article_id: 5,
    title: '为什么糖尿病患者要关注眼部健康',
    category: '并发症预防',
    summary: '长期血糖异常可能影响眼部健康，定期检查有助于早发现。',
    content: '长期血糖控制不佳可能影响眼底血管健康。糖尿病患者或高风险人群应关注视力变化，按医生建议进行眼底检查。日常保持血糖、血压和血脂管理，有助于降低并发症风险。',
    status: 'published',
    view_count: 72,
    is_recommended: false,
    created_at: '2026-06-14 16:45',
    updated_at: '2026-06-14 16:45',
    tags: ['眼部检查', '并发症预防'],
    bg: '#FEF3E2',
    color: '#E8A840',
    tagBg: '#FEF8EC',
    tagColor: '#B8862A'
  },
  {
    article_id: 6,
    title: '不吃主食真的能控糖吗',
    category: '控糖误区',
    summary: '完全不吃主食并不科学，合理选择主食种类和摄入量更重要。',
    content: '控糖不等于完全不吃主食。长期过度限制主食可能影响营养均衡，也难以持续。更推荐选择全谷物、杂豆、薯类等主食来源，并结合个人血糖监测结果调整摄入量。',
    status: 'published',
    view_count: 104,
    is_recommended: false,
    created_at: '2026-06-12 08:50',
    updated_at: '2026-06-12 08:50',
    tags: ['控糖误区', '主食选择'],
    bg: '#EEF2FE',
    color: '#7A9BD4',
    tagBg: '#EEF2FE',
    tagColor: '#5A7BC4'
  }
]

function unwrapResponse(response) {
  if (Array.isArray(response)) return response
  return response?.data ?? response?.records ?? response?.list ?? response?.items ?? response
}

function normalizeList(response) {
  const data = unwrapResponse(response)
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.items)) return data.items
  return []
}

function normalizeArticle(article, index = 0) {
  const fallback = MOCK_ARTICLES[index % MOCK_ARTICLES.length]
  const tags = Array.isArray(article?.tags)
    ? article.tags
    : String(article?.tags || '')
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)

  const rawCategory = article?.category ?? article?.category_code ?? fallback.category
  const displayCategory = CATEGORY_CODE_TO_LABEL[rawCategory] || rawCategory

  return {
    ...fallback,
    ...article,
    article_id: article?.article_id ?? article?.articleId ?? article?.id ?? fallback.article_id,
    category: displayCategory,
    category_code: CATEGORY_LABEL_TO_CODE[displayCategory] || rawCategory,
    cover_image: article?.cover_image ?? article?.coverImage ?? article?.imageUrl ?? '',
    view_count: article?.view_count ?? article?.viewCount ?? fallback.view_count,
    is_recommended: article?.is_recommended ?? article?.isRecommended ?? fallback.is_recommended,
    created_at: article?.created_at ?? article?.createdAt ?? article?.createTime ?? fallback.created_at,
    updated_at: article?.updated_at ?? article?.updatedAt ?? article?.updateTime ?? fallback.updated_at,
    tags: tags.length ? tags : fallback.tags
  }
}

function isPublished(article) {
  return !article.status || ['published', '已上架', 'enabled', 'active'].includes(article.status)
}

function resolveErrorMessage(error, fallback = '健康资讯加载失败，请稍后重试') {
  return error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback
}

function toApiCategory(category) {
  if (!category || category === '全部') return undefined
  return CATEGORY_LABEL_TO_CODE[category] || category
}

export const useArticlesStore = defineStore('articles', {
  state: () => ({
    articles: [],
    categories: ARTICLE_CATEGORIES,
    detail: null,
    loading: false,
    detailLoading: false,
    error: '',
    detailError: '',
    usingFallback: false,
    favorites: []
  }),
  getters: {
    recommendedArticles: (state) => state.articles.filter((article) => article.is_recommended).slice(0, 4)
  },
  actions: {
    async fetchCategories() {
      try {
        const response = await getArticleCategories()
        const list = normalizeList(response)
        this.categories = list.length
          ? ['全部', ...list.filter((item) => item && item !== '全部')]
          : ARTICLE_CATEGORIES
      } catch (error) {
        this.categories = ARTICLE_CATEGORIES
      }
      return this.categories
    },
    async fetchArticles(params = {}) {
      this.loading = true
      this.error = ''
      try {
        const response = await listArticles({
          page: 1,
          page_size: 20,
          ...params,
          category: toApiCategory(params.category)
        })
        const list = normalizeList(response)
        this.articles = list.map(normalizeArticle).filter(isPublished)
        this.usingFallback = false
      } catch (error) {
        this.error = resolveErrorMessage(error)
        this.articles = MOCK_ARTICLES
        this.usingFallback = true
      } finally {
        this.loading = false
      }
      return this.articles
    },
    async fetchArticleDetail(articleId) {
      this.detailLoading = true
      this.detailError = ''
      try {
        const response = await getArticleDetail(articleId)
        this.detail = normalizeArticle(unwrapResponse(response))
        this.usingFallback = false
      } catch (error) {
        const localArticle = this.articles.find((article) => String(article.article_id) === String(articleId))
          || MOCK_ARTICLES.find((article) => String(article.article_id) === String(articleId))
        if (localArticle) {
          this.detail = localArticle
          this.detailError = '当前展示本地示例内容，后端详情接口接入后将自动替换。'
        } else {
          this.detail = null
          this.detailError = resolveErrorMessage(error, '资讯详情加载失败')
        }
      } finally {
        this.detailLoading = false
      }
      return this.detail
    },
    toggleFavorite(articleId) {
      const id = String(articleId)
      if (this.favorites.includes(id)) {
        this.favorites = this.favorites.filter((item) => item !== id)
      } else {
        this.favorites = [...this.favorites, id]
      }
    },
    isFavorite(articleId) {
      return this.favorites.includes(String(articleId))
    }
  }
})
