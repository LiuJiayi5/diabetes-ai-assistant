<template>
  <AiChatPageShell title="AI 医生助手" subtitle="解释控糖、饮食、运动与打卡分析">
    <section class="assistant-hero">
      <div class="hero-copy">
        <p class="eyebrow">智能健康咨询</p>
        <h2>把控糖问题说清楚</h2>
        <p>我可以结合你的健康信息和最近打卡情况，给出更容易执行的生活建议。</p>
      </div>
      <div class="doctor-avatar">
        <Bot :size="42" :stroke-width="1.9" />
        <span></span>
      </div>
    </section>

    <section class="quick-card">
      <div class="section-title">
        <Sparkles :size="18" />
        <h3>可以这样问我</h3>
      </div>
      <div class="question-list">
        <button v-for="item in quickQuestions" :key="item" type="button" @click="startWith(item)">
          {{ item }}
        </button>
      </div>
    </section>

    <section class="expert-section">
      <div class="section-title">
        <Stethoscope :size="18" />
        <h3>选择咨询专家</h3>
      </div>
      <div v-if="loadingExperts" class="expert-loading">正在读取专家...</div>
      <div v-else class="expert-list">
        <article v-for="expert in experts" :key="expert.expert_id" class="expert-card">
          <div class="expert-avatar">
            <img v-if="expertAvatar(expert)" :src="expertAvatar(expert)" alt="" />
            <span v-else>{{ initial(expert.expert_name) }}</span>
          </div>
          <div class="expert-copy">
            <h4>{{ expert.expert_name }}</h4>
            <p>{{ expert.title || 'AI 专家' }} · {{ expert.department || '智能咨询' }}</p>
            <span>{{ expert.specialty || '糖尿病健康咨询' }}</span>
          </div>
          <button type="button" @click="startWithExpert(expert)">咨询</button>
        </article>
      </div>
    </section>

    <section class="ability-grid">
      <article v-for="item in abilities" :key="item.title" class="ability-card">
        <div class="ability-icon" :class="item.tone">
          <component :is="item.icon" :size="19" :stroke-width="2" />
        </div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.text }}</p>
      </article>
    </section>

    <section class="notice-card">
      <ShieldCheck :size="18" />
      <p>AI 建议仅供健康管理参考，不能替代线下医生诊疗；如有明显不适或指标异常，请及时就医。</p>
    </section>

    <button class="primary-button" type="button" @click="router.push('/app/ai-chat/chat')">
      <MessageCircle :size="18" />
      <span>立即咨询</span>
    </button>
  </AiChatPageShell>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bot, Dumbbell, HeartPulse, MessageCircle, Salad, ShieldCheck, Sparkles, Stethoscope } from 'lucide-vue-next'
import AiChatPageShell from '../components/AiChatPageShell.vue'
import { getAiExperts } from '@/api/aiChat'
import { resolveAvatarUrl } from '@/utils/assets'

const router = useRouter()
const experts = ref([])
const loadingExperts = ref(false)

const quickQuestions = [
  '帮我解释最近一周打卡分析',
  '为什么我的完成率不高？',
  '饮食和运动应该怎么调整？'
]

const abilities = [
  { title: '糖尿病问答', text: '解释基础知识、常见类型和日常注意事项。', icon: Stethoscope, tone: 'green' },
  { title: '饮食建议', text: '围绕控糖饮食、食物选择和餐次安排给建议。', icon: Salad, tone: 'cyan' },
  { title: '运动建议', text: '根据生活习惯给出温和、可坚持的运动方向。', icon: Dumbbell, tone: 'orange' },
  { title: '指标解释', text: '帮助理解血糖、BMI、风险评估和打卡分析。', icon: HeartPulse, tone: 'purple' }
]

function startWith(question) {
  router.push({ path: '/app/ai-chat/chat', query: { q: question } })
}

function startWithExpert(expert) {
  router.push({ path: '/app/ai-chat/chat', query: { expert_id: expert.expert_id } })
}

function initial(name) {
  return (name || 'AI').slice(0, 1)
}

function expertAvatar(expert) {
  return expert?.avatar_url ? resolveAvatarUrl(expert.avatar_url) : ''
}

async function loadExperts() {
  loadingExperts.value = true
  try {
    const response = await getAiExperts()
    experts.value = response?.data || []
  } finally {
    loadingExperts.value = false
  }
}

onMounted(loadExperts)
</script>

<style scoped>
.assistant-hero {
  min-height: 166px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  padding: 22px 20px;
  border-radius: var(--figma-radius-card-lg);
  background: var(--figma-profile-card);
  box-shadow: var(--figma-shadow-hero);
  overflow: hidden;
}

.hero-copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 6px;
  color: rgba(36, 50, 61, 0.65);
  font-size: 11px;
  font-weight: 700;
}

.assistant-hero h2 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 22px;
  font-weight: 800;
  line-height: 1.3;
}

.assistant-hero p:last-child {
  margin: 8px 0 0;
  color: rgba(36, 50, 61, 0.68);
  font-size: 12px;
  line-height: 1.7;
}

.doctor-avatar {
  position: relative;
  width: 92px;
  height: 92px;
  flex: 0 0 92px;
  display: grid;
  place-items: center;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.58);
  color: #4FB783;
  box-shadow: 0 12px 28px rgba(90, 180, 150, 0.15);
}

.doctor-avatar span {
  position: absolute;
  right: 16px;
  bottom: 15px;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.92);
  background: #4FB783;
}

.quick-card,
.ability-card,
.notice-card {
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.quick-card {
  margin-bottom: 14px;
  padding: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4FB783;
}

.section-title h3,
.ability-card h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 14px;
  font-weight: 700;
}

.question-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.question-list button {
  min-height: 38px;
  padding: 0 14px;
  border-radius: var(--figma-radius-pill);
  background: #F7FCF9;
  color: var(--figma-text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-align: left;
}

.ability-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.expert-section {
  margin-bottom: 14px;
  padding: 16px;
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
  box-shadow: var(--figma-shadow-card);
}

.expert-loading {
  padding: 14px 0 2px;
  color: var(--figma-text-muted);
  font-size: 12px;
}

.expert-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.expert-card {
  display: grid;
  grid-template-columns: 44px 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 11px;
  border-radius: 18px;
  background: linear-gradient(145deg, #F7FCF9, #F2FAFB);
}

.expert-avatar {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  overflow: hidden;
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 16px;
  font-weight: 800;
}

.expert-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.expert-copy {
  min-width: 0;
}

.expert-copy h4 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 13px;
  font-weight: 800;
}

.expert-copy p,
.expert-copy span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.expert-copy p {
  margin: 3px 0 0;
  color: var(--figma-text-secondary);
  font-size: 11px;
}

.expert-copy span {
  margin-top: 3px;
  color: var(--figma-text-muted);
  font-size: 10px;
}

.expert-card button {
  min-height: 32px;
  padding: 0 12px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 12px;
  font-weight: 700;
}

.ability-card {
  min-height: 132px;
  padding: 15px;
}

.ability-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  margin-bottom: 12px;
  border-radius: 14px;
}

.ability-icon.green {
  color: #4FB783;
  background: #E5F6EE;
}

.ability-icon.cyan {
  color: #4FAAC4;
  background: #E4F3FB;
}

.ability-icon.orange {
  color: #B8862A;
  background: #FEF3E2;
}

.ability-icon.purple {
  color: #B07CD4;
  background: #F3EAF8;
}

.ability-card p {
  margin: 7px 0 0;
  color: var(--figma-text-muted);
  font-size: 11px;
  line-height: 1.65;
}

.notice-card {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-top: 14px;
  padding: 13px 14px;
  color: #4FB783;
  background: linear-gradient(145deg, #EDF8F4, #EAF5FA);
}

.notice-card p {
  margin: 0;
  color: var(--figma-text-secondary);
  font-size: 11px;
  line-height: 1.65;
}

.primary-button {
  width: 100%;
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  margin-top: 14px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 700;
  box-shadow: var(--figma-shadow-button);
}
</style>
