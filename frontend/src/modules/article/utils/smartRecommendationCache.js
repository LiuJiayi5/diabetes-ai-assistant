import { getContentRecommendations, getPatientEducationProfile } from '@/api/article'
import { getLatestInterventionReview } from '@/api/interventionReview'

const fallbackCache = new Map()
const profileCacheKey = 'profile'

export function getSmartRecommendationCache() {
  if (typeof window === 'undefined') return fallbackCache
  if (!window.__diabetesSmartRecommendationCache) {
    window.__diabetesSmartRecommendationCache = new Map()
  }
  return window.__diabetesSmartRecommendationCache
}

export function smartRecommendationCacheKey({ scenario, limit }) {
  return `${currentPatientTokenKey()}:${scenario}:${limit}`
}

export async function loadSmartRecommendationBundle({ scenario, limit }) {
  const cache = getSmartRecommendationCache()
  const key = smartRecommendationCacheKey({ scenario, limit })
  const cached = cache.get(key)
  if (cached?.status === 'ready') return cached
  if (cached?.promise) return cached.promise

  const promise = Promise.all([
    getContentRecommendations({ scenario, limit }),
    loadEducationProfile()
  ]).then(([recommendResponse, profile]) => {
    const bundle = {
      status: 'ready',
      items: unwrapList(recommendResponse),
      profile
    }
    cache.set(key, bundle)
    return bundle
  }).catch((error) => {
    cache.delete(key)
    throw error
  })

  cache.set(key, { status: 'pending', promise })
  return promise
}

export function prefetchSmartRecommendationBundle(options) {
  return loadSmartRecommendationBundle(options).catch(() => null)
}

export function prewarmPatientSmartRecommendations() {
  const home = prefetchSmartRecommendationBundle({ scenario: 'home', limit: 3 })
  const plan = resolveReadingScenario()
    .then((scenario) => prefetchSmartRecommendationBundle({ scenario, limit: 4 }))
    .catch(() => prefetchSmartRecommendationBundle({ scenario: 'life_plan', limit: 4 }))

  return Promise.allSettled([home, plan])
}

async function resolveReadingScenario() {
  const response = await getLatestInterventionReview()
  const review = response?.data ?? response
  return review?.review_id || review?.reviewId ? 'intervention_review' : 'life_plan'
}

async function loadEducationProfile() {
  const cache = getSmartRecommendationCache()
  const key = `${currentPatientTokenKey()}:${profileCacheKey}`
  const cached = cache.get(key)
  if (cached?.status === 'ready') return cached.profile
  if (cached?.promise) return cached.promise

  const promise = getPatientEducationProfile()
    .then((response) => {
      const profile = response?.data ?? response ?? null
      cache.set(key, { status: 'ready', profile })
      return profile
    })
    .catch(() => {
      cache.delete(key)
      return null
    })

  cache.set(key, { status: 'pending', promise })
  return promise
}

function unwrapList(response) {
  const data = response?.data ?? response
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

function currentPatientTokenKey() {
  if (typeof window === 'undefined') return 'server'
  const token = localStorage.getItem('diabetes_patient_jwt') || sessionStorage.getItem('diabetes_patient_jwt') || ''
  return token ? token.slice(-18) : 'anonymous'
}
