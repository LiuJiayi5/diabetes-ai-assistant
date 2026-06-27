<template>
  <span class="risk-tag" :class="`risk-tag--${level || 'unknown'}`">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'
import { formatRiskLevel } from '@/utils/health'

const props = defineProps({
  level: { type: String, default: '' }
})

const label = computed(() => formatRiskLevel(props.level) || '待评估')
</script>

<style scoped>
.risk-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: var(--figma-radius-pill);
  font-size: 12px;
  font-weight: 600;
}

.risk-tag--low {
  background: rgba(111, 207, 151, 0.18);
  color: #3d9b6a;
}

.risk-tag--medium {
  background: rgba(255, 193, 120, 0.22);
  color: #c67a2e;
}

.risk-tag--high {
  background: rgba(232, 120, 120, 0.18);
  color: #c94f4f;
}

.risk-tag--unknown {
  background: rgba(154, 173, 184, 0.18);
  color: var(--figma-text-muted);
}
</style>
