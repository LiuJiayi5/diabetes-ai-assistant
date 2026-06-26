<template>
  <div class="article-tabs mobile-scroll">
    <button
      v-for="category in categories"
      :key="category"
      type="button"
      class="article-tab"
      :class="{ 'article-tab--active': category === modelValue }"
      @click="$emit('update:modelValue', category)"
    >
      {{ category }}
    </button>
  </div>
</template>

<script setup>
defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: String,
    default: '全部'
  }
})

defineEmits(['update:modelValue'])
</script>
