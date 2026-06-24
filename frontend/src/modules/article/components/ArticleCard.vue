<template>
  <button type="button" class="article-card" @click="$emit('open', article)">
    <span class="article-card__icon" :style="{ background: article.bg, color: article.color }">
      <BookOpen />
    </span>
    <span class="article-card__body">
      <span class="article-card__title">{{ article.title }}</span>
      <span class="article-card__summary">{{ article.summary }}</span>
      <span class="article-card__meta">
        <span class="article-card__tags">
          <span
            v-for="tag in article.tags.slice(0, 2)"
            :key="tag"
            class="article-tag"
            :style="{ background: article.tagBg, color: article.tagColor }"
          >
            {{ tag }}
          </span>
        </span>
        <span class="article-card__views">
          <Eye />
          {{ article.view_count || 0 }}
        </span>
      </span>
    </span>
    <ChevronRight class="article-card__arrow" />
  </button>
</template>

<script setup>
import { BookOpen, ChevronRight, Eye } from 'lucide-vue-next'

defineProps({
  article: {
    type: Object,
    required: true
  }
})

defineEmits(['open'])
</script>
