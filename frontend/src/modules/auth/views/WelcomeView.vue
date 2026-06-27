<template>
  <MobileShell>
    <section class="welcome-page">
      <div class="glow glow--top" />
      <div class="glow glow--bottom" />

      <div class="welcome-page__content">
        <div class="brand-logo">
          <Leaf class="brand-logo__leaf" :stroke-width="2" />
          <span class="brand-logo__badge">
            <Droplet class="brand-logo__drop" fill="white" />
          </span>
        </div>

        <div class="welcome-page__heading">
          <h1>糖尿病预治智能助手</h1>
          <p>让控糖生活更清晰</p>
        </div>

        <div class="welcome-page__preview">
          <div class="preview-card preview-card--primary">
            <span>今日方案</span>
            <strong>饮食 + 运动</strong>
          </div>
          <div class="preview-card">
            <span>健康记录</span>
            <strong>持续追踪</strong>
          </div>
        </div>

        <button class="primary-button" type="button" @click="goLogin">开始使用</button>
        <button class="ghost-link" type="button" @click="goRegister">创建新账号</button>
      </div>
    </section>
  </MobileShell>
</template>

<script setup>
import { Droplet, Leaf } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import MobileShell from '@/components/mobile/MobileShell.vue'

const router = useRouter()

function goLogin() {
  router.push('/login')
}

function goRegister() {
  router.push('/register')
}
</script>

<style scoped>
.welcome-page {
  position: relative;
  min-height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 64px 24px 34px;
  background: var(--figma-auth-bg);
}

.glow {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(48px);
}

.glow--top {
  top: -22px;
  right: -36px;
  width: 256px;
  height: 256px;
  background: rgba(111, 207, 151, 0.15);
}

.glow--bottom {
  left: -56px;
  bottom: 88px;
  width: 288px;
  height: 288px;
  background: rgba(190, 234, 242, 0.25);
}

.welcome-page__content {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand-logo {
  width: 92px;
  height: 92px;
  margin-top: 44px;
  margin-bottom: 26px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 28px;
  background: #FFFFFF;
  box-shadow: 0 10px 26px rgba(90, 180, 150, 0.18);
}

.brand-logo__leaf {
  width: 46px;
  height: 46px;
  color: var(--figma-primary-green);
}

.brand-logo__badge {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--figma-primary-green-dark);
}

.brand-logo__drop {
  width: 15px;
  height: 15px;
  color: #FFFFFF;
}

.welcome-page__heading {
  text-align: center;
}

.welcome-page__heading h1 {
  margin: 0 0 8px;
  color: var(--figma-text-primary);
  font-size: 24px;
  font-weight: 600;
  line-height: 1.5;
}

.welcome-page__heading p {
  margin: 0;
  color: var(--figma-text-secondary);
  font-size: 14px;
}

.welcome-page__preview {
  width: 100%;
  margin: auto 0 34px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.preview-card {
  min-height: 104px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.58);
  box-shadow: var(--figma-shadow-card);
  backdrop-filter: blur(8px);
}

.preview-card--primary {
  background: linear-gradient(145deg, #FFFFFF 0%, #EEF8F3 60%, #EAF5FA 100%);
}

.preview-card span {
  display: block;
  margin-bottom: 8px;
  color: var(--figma-text-muted);
  font-size: 12px;
}

.preview-card strong {
  color: var(--figma-text-strong);
  font-size: 15px;
  line-height: 1.45;
}

.primary-button {
  width: 100%;
  height: 48px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 16px;
  font-weight: 700;
  box-shadow: var(--figma-shadow-button);
}

.ghost-link {
  margin-top: 14px;
  background: transparent;
  color: var(--figma-primary-green);
  font-size: 14px;
  font-weight: 500;
}
</style>
