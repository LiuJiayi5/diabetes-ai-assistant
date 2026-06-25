<template>
  <div class="static-page">
    <header class="static-topbar">
      <button type="button" class="back-button" @click="goBack">
        <ArrowLeft />
      </button>
      <h1>关于系统</h1>
      <span />
    </header>

    <main class="static-content mobile-scroll">
      <section class="static-hero-card">
        <span class="static-hero-card__icon">
          <Leaf :stroke-width="1.8" />
        </span>
        <h2>糖尿病预治智能助手</h2>
        <p>面向糖尿病预防、健康管理与生活干预的智能辅助平台</p>
      </section>

      <section class="static-card">
        <span class="static-card__icon">
          <Sparkles :stroke-width="1.8" />
        </span>
        <div>
          <h2>核心功能</h2>
          <ul>
            <li v-for="item in features" :key="item">{{ item }}</li>
          </ul>
        </div>
      </section>

      <section class="static-card">
        <span class="static-card__icon static-card__icon--blue">
          <Layers :stroke-width="1.8" />
        </span>
        <div>
          <h2>技术组成</h2>
          <ul>
            <li>前端：Vue 3</li>
            <li>后端：Spring Boot</li>
            <li>数据库：MySQL</li>
            <li>AI 服务：智能分析与问答服务</li>
          </ul>
        </div>
      </section>

      <section class="static-card">
        <span class="static-card__icon">
          <Info :stroke-width="1.8" />
        </span>
        <div>
          <h2>版本信息</h2>
          <ul>
            <li>当前版本：v1.0.0</li>
            <li>项目类型：Web 前端实训项目</li>
            <li>当前模块：用户认证与基础信息模块</li>
          </ul>
        </div>
      </section>

      <section class="static-card static-card--notice">
        <span class="static-card__icon static-card__icon--blue">
          <ShieldCheck :stroke-width="1.8" />
        </span>
        <div>
          <h2>安全提示</h2>
          <ul>
            <li>本系统提供的 AI 建议和科普内容仅供参考</li>
            <li>如出现明显身体不适或异常指标，请及时线下就医</li>
          </ul>
        </div>
      </section>
    </main>

    <footer class="static-footer">
      <button type="button" class="static-primary-button" @click="goBack">返回个人中心</button>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowLeft, Info, Layers, Leaf, ShieldCheck, Sparkles } from 'lucide-vue-next'

const router = useRouter()

const features = [
  '用户认证与个人中心',
  '健康档案与健康数据记录',
  '糖尿病风险预测',
  '个性化生活方案',
  '健康资讯与科普内容',
  'AI 医生咨询',
  '饮食运动打卡与行为分析'
]

function goBack() {
  router.push('/app/account')
}
</script>

<style scoped>
@import './static-info.css';
</style>
