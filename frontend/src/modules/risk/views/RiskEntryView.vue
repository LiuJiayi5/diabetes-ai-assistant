<template>
  <div class="figma-page">
    <PageHeader title="糖尿病风险预测" :show-back="false" />

    <FigmaCard variant="gradient">
      <p class="risk-hero__eyebrow">基于档案与最新健康数据</p>
      <h2 class="risk-hero__title">智能风险评估</h2>
      <p class="risk-hero__summary">{{ latestMetricSummary }}</p>
      <button class="figma-btn-primary risk-hero__btn" type="button" @click="metricPopup = true">
        录入今日数据
      </button>
    </FigmaCard>

    <FigmaCard v-if="!entry?.can_predict" variant="soft">
      <p class="figma-page__hint">{{ entry?.missing_reason || '请先完善健康档案并录入健康数据。' }}</p>
      <button
        v-if="!hasProfile"
        class="figma-btn-secondary"
        type="button"
        style="margin-top: 12px"
        @click="router.push('/app/profile')"
      >
        去填写健康档案
      </button>
    </FigmaCard>

    <template v-if="latestAssessment">
      <h2 class="figma-page__section-title">最新评估</h2>
      <FigmaCard>
        <div class="risk-result__head">
          <RiskLevelTag :level="latestAssessment.risk_level" />
          <span v-if="latestAssessment.risk_score != null" class="risk-result__score">
            评分 {{ latestAssessment.risk_score }}
          </span>
        </div>
        <p class="risk-result__summary">{{ latestAssessment.summary || latestAssessment.request_summary }}</p>
        <p v-if="latestAssessment.call_status === 'failed'" class="risk-result__error">
          {{ latestAssessment.error_message || 'AI 服务暂不可用' }}
        </p>
        <button
          v-if="latestAssessment.assessment_id"
          class="figma-btn-secondary"
          type="button"
          style="margin-top: 12px"
          @click="router.push(`/app/risk/${latestAssessment.assessment_id}`)"
        >
          查看完整报告
        </button>
      </FigmaCard>
    </template>

    <button
      v-if="entry?.can_predict"
      class="figma-btn-primary"
      type="button"
      :disabled="predicting"
      style="margin-top: 4px"
      @click="handlePredict"
    >
      {{ predicting ? '评估中...' : '重新发起评估' }}
    </button>

    <button class="figma-btn-secondary" type="button" style="margin-top: 10px" @click="router.push('/app/risk/history')">
      查看评估记录
    </button>

    <p class="figma-disclaimer">AI 建议仅供参考，不能替代线下诊疗。</p>

    <van-popup v-model:show="metricPopup" position="bottom" round :style="{ maxHeight: '88%' }">
      <div class="figma-popup">
        <h3 class="figma-popup__title">录入今日健康数据</h3>
        <van-form @submit="handleSaveMetric">
          <van-field v-model="metricForm.recorded_at" label="记录日期" placeholder="YYYY-MM-DD" required />
          <van-field v-model="metricForm.weight_kg" type="number" label="体重(kg)" placeholder="如 68.5" />
          <van-field v-model="metricForm.waist_cm" type="number" label="腰围(cm)" placeholder="如 85" />
          <van-field v-model="metricForm.systolic_bp" type="digit" label="收缩压" placeholder="如 120" />
          <van-field v-model="metricForm.diastolic_bp" type="digit" label="舒张压" placeholder="如 80" />
          <van-field v-model="metricForm.fasting_glucose" type="number" label="空腹血糖" placeholder="mmol/L" />
          <van-field v-model="metricForm.postprandial_glucose" type="number" label="餐后血糖" placeholder="mmol/L" />
          <van-field v-model="metricForm.hba1c" type="number" label="糖化血红蛋白" placeholder="%" />
          <van-field v-model="metricForm.diet_status" label="饮食状态" placeholder="如：主食偏多" />
          <van-field v-model="metricForm.exercise_status" label="运动状态" placeholder="如：每周运动2次" />
          <div style="margin-top: 16px">
            <button class="figma-btn-primary" type="submit" :disabled="savingMetric">
              {{ savingMetric ? '保存中...' : '保存并评估' }}
            </button>
          </div>
        </van-form>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Form as VanForm, Field as VanField, Popup as VanPopup, showToast } from 'vant'
import PageHeader from '@/components/mobile/PageHeader.vue'
import FigmaCard from '@/components/mobile/FigmaCard.vue'
import RiskLevelTag from '@/components/mobile/RiskLevelTag.vue'
import { getMyProfile } from '@/api/profile'
import { getLatestMetric, saveMetric } from '@/api/healthMetric'
import { getRiskEntry, predictRisk } from '@/api/riskAssessment'
import { assertSuccess } from '@/utils/response'
import { buildMetricSummary, todayString } from '@/utils/health'

const router = useRouter()
const entry = ref(null)
const latestAssessment = ref(null)
const latestMetric = ref(null)
const hasProfile = ref(false)
const metricPopup = ref(false)
const savingMetric = ref(false)
const predicting = ref(false)

const metricForm = reactive({
  recorded_at: todayString(),
  weight_kg: '',
  waist_cm: '',
  systolic_bp: '',
  diastolic_bp: '',
  fasting_glucose: '',
  postprandial_glucose: '',
  hba1c: '',
  diet_status: '',
  exercise_status: ''
})

const latestMetricSummary = computed(() => buildMetricSummary(latestMetric.value))

onMounted(loadPage)

async function loadPage() {
  try {
    const [entryData, metricData, profileData] = await Promise.all([
      getRiskEntry(),
      getLatestMetric(),
      getMyProfile()
    ])
    entry.value = assertSuccess(entryData)
    latestMetric.value = assertSuccess(metricData)
    const profile = assertSuccess(profileData)
    hasProfile.value = Boolean(profile)
    latestAssessment.value = entry.value?.latest_assessment || null
  } catch (error) {
    showToast(error.message || '加载失败')
  }
}

function buildMetricPayload() {
  const pickNumber = (value) => (value === '' || value == null ? null : Number(value))
  const pickInt = (value) => (value === '' || value == null ? null : parseInt(value, 10))
  return {
    recorded_at: metricForm.recorded_at,
    weight_kg: pickNumber(metricForm.weight_kg),
    waist_cm: pickNumber(metricForm.waist_cm),
    systolic_bp: pickInt(metricForm.systolic_bp),
    diastolic_bp: pickInt(metricForm.diastolic_bp),
    fasting_glucose: pickNumber(metricForm.fasting_glucose),
    postprandial_glucose: pickNumber(metricForm.postprandial_glucose),
    hba1c: pickNumber(metricForm.hba1c),
    diet_status: metricForm.diet_status || null,
    exercise_status: metricForm.exercise_status || null
  }
}

async function handleSaveMetric() {
  savingMetric.value = true
  try {
    assertSuccess(await saveMetric(buildMetricPayload()))
    metricPopup.value = false
    showToast('健康数据已保存')
    await loadPage()

    if (!hasProfile.value) {
      showToast('请先完善健康档案后再评估')
      return
    }

    await handlePredict()
  } catch (error) {
    showToast(error.message || '保存失败')
  } finally {
    savingMetric.value = false
  }
}

async function handlePredict() {
  predicting.value = true
  try {
    const result = assertSuccess(await predictRisk())
    latestAssessment.value = result
    showToast('评估完成')
    await loadPage()
  } catch (error) {
    if (error.code === 400) {
      showToast(error.message || '请先补齐档案和健康数据')
    } else if (error.code === 502) {
      showToast('AI 服务暂不可用，请稍后重试')
      await loadPage()
    } else {
      showToast(error.message || '评估失败')
    }
  } finally {
    predicting.value = false
  }
}
</script>

<style scoped>
.risk-hero__eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  color: var(--figma-text-muted);
}

.risk-hero__title {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--figma-text-strong);
}

.risk-hero__summary {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--figma-text-secondary);
}

.risk-hero__btn {
  margin-top: 4px;
}

.risk-result__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.risk-result__score {
  font-size: 13px;
  color: var(--figma-text-muted);
}

.risk-result__summary {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--figma-text-secondary);
}

.risk-result__error {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--figma-error);
}
</style>
