<template>
  <section class="figma-card" :class="[`figma-card--${variant}`, { 'figma-card--clickable': clickable }]" @click="handleClick">
    <slot />
  </section>
</template>

<script setup>
const props = defineProps({
  variant: {
    type: String,
    default: 'white'
  },
  clickable: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

function handleClick(event) {
  if (props.clickable) {
    emit('click', event)
  }
}
</script>

<style scoped>
.figma-card {
  border-radius: var(--figma-radius-card);
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: var(--figma-shadow-card);
}

.figma-card--white {
  background: var(--figma-bg-card);
}

.figma-card--gradient {
  background: var(--figma-profile-card);
  box-shadow: var(--figma-shadow-profile);
}

.figma-card--soft {
  background: var(--figma-info-soft);
}

.figma-card--clickable {
  cursor: pointer;
}
</style>
