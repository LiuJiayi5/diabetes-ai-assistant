<template>
  <div class="figma-page">
    <PageHeader title="健康档案" back-to="/app/account" />

    <FigmaCard v-if="profile && !editing" variant="gradient">
      <p class="profile-hero__title">我的健康画像</p>
      <p class="profile-hero__summary">{{ profile.profile_summary || '已建立基础档案' }}</p>
      <div class="profile-hero__tags">
        <span v-if="profile.age">{{ profile.age }} 岁</span>
        <span>{{ formatGender(profile.gender) }}</span>
        <span v-if="profile.height_cm">{{ profile.height_cm }} cm</span>
      </div>
      <button class="figma-btn-secondary profile-hero__btn" type="button" @click="editing = true">
        编辑档案
      </button>
    </FigmaCard>

    <FigmaCard>
      <p v-if="!profile && !loading" class="figma-page__hint">
        健康档案是你的长期健康画像，填写后可用于风险预测与生活方案。
      </p>

      <van-form v-if="editing || !profile" @submit="handleSubmit">
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
        <van-field v-model="form.family_history" label="家族病史" type="textarea" rows="2" placeholder="如：父亲有糖尿病" />
        <van-field v-model="form.chronic_history" label="慢病史" type="textarea" rows="2" placeholder="选填" />
        <van-field v-model="form.allergy_history" label="过敏史" type="textarea" rows="2" placeholder="选填" />
        <div style="margin-top: 16px">
          <button class="figma-btn-primary" type="submit" :disabled="saving">
            {{ saving ? '保存中...' : '保存档案' }}
          </button>
        </div>
      </van-form>

      <div v-else class="profile-detail">
        <div class="figma-metric-row"><span class="figma-metric-row__label">基础体重</span><span class="figma-metric-row__value">{{ profile.base_weight_kg }} kg</span></div>
        <div class="figma-metric-row"><span class="figma-metric-row__label">基础腰围</span><span class="figma-metric-row__value">{{ profile.base_waist_cm || '—' }} cm</span></div>
        <div class="figma-metric-row"><span class="figma-metric-row__label">家族病史</span><span class="figma-metric-row__value">{{ profile.family_history || '—' }}</span></div>
        <div class="figma-metric-row"><span class="figma-metric-row__label">慢病史</span><span class="figma-metric-row__value">{{ profile.chronic_history || '—' }}</span></div>
        <div class="figma-metric-row"><span class="figma-metric-row__label">过敏史</span><span class="figma-metric-row__value">{{ profile.allergy_history || '—' }}</span></div>
      </div>
    </FigmaCard>

    <p class="figma-disclaimer">档案信息将用于 AI 风险评估参考，不能替代线下诊疗。</p>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Form as VanForm, Field as VanField, RadioGroup as VanRadioGroup, Radio as VanRadio, showToast } from 'vant'
import PageHeader from '@/components/mobile/PageHeader.vue'
import FigmaCard from '@/components/mobile/FigmaCard.vue'
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

async function handleSubmit() {
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
.profile-hero__title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
  color: var(--figma-text-strong);
}

.profile-hero__summary {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--figma-text-secondary);
}

.profile-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.profile-hero__tags span {
  padding: 4px 10px;
  border-radius: var(--figma-radius-pill);
  background: rgba(255, 255, 255, 0.55);
  font-size: 12px;
  color: var(--figma-text-strong);
}

.profile-hero__btn {
  width: auto;
  min-width: 120px;
  padding: 0 18px;
}
</style>
