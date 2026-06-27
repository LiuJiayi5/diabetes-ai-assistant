<template>
  <MobileShell>
    <div class="patient-layout">
      <main class="patient-layout__body mobile-scroll">
        <RouterView />
      </main>
      <BottomNav />
    </div>
  </MobileShell>
</template>

<script setup>
import MobileShell from '@/components/mobile/MobileShell.vue'
import BottomNav from '@/components/mobile/BottomNav.vue'
</script>

<style scoped>
.patient-layout {
  position: relative;
  min-height: 100%;
  display: flex;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  background: var(--figma-bg-page);
}

.patient-layout__body {
  flex: 1;
  overflow-y: auto;
}
</style>
