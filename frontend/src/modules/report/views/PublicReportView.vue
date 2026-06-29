<template>
  <main class="public-report-redirect">
    <div>
      <LoaderCircle class="public-report-redirect__icon" />
      <p>正在打开报告...</p>
    </div>
  </main>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { LoaderCircle } from 'lucide-vue-next'

const route = useRoute()

onMounted(() => {
  const reportId = encodeURIComponent(route.params.reportId || '')
  window.location.replace(`/api/reports/public/${reportId}/html`)
})
</script>

<style scoped>
.public-report-redirect {
  display: grid;
  min-height: 100vh;
  place-items: center;
  background: #f7fcf9;
  color: #4a8a6a;
  font-size: 15px;
}

.public-report-redirect > div {
  display: grid;
  justify-items: center;
  gap: 10px;
}

.public-report-redirect__icon {
  width: 28px;
  height: 28px;
  animation: public-report-spin 1s linear infinite;
}

@keyframes public-report-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
