<template>
  <div class="figma-page profile-page">
    <PageHeader title="健康档案" back-to="/app/account" />

    <section v-if="profile && !editing" class="profile-hero">
      <span class="profile-hero__glow profile-hero__glow--green"></span>
      <span class="profile-hero__glow profile-hero__glow--blue"></span>
      <div class="profile-hero__top">
        <div class="profile-avatar">
          <UserRound :size="24" />
        </div>
        <button class="edit-pill" type="button" @click="editing = true">
          <PencilLine :size="14" />
          <span>编辑</span>
        </button>
      </div>
      <h2>我的健康画像</h2>
      <p>{{ profile.profile_summary || '已建立基础档案，可用于风险预测与生活方案。' }}</p>
      <div class="profile-hero__tags">
        <span v-if="profile.age">{{ profile.age }} 岁</span>
        <span>{{ formatGender(profile.gender) }}</span>
        <span v-if="profile.height_cm">{{ profile.height_cm }} cm</span>
      </div>
    </section>

    <template v-if="profile && !editing">
      <section class="metric-grid">
        <article class="metric-card">
          <span>基础体重</span>
          <strong>{{ profile.base_weight_kg || '—' }}</strong>
          <small>kg</small>
        </article>
        <article class="metric-card">
          <span>基础腰围</span>
          <strong>{{ profile.base_waist_cm || '—' }}</strong>
          <small>cm</small>
        </article>
        <article class="metric-card">
          <span>年龄</span>
          <strong>{{ profile.age || '—' }}</strong>
          <small>岁</small>
        </article>
        <article class="metric-card">
          <span>身高</span>
          <strong>{{ profile.height_cm || '—' }}</strong>
          <small>cm</small>
        </article>
      </section>

      <section class="history-list">
        <article class="history-card">
          <div class="history-card__icon">
            <UsersRound :size="18" />
          </div>
          <div>
            <h3>家族病史</h3>
            <p>{{ profile.family_history || '暂无记录' }}</p>
          </div>
        </article>
        <article class="history-card">
          <div class="history-card__icon history-card__icon--blue">
            <HeartPulse :size="18" />
          </div>
          <div>
            <h3>慢病史</h3>
            <p>{{ profile.chronic_history || '暂无记录' }}</p>
          </div>
        </article>
        <article class="history-card">
          <div class="history-card__icon history-card__icon--amber">
            <ShieldAlert :size="18" />
          </div>
          <div>
            <h3>过敏史</h3>
            <p>{{ profile.allergy_history || '暂无记录' }}</p>
          </div>
        </article>
      </section>
    </template>

    <section v-else class="profile-form-card">
      <div class="form-card__head">
        <div>
          <p class="eyebrow">{{ profile ? '更新档案' : '首次建档' }}</p>
          <h2>{{ profile ? '完善健康画像' : '建立健康画像' }}</h2>
        </div>
        <button v-if="profile" class="cancel-pill" type="button" @click="cancelEdit">取消</button>
      </div>
      <p v-if="!profile && !loading" class="form-intro">
        健康档案是长期健康画像，填写后可用于风险预测与生活方案。
      </p>

      <van-form class="profile-form" @submit="handleSubmit">
        <div class="form-section">
          <p class="form-section__title">基础信息</p>
          <van-field v-model.number="form.age" type="digit" label="年龄" placeholder="请输入年龄" required />
          <van-field label="性别" required>
            <template #input>
              <van-radio-group v-model="form.gender" direction="horizontal">
                <van-radio name="male">男</van-radio>
                <van-radio name="female">女</van-radio>
                <van-radio name="other">其他</van-radio>
              </van-radio-group>
            </template>
          </van-field>
          <van-field v-model="form.height_cm" type="number" label="身高(cm)" placeholder="如 170" required />
          <van-field v-model="form.base_weight_kg" type="number" label="基础体重(kg)" placeholder="如 65" required />
          <van-field v-model="form.base_waist_cm" type="number" label="基础腰围(cm)" placeholder="选填" />
        </div>

        <div class="form-section">
          <p class="form-section__title">病史信息</p>
          <van-field v-model="form.family_history" label="家族病史" type="textarea" rows="2" placeholder="如：父亲有糖尿病" />
          <van-field v-model="form.chronic_history" label="慢病史" type="textarea" rows="2" placeholder="选填" />
          <van-field v-model="form.allergy_history" label="过敏史" type="textarea" rows="2" placeholder="选填" />
        </div>

        <button class="submit-button" type="submit" :disabled="saving">
          <LoaderCircle v-if="saving" class="spin" :size="17" />
          <CheckCircle2 v-else :size="17" />
          <span>{{ saving ? '保存中...' : '保存档案' }}</span>
        </button>
      </van-form>
    </section>

    <p class="figma-disclaimer">档案信息将用于 AI 风险评估参考，不能替代线下诊疗。</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Form as VanForm, Field as VanField, RadioGroup as VanRadioGroup, Radio as VanRadio, showToast } from 'vant'
import {
  CheckCircle2,
  HeartPulse,
  LoaderCircle,
  PencilLine,
  ShieldAlert,
  UserRound,
  UsersRound
} from 'lucide-vue-next'
import PageHeader from '@/components/mobile/PageHeader.vue'
import { getMyProfile, saveProfile } from '@/api/profile'
import { assertSuccess } from '@/utils/response'
import { formatGender } from '@/utils/health'

const loading = ref(true)
const saving = ref(false)
const editing = ref(false)
const profile = ref(null)
const form = reactive({
  age: '',
  gender: 'male',
  height_cm: '',
  base_weight_kg: '',
  base_waist_cm: '',
  family_history: '',
  chronic_history: '',
  allergy_history: ''
})

onMounted(loadProfile)

async function loadProfile() {
  loading.value = true
  try {
    const data = assertSuccess(await getMyProfile())
    profile.value = data
    if (data) {
      fillForm(data)
      editing.value = false
    } else {
      editing.value = true
    }
  } catch (error) {
    showToast(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function fillForm(data) {
  form.age = data.age
  form.gender = data.gender || 'male'
  form.height_cm = data.height_cm
  form.base_weight_kg = data.base_weight_kg
  form.base_waist_cm = data.base_waist_cm || ''
  form.family_history = data.family_history || ''
  form.chronic_history = data.chronic_history || ''
  form.allergy_history = data.allergy_history || ''
}

function cancelEdit() {
  if (profile.value) {
    fillForm(profile.value)
    editing.value = false
  }
}

async function handleSubmit() {
  if (!form.age || Number(form.age) < 1 || Number(form.age) > 120) {
    showToast('请输入 1-120 岁之间的年龄')
    return
  }
  if (!form.height_cm || Number(form.height_cm) < 50 || Number(form.height_cm) > 250) {
    showToast('请输入 50-250cm 之间的身高')
    return
  }
  if (!form.base_weight_kg || Number(form.base_weight_kg) < 20 || Number(form.base_weight_kg) > 300) {
    showToast('请输入 20-300kg 之间的基础体重')
    return
  }
  if (form.base_waist_cm && (Number(form.base_waist_cm) < 30 || Number(form.base_waist_cm) > 200)) {
    showToast('请输入 30-200cm 之间的基础腰围')
    return
  }
  saving.value = true
  try {
    const payload = {
      age: Number(form.age),
      gender: form.gender,
      height_cm: Number(form.height_cm),
      base_weight_kg: Number(form.base_weight_kg),
      base_waist_cm: form.base_waist_cm ? Number(form.base_waist_cm) : null,
      family_history: form.family_history || null,
      chronic_history: form.chronic_history || null,
      allergy_history: form.allergy_history || null
    }
    assertSuccess(await saveProfile(payload))
    showToast('档案已保存')
    await loadProfile()
  } catch (error) {
    showToast(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.profile-page {
  padding-bottom: calc(90px + env(safe-area-inset-bottom));
}

.profile-hero,
.metric-card,
.history-card,
.profile-form-card {
  box-shadow: var(--figma-shadow-card);
}

.profile-hero {
  position: relative;
  overflow: hidden;
  margin-bottom: 14px;
  padding: 20px;
  border-radius: var(--figma-radius-card-lg);
  background: linear-gradient(145deg, #FFFFFF 0%, #EEF8F3 58%, #EAF5FA 100%);
}

.profile-hero__glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(22px);
  pointer-events: none;
}

.profile-hero__glow--green {
  top: -38px;
  right: 58px;
  width: 142px;
  height: 142px;
  background: rgba(174, 232, 199, 0.23);
}

.profile-hero__glow--blue {
  right: -30px;
  bottom: -36px;
  width: 150px;
  height: 150px;
  background: rgba(191, 233, 242, 0.25);
}

.profile-hero__top,
.profile-hero h2,
.profile-hero p,
.profile-hero__tags {
  position: relative;
  z-index: 1;
}

.profile-hero__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.profile-avatar {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.64);
  color: #4FB783;
}

.edit-pill,
.cancel-pill {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 0 12px;
  border-radius: var(--figma-radius-pill);
  background: rgba(174, 232, 199, 0.30);
  color: #4A8A6A;
  font-size: 12px;
  font-weight: 700;
}

.profile-hero h2 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 21px;
  font-weight: 800;
  line-height: 1.35;
}

.profile-hero p {
  margin: 9px 0 0;
  color: var(--figma-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.profile-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.profile-hero__tags span {
  padding: 6px 10px;
  border-radius: var(--figma-radius-pill);
  background: rgba(255, 255, 255, 0.58);
  color: #4A7A62;
  font-size: 11px;
  font-weight: 700;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.metric-card {
  min-height: 92px;
  padding: 14px;
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
}

.metric-card span {
  display: block;
  color: var(--figma-text-muted);
  font-size: 11px;
  font-weight: 600;
}

.metric-card strong {
  display: inline-block;
  margin-top: 10px;
  color: var(--figma-text-strong);
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

.metric-card small {
  margin-left: 4px;
  color: var(--figma-text-faint);
  font-size: 11px;
}

.history-list {
  display: grid;
  gap: 10px;
}

.history-card {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 11px;
  padding: 15px;
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
}

.history-card__icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: #E5F6EE;
  color: #4FB783;
}

.history-card__icon--blue {
  background: #E4F3FB;
  color: #4FAAC4;
}

.history-card__icon--amber {
  background: #F8EED8;
  color: #C48A32;
}

.history-card h3 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 14px;
  font-weight: 700;
}

.history-card p {
  margin: 5px 0 0;
  color: var(--figma-text-secondary);
  font-size: 13px;
  font-weight: 400;
  line-height: 1.7;
}

.profile-form-card {
  padding: 16px;
  border-radius: var(--figma-radius-card);
  background: #FFFFFF;
}

.form-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}

.eyebrow {
  margin: 0 0 6px;
  color: rgba(36, 50, 61, 0.62);
  font-size: 11px;
  font-weight: 600;
}

.form-card__head h2 {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 18px;
  font-weight: 800;
}

.form-intro {
  margin: 0 0 12px;
  color: var(--figma-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.profile-form {
  display: grid;
  gap: 12px;
}

.form-section {
  display: grid;
  gap: 9px;
}

.form-section__title {
  margin: 0;
  color: var(--figma-text-strong);
  font-size: 14px;
  font-weight: 700;
}

.submit-button {
  min-height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: var(--figma-radius-pill);
  background: var(--figma-green-button);
  color: #FFFFFF;
  font-size: 14px;
  font-weight: 700;
  box-shadow: var(--figma-shadow-button);
}

.submit-button:disabled {
  opacity: 0.72;
}

:deep(.van-cell) {
  border-radius: 16px;
  background: #F7FCF9;
  color: var(--figma-text-strong);
}

:deep(.van-cell::after) {
  display: none;
}

:deep(.van-field__label) {
  color: var(--figma-text-secondary);
  font-size: 12px;
  font-weight: 500;
}

:deep(.van-field__control) {
  color: var(--figma-text-strong);
  font-size: 13px;
}

:deep(.van-radio__label) {
  color: var(--figma-text-secondary);
  font-size: 13px;
}

.spin {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
