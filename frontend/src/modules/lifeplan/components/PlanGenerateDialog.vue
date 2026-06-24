<template>
  <div v-if="show" class="plan-dialog-overlay" @click.self="$emit('close')">
    <section class="plan-dialog">
      <header>
        <h2>生成生活方案</h2>
        <button type="button" @click="$emit('close')">
          <X />
        </button>
      </header>

      <div class="goal-options">
        <button
          v-for="goal in goalOptions"
          :key="goal"
          type="button"
          :class="{ active: form.plan_goal === goal }"
          @click="form.plan_goal = goal"
        >
          {{ goal }}
        </button>
      </div>

      <label class="plan-field">
        <span>方案目标</span>
        <input v-model.trim="form.plan_goal" type="text" placeholder="例如：控糖和减重" />
      </label>

      <label class="plan-field">
        <span>忌口或不适合项目</span>
        <input
          v-model.trim="avoidInput"
          type="text"
          placeholder="输入后按回车添加"
          @keydown.enter.prevent="addAvoidItem"
        />
      </label>

      <div class="avoid-tags">
        <button
          v-for="item in form.avoid_items"
          :key="item"
          type="button"
          @click="removeAvoidItem(item)"
        >
          {{ item }}
          <X />
        </button>
      </div>

      <p v-if="error" class="plan-dialog__error">{{ error }}</p>

      <button class="plan-dialog__submit" type="button" :disabled="generating" @click="submit">
        <LoaderCircle v-if="generating" class="spin" />
        {{ generating ? '生成中' : '生成方案' }}
      </button>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { showToast } from 'vant'
import { LoaderCircle, X } from 'lucide-vue-next'

defineProps({
  show: {
    type: Boolean,
    default: false
  },
  generating: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['close', 'submit'])

const goalOptions = ['控糖和减重', '改善饮食习惯', '增加运动', '稳定血糖', '综合生活干预']
const avoidInput = ref('')
const form = reactive({
  plan_goal: '控糖和减重',
  avoid_items: [],
  plan_days: 7
})

function addAvoidItem() {
  const value = avoidInput.value.trim()
  if (!value) return
  if (!form.avoid_items.includes(value)) {
    form.avoid_items.push(value)
  }
  avoidInput.value = ''
}

function removeAvoidItem(item) {
  form.avoid_items = form.avoid_items.filter((value) => value !== item)
}

function submit() {
  addAvoidItem()
  if (!form.plan_goal) {
    showToast('请填写方案目标')
    return
  }

  emit('submit', {
    plan_goal: form.plan_goal,
    avoid_items: [...form.avoid_items],
    plan_days: 7
  })
}
</script>
