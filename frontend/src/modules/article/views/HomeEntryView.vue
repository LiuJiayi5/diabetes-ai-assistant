<template>
  <section class="home-page">
    <header class="home-header">
      <div class="brand-mark">
        <Leaf :size="20" :stroke-width="2" />
      </div>
      <div>
        <h1>糖尿病预治助手</h1>
        <p>今日健康管理入口</p>
      </div>
    </header>

    <main class="home-scroll mobile-scroll">
      <section class="hero-card">
        <p>今天也一起</p>
        <h2>稳定控糖，从记录开始</h2>
        <span>饮食、运动、方案与 AI 建议都会逐步沉淀为你的健康轨迹。</span>
      </section>

      <RouterLink class="checkin-card" to="/app/checkin">
        <div class="checkin-icon">
          <CalendarCheck2 :size="22" :stroke-width="2" />
        </div>
        <div>
          <h3>今日打卡</h3>
          <p>记录饮食与运动完成情况</p>
        </div>
        <ChevronRight class="arrow" :size="18" :stroke-width="1.8" />
      </RouterLink>

      <section class="info-card">
        <h3>健康提示</h3>
        <p>保持每日记录，可以帮助后续行为分析更准确地识别你的生活习惯。</p>
      </section>

      <p class="medical-note">AI 建议仅供参考，不能替代线下诊疗。</p>
    </main>
  </section>
</template>

<script setup>
import { CalendarCheck2, ChevronRight, Leaf } from 'lucide-vue-next'
</script>

<style scoped>
.home-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #F4FAF7;
}

.home-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px 14px;
  background: #FFFFFF;
}

.brand-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: var(--figma-secondary-green);
  color: var(--figma-primary-green-dark);
}

.home-header h1 {
  margin: 0;
  color: var(--figma-text-primary);
  font-size: 15px;
  font-weight: 600;
}

.home-header p {
  margin: 2px 0 0;
  color: var(--figma-text-muted);
  font-size: 11px;
}

.home-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px 18px;
}

.hero-card {
  padding: 20px;
  border-radius: var(--figma-radius-card);
  background: linear-gradient(135deg, #6FCF97, #4FB783, #BEEAF2);
  color: #FFFFFF;
  box-shadow: var(--figma-shadow-hero);
}

.hero-card p {
  margin: 0 0 5px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 12px;
}

.hero-card h2 {
  margin: 0 0 8px;
  font-size: 20px;
  line-height: 1.35;
}

.hero-card span {
  display: block;
  max-width: 260px;
  color: rgba(255, 255, 255, 0.86);
  font-size: 12px;
  line-height: 1.7;
}

.checkin-card,
.info-card {
  margin-top: 14px;
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.checkin-card {
  min-height: 86px;
  display: grid;
  grid-template-columns: 44px 1fr 20px;
  gap: 12px;
  align-items: center;
  padding: 16px;
}

.checkin-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 15px;
  background: var(--figma-secondary-green);
  color: var(--figma-primary-green-dark);
}

.checkin-card h3,
.info-card h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 15px;
  font-weight: 600;
}

.checkin-card p,
.info-card p {
  margin: 5px 0 0;
  color: var(--figma-text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.arrow {
  color: var(--figma-icon-muted);
}

.info-card {
  padding: 16px;
  background: linear-gradient(145deg, #EDF8F4, #EAF5FA);
}

.medical-note {
  margin: 14px 0 0;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.65);
  color: rgba(107, 114, 128, 0.78);
  font-size: 10px;
  text-align: center;
}
</style>
