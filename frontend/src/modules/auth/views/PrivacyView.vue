<template>
  <div class="static-page">
    <header class="static-topbar">
      <button type="button" class="back-button" @click="goBack">
        <ArrowLeft />
      </button>
      <h1>隐私说明</h1>
      <span />
    </header>

    <main class="static-content mobile-scroll">
      <section
        v-for="section in privacySections"
        :key="section.title"
        class="static-card"
      >
        <span class="static-card__icon">
          <component :is="section.icon" :stroke-width="1.8" />
        </span>
        <div>
          <h2>{{ section.title }}</h2>
          <ul>
            <li v-for="item in section.items" :key="item">{{ item }}</li>
          </ul>
        </div>
      </section>
    </main>

    <footer class="static-footer">
      <button type="button" class="static-primary-button" @click="goBack">我知道了</button>
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowLeft, Bot, Database, FileText, ShieldCheck } from 'lucide-vue-next'

const router = useRouter()

const privacySections = [
  {
    title: '我们会收集哪些信息',
    icon: Database,
    items: [
      '账号基础信息：用户名、手机号、邮箱、头像等',
      '健康相关信息：健康档案、健康指标、评估记录、生活方案、打卡记录等',
      '使用记录：登录时间、功能使用记录、系统操作记录等'
    ]
  },
  {
    title: '这些信息用于什么',
    icon: FileText,
    items: [
      '用于登录认证和账号管理',
      '用于生成健康评估、生活方案和打卡分析',
      '用于展示个人中心、历史记录和健康资讯',
      '用于系统运行维护和问题排查'
    ]
  },
  {
    title: '数据安全说明',
    icon: ShieldCheck,
    items: [
      '本系统为糖尿病预治智能助手实训项目',
      '健康建议仅供参考，不能替代线下诊疗',
      '系统会尽量按照最小必要原则使用数据',
      '不展示夸张承诺，不使用无法保证的绝对化表述'
    ]
  },
  {
    title: 'AI 使用说明',
    icon: Bot,
    items: [
      '智能分析服务仅用于健康评估、方案生成和问答场景',
      '普通健康资讯内容由后台内容管理维护',
      'AI 生成内容仅供参考，不能作为诊断依据'
    ]
  }
]

function goBack() {
  router.push('/app/account')
}
</script>

<style scoped>
@import './static-info.css';
</style>
