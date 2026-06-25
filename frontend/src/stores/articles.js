import { defineStore } from 'pinia'
import { getArticleCategories, getArticleDetail, getHomeContents, listArticles } from '@/api/article'

export const ALL_CATEGORY = '全部'

export const ARTICLE_CATEGORIES = [
  ALL_CATEGORY,
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
    code: 'diet',
    subtitle: '学会搭配主食、蔬菜和优质蛋白',
    tags: ['餐盘法', '低GI主食'],
    bg: 'linear-gradient(135deg, #BCEBCF 0%, #C7E8F7 100%)'
  },
  {
    title: '运动指南',
    code: 'exercise',
    subtitle: '用适合自己的节奏提升控糖能力',
    tags: ['餐后散步', '抗阻训练'],
    bg: 'linear-gradient(135deg, #BFE3F5 0%, #BCEBCF 100%)'
  },
  {
    title: '日常习惯',
    code: 'habit',
    subtitle: '把作息、饮水和记录变成日常',
    tags: ['规律作息', '压力管理'],
    bg: 'linear-gradient(135deg, #E8E8FC 0%, #EAF5FA 100%)'
  },
  {
    title: '糖尿病科普',
    code: 'science',
    subtitle: '理解指标、类型和风险管理',
    tags: ['血糖指标', '风险识别'],
    bg: 'linear-gradient(135deg, #EAF8F4 0%, #E8F4FA 100%)'
  },
  {
    title: '并发症预防',
    code: 'complication',
    subtitle: '关注眼、足、肾和心血管健康',
    tags: ['定期检查', '日常护理'],
    bg: 'linear-gradient(135deg, #FEF3E2 0%, #EAF8F4 100%)'
  },
  {
    title: '控糖误区',
    code: 'mistake',
    subtitle: '避开常见误解，建立长期习惯',
    tags: ['无糖食品', '科学用药'],
    bg: 'linear-gradient(135deg, #EEF2FE 0%, #DDF7E9 100%)'
  }
]

export const CATEGORY_CODE_TO_LABEL = Object.fromEntries(
  FEATURED_CATEGORIES.map((category) => [category.code, category.title])
)

export const CATEGORY_LABEL_TO_CODE = Object.fromEntries(
  FEATURED_CATEGORIES.map((category) => [category.title, category.code])
)

export const CATEGORY_META = {
  饮食指导: {
    tags: ['餐盘法', '低GI主食'],
    bg: '#E5F6EE',
    color: '#5BBF8A',
    tagBg: '#EEF8F2',
    tagColor: '#4A8A6A'
  },
  运动指南: {
    tags: ['餐后散步', '有氧运动'],
    bg: '#E4F3FB',
    color: '#4FAAC4',
    tagBg: '#EEF6FF',
    tagColor: '#3A8AAC'
  },
  日常习惯: {
    tags: ['规律作息', '压力管理'],
    bg: '#EDE8FC',
    color: '#9B8FD4',
    tagBg: '#F3EEFF',
    tagColor: '#7A6DB8'
  },
  糖尿病科普: {
    tags: ['指标解读', '风险识别'],
    bg: '#E5F6EE',
    color: '#5BBF8A',
    tagBg: '#EEF8F2',
    tagColor: '#4A8A6A'
  },
  并发症预防: {
    tags: ['定期检查', '日常护理'],
    bg: '#FEF3E2',
    color: '#E8A840',
    tagBg: '#FEF8EC',
    tagColor: '#B8862A'
  },
  控糖误区: {
    tags: ['控糖误区', '科学管理'],
    bg: '#EEF2FE',
    color: '#7A9BD4',
    tagBg: '#EEF2FE',
    tagColor: '#5A7BC4'
  }
}

export const FALLBACK_ARTICLES = [
  {
    article_id: 1,
    title: '控糖餐盘怎么搭配',
    category: '饮食指导',
    summary: '用一半蔬菜、适量主食和优质蛋白搭出更稳定的一餐。',
    content: '控糖饮食不是完全不吃主食，而是学会把一餐分配得更均衡。可以先把非淀粉类蔬菜放到餐盘的一半，再搭配适量全谷物或杂豆主食，最后加入鱼、禽、蛋、奶、豆制品等优质蛋白。烹调上尽量少油少盐，少用浓油赤酱。每个人对食物的反应不同，建议结合餐后血糖记录慢慢找到适合自己的份量。如指标明显异常，应及时咨询线下医生。',
    status: 'published',
    view_count: 120,
    is_recommended: true,
    sort_order: 1,
    created_at: '2026-06-20 09:30',
    updated_at: '2026-06-20 09:30',
    tags: ['餐盘法', '低GI主食']
  },
  {
    article_id: 2,
    title: '餐后散步为什么适合多数控糖人群',
    category: '运动指南',
    summary: '餐后轻中等强度活动有助于改善餐后血糖波动。',
    content: '餐后散步的重点不是追求速度，而是让身体温和活动起来。多数人可以从饭后稍作休息后步行10到20分钟开始，选择平坦路线，保持能说话但略微出汗的强度。刚吃完不建议立刻剧烈运动，也不要空腹硬撑。若出现心慌、头晕、胸闷或明显不适，应停止运动并及时就医。',
    status: 'published',
    view_count: 86,
    is_recommended: true,
    sort_order: 2,
    created_at: '2026-06-19 18:20',
    updated_at: '2026-06-19 18:20',
    tags: ['餐后散步', '有氧运动']
  },
  {
    article_id: 3,
    title: '规律作息对血糖管理有什么帮助',
    category: '日常习惯',
    summary: '稳定睡眠和作息能帮助身体形成更清晰的代谢节奏。',
    content: '睡眠不足、熬夜和夜宵都可能让第二天的血糖管理更吃力。建议尽量固定睡觉和起床时间，晚间减少高糖零食和刺激性饮品，睡前保留一点放松时间。情绪压力较大时，可以用深呼吸、散步、听轻音乐等方式缓冲。若长期睡眠差或血糖波动明显，应和医生沟通。',
    status: 'published',
    view_count: 98,
    is_recommended: false,
    sort_order: 3,
    created_at: '2026-06-18 14:10',
    updated_at: '2026-06-18 14:10',
    tags: ['规律作息', '压力管理']
  },
  {
    article_id: 4,
    title: '什么是糖尿病',
    category: '糖尿病科普',
    summary: '先理解血糖为什么会升高，再谈长期管理。',
    content: '血糖来自食物消化吸收，也和身体自身调节有关。胰岛素帮助葡萄糖进入细胞被利用，当胰岛素分泌不足或身体对胰岛素不敏感时，血糖就可能持续偏高。糖尿病需要长期管理，但并不等于生活失控。规律复查、合理饮食、适度运动和遵医嘱治疗都很重要，具体诊断和用药应由医生判断。',
    status: 'published',
    view_count: 156,
    is_recommended: true,
    sort_order: 4,
    created_at: '2026-06-16 10:00',
    updated_at: '2026-06-16 10:00',
    tags: ['血糖指标', '风险识别']
  },
  {
    article_id: 5,
    title: '眼部检查不能忽视',
    category: '并发症预防',
    summary: '视网膜健康需要定期关注，早发现更安心。',
    content: '长期血糖控制不佳可能影响眼底血管健康。糖尿病患者或高风险人群应按医生建议进行眼科随访，尤其是出现视物模糊、黑影、视力下降等变化时不要拖延。日常管理不只看血糖，也要关注血压、血脂和用眼习惯。若出现明显眼部异常，应及时线下就医。',
    status: 'published',
    view_count: 72,
    is_recommended: false,
    sort_order: 5,
    created_at: '2026-06-14 16:45',
    updated_at: '2026-06-14 16:45',
    tags: ['眼部检查', '并发症预防']
  },
  {
    article_id: 6,
    title: '不吃主食就能控糖吗',
    category: '控糖误区',
    summary: '完全取消主食并不科学，选择和份量更重要。',
    content: '控糖不等于完全不吃主食。长期过度限制主食可能造成饥饿感强、营养不均衡，也不利于坚持。更推荐选择全谷物、杂豆、薯类等主食来源，控制总量，并搭配蔬菜和蛋白质。不同人的餐后反应不同，可以记录血糖变化后再调整。用药或饮食方案变化应咨询医生。',
    status: 'published',
    view_count: 104,
    is_recommended: false,
    sort_order: 6,
    created_at: '2026-06-12 08:50',
    updated_at: '2026-06-12 08:50',
    tags: ['控糖误区', '主食选择']
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

function normalizeText(value) {
  return String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '')
}

function splitKeyword(value) {
  return String(value ?? '')
    .trim()
    .toLowerCase()
    .split(/[\s,，。;；、]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function fieldScore(field, keyword, weight) {
  const text = normalizeText(field)
  if (!text || !keyword.compact) return 0
  let score = 0
  if (text === keyword.compact) score += weight * 3
  if (text.includes(keyword.compact)) score += weight * 2
  keyword.tokens.forEach((token) => {
    const compactToken = normalizeText(token)
    if (compactToken && text.includes(compactToken)) score += weight
  })
  return score
}

export function searchArticles(articles, rawKeyword, options = {}) {
  const compact = normalizeText(rawKeyword)
  if (!compact) return [...articles]

  const keyword = {
    compact,
    tokens: splitKeyword(rawKeyword)
  }

  return articles
    .map((article) => {
      const tags = Array.isArray(article.tags) ? article.tags.join(' ') : article.tags
      const score =
        fieldScore(article.title, keyword, 12)
        + fieldScore(article.summary, keyword, 8)
        + fieldScore(article.category, keyword, 7)
        + fieldScore(article.category_code, keyword, 5)
        + fieldScore(tags, keyword, 6)
        + fieldScore(article.keywords, keyword, 6)
        + fieldScore(article.content, keyword, 3)

      return { article, score }
    })
    .filter((item) => item.score > 0)
    .sort((a, b) => {
      if (b.score !== a.score) return b.score - a.score
      if (Number(b.article.is_recommended) !== Number(a.article.is_recommended)) {
        return Number(b.article.is_recommended) - Number(a.article.is_recommended)
      }
      return Number(a.article.sort_order ?? 999) - Number(b.article.sort_order ?? 999)
    })
    .map((item) => item.article)
    .slice(0, options.limit || articles.length)
}

export function getCategoryMeta(category) {
  const label = CATEGORY_CODE_TO_LABEL[category] || category
  const featured = FEATURED_CATEGORIES.find((item) => item.code === category || item.title === label)
  return {
    title: label || '健康资讯',
    code: featured?.code || CATEGORY_LABEL_TO_CODE[label] || category,
    subtitle: featured?.subtitle || '精选健康科普内容，帮助你更稳地管理日常生活。',
    tags: featured?.tags || CATEGORY_META[label]?.tags || [],
    bg: featured?.bg || 'linear-gradient(135deg, #EAF8F4 0%, #E8F4FA 100%)'
  }
}

export function sampleArticlesByCategory(articles, limit = 10) {
  const published = [...articles].sort((a, b) => {
    if (Number(b.is_recommended) !== Number(a.is_recommended)) {
      return Number(b.is_recommended) - Number(a.is_recommended)
    }
    return Number(a.sort_order ?? 999) - Number(b.sort_order ?? 999)
  })
  const selected = []
  FEATURED_CATEGORIES.forEach((category) => {
    const article = published.find((item) => item.category_code === category.code && !selected.includes(item))
    if (article) selected.push(article)
  })
  published.forEach((article) => {
    if (selected.length < limit && !selected.includes(article)) selected.push(article)
  })
  return selected.slice(0, limit)
}

function normalizeArticle(article, index = 0) {
  const fallback = FALLBACK_ARTICLES[index % FALLBACK_ARTICLES.length]
  const tags = Array.isArray(article?.tags)
    ? article.tags
    : String(article?.tags || '')
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean)

  const rawCategory = article?.category ?? article?.category_code ?? article?.categoryCode ?? fallback.category
  const displayCategory = CATEGORY_CODE_TO_LABEL[rawCategory] || rawCategory || fallback.category
  const categoryCode = CATEGORY_LABEL_TO_CODE[displayCategory] || rawCategory || fallback.category_code
  const categoryMeta = CATEGORY_META[displayCategory] || CATEGORY_META[fallback.category]

  return {
    ...fallback,
    ...categoryMeta,
    ...article,
    article_id: article?.article_id ?? article?.articleId ?? article?.id ?? fallback.article_id,
    title: article?.title || fallback.title,
    summary: article?.summary || article?.description || fallback.summary,
    content: article?.content || fallback.content,
    category: displayCategory,
    category_code: categoryCode,
    cover_image: article?.cover_image ?? article?.coverImage ?? article?.image_url ?? article?.imageUrl ?? fallback.cover_image ?? '',
    view_count: article?.view_count ?? article?.viewCount ?? fallback.view_count,
    is_recommended: article?.is_recommended ?? article?.isRecommended ?? fallback.is_recommended,
    sort_order: article?.sort_order ?? article?.sortOrder ?? fallback.sort_order ?? index + 1,
    created_at: article?.created_at ?? article?.createdAt ?? article?.createTime ?? fallback.created_at,
    updated_at: article?.updated_at ?? article?.updatedAt ?? article?.updateTime ?? fallback.updated_at,
    tags: tags.length ? tags : categoryMeta.tags
  }
}

function normalizeHomeContent(item) {
  return {
    content_id: item?.content_id ?? item?.contentId ?? item?.id,
    content_type: item?.content_type ?? item?.contentType,
    title: item?.title || '健康管理',
    subtitle: item?.subtitle || '',
    image_url: item?.image_url ?? item?.imageUrl ?? '',
    link_type: item?.link_type ?? item?.linkType ?? 'none',
    link_value: item?.link_value ?? item?.linkValue ?? '',
    sort_order: item?.sort_order ?? item?.sortOrder ?? 0,
    status: item?.status || 'enabled'
  }
}

function isPublished(article) {
  return !article.status || ['published', '已上架', 'enabled', 'active'].includes(article.status)
}

function resolveErrorMessage(error, fallback = '健康资讯加载失败，请稍后重试') {
  return error?.response?.data?.message || error?.response?.data?.error || error?.message || fallback
}

function toApiCategory(category) {
  if (!category || category === ALL_CATEGORY) return undefined
  return CATEGORY_LABEL_TO_CODE[category] || category
}

export const useArticlesStore = defineStore('articles', {
  state: () => ({
    articles: [],
    categories: ARTICLE_CATEGORIES,
    detail: null,
    loading: false,
    detailLoading: false,
    homeLoading: false,
    error: '',
    detailError: '',
    homeError: '',
    usingFallback: false,
    favorites: [],
    banners: []
  }),
  getters: {
    recommendedArticles: (state) => state.articles.filter((article) => article.is_recommended).slice(0, 4),
    articleCountByCategory: (state) => FEATURED_CATEGORIES.reduce((map, category) => {
      map[category.code] = state.articles.filter((article) => article.category_code === category.code).length
      return map
    }, {})
  },
  actions: {
    async fetchCategories() {
      try {
        const response = await getArticleCategories()
        const list = normalizeList(response)
        this.categories = list.length
          ? [ALL_CATEGORY, ...list.filter((item) => item && item !== ALL_CATEGORY)]
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
          page_size: 60,
          ...params,
          category: toApiCategory(params.category)
        })
        const list = normalizeList(response)
        this.articles = list.map(normalizeArticle).filter(isPublished)
        this.usingFallback = false
      } catch (error) {
        this.error = resolveErrorMessage(error)
        this.articles = FALLBACK_ARTICLES.map(normalizeArticle)
        this.usingFallback = true
      } finally {
        this.loading = false
      }
      return this.articles
    },
    async fetchHomeContents() {
      this.homeLoading = true
      this.homeError = ''
      try {
        const response = await getHomeContents()
        const data = unwrapResponse(response)
        const banners = normalizeList(data?.banners || [])
        this.banners = banners
          .map(normalizeHomeContent)
          .filter((item) => item.content_type === 'banner' && item.status === 'enabled')
      } catch (error) {
        this.homeError = resolveErrorMessage(error, '首页内容加载失败，请稍后重试')
        this.banners = []
      } finally {
        this.homeLoading = false
      }
      return this.banners
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
          || FALLBACK_ARTICLES.find((article) => String(article.article_id) === String(articleId))
        if (localArticle) {
          this.detail = normalizeArticle(localArticle)
          this.detailError = ''
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
