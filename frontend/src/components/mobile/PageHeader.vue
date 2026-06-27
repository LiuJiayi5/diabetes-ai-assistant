<template>
  <header class="page-header">
    <button v-if="showBack" class="page-header__back" type="button" @click="handleBack">
      <ChevronLeft class="page-header__icon" />
    </button>
    <h1 class="page-header__title">{{ title }}</h1>
    <div class="page-header__right">
      <slot name="right" />
    </div>
  </header>
</template>

<script setup>
import { ChevronLeft } from 'lucide-vue-next'
import { useRouter } from 'vue-router'

const props = defineProps({
  title: { type: String, required: true },
  showBack: { type: Boolean, default: true },
  backTo: { type: String, default: '' }
})

const router = useRouter()

function handleBack() {
  if (props.backTo) {
    router.push(props.backTo)
    return
  }
  router.back()
}
</script>

<style scoped>
.page-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: 40px 1fr 40px;
  align-items: center;
  padding: 12px 16px 8px;
  background: var(--figma-bg-page);
}

.page-header__back {
  grid-column: 1;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.85);
  color: var(--figma-text-strong);
  box-shadow: var(--figma-shadow-card);
}

.page-header__icon {
  width: 20px;
  height: 20px;
}

.page-header__title {
  grid-column: 2;
  margin: 0;
  text-align: center;
  font-size: 17px;
  font-weight: 700;
  color: var(--figma-text-strong);
}

.page-header__right {
  grid-column: 3;
  display: flex;
  justify-content: flex-end;
}
</style>
