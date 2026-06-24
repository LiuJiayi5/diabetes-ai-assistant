<template>
  <div class="mobile-stage">
    <div class="mobile-device" :class="{ 'mobile-device--plain': plain }">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  plain: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.mobile-stage {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--figma-app-outer-bg);
  padding: 18px;
}

.mobile-device {
  width: min(100%, var(--figma-device-width));
  height: min(100vh - 36px, var(--figma-device-height));
  min-height: 720px;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  background: var(--figma-device-bg);
  border-radius: var(--figma-device-radius);
  box-shadow: 0 24px 70px rgba(36, 50, 61, 0.20);
}

.mobile-device--plain {
  background: transparent;
  box-shadow: none;
}

@media (max-width: 520px) {
  .mobile-stage {
    padding: 0;
    align-items: stretch;
  }

  .mobile-device {
    width: 100%;
    height: 100vh;
    min-height: 100vh;
    border-radius: 0;
  }
}
</style>
