<template>
  <div class="image-uploader">
    <div
      class="image-uploader__dropzone"
      :class="{ 'image-uploader__dropzone--dragging': dragging, 'image-uploader__dropzone--busy': uploading }"
      role="button"
      tabindex="0"
      @click="openPicker"
      @keydown.enter.prevent="openPicker"
      @keydown.space.prevent="openPicker"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInput"
        class="image-uploader__input"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        @change="handleFileChange"
      />

      <div v-if="previewUrl" class="image-uploader__preview">
        <img :src="previewUrl" alt="图片预览" />
      </div>
      <div v-else class="image-uploader__empty">
        <ImagePlus />
        <strong>{{ title }}</strong>
        <span>{{ hint }}</span>
      </div>

      <div v-if="uploading" class="image-uploader__mask">
        <LoaderCircle class="image-uploader__spin" />
        <span>上传中</span>
      </div>
    </div>

    <div class="image-uploader__actions">
      <button type="button" class="image-uploader__action" :disabled="uploading" @click="openPicker">
        {{ previewUrl ? '替换图片' : '选择图片' }}
      </button>
      <button
        v-if="previewUrl"
        type="button"
        class="image-uploader__action image-uploader__action--danger"
        :disabled="uploading"
        @click="clearImage"
      >
        删除图片
      </button>
    </div>

    <p v-if="errorMessage" class="image-uploader__error">{{ errorMessage }}</p>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ImagePlus, LoaderCircle } from 'lucide-vue-next'
import { uploadImage } from '@/api/upload'
import { resolveAssetUrl } from '@/utils/assets'

const props = defineProps({
  modelValue: { type: String, default: '' },
  title: { type: String, default: '上传图片' },
  hint: { type: String, default: '点击选择或拖拽图片到这里，支持 JPG、PNG、WEBP，最大 5MB' },
  maxSizeMb: { type: Number, default: 5 }
})

const emit = defineEmits(['update:modelValue', 'uploaded', 'error'])

const fileInput = ref(null)
const dragging = ref(false)
const uploading = ref(false)
const localPreviewUrl = ref('')
const errorMessage = ref('')

const previewUrl = computed(() => localPreviewUrl.value || resolveAssetUrl(props.modelValue))

watch(() => props.modelValue, () => {
  revokeLocalPreview()
})

function openPicker() {
  if (!uploading.value) fileInput.value?.click()
}

function handleFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (file) upload(file)
}

function handleDrop(event) {
  dragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) upload(file)
}

async function upload(file) {
  const validation = validateFile(file)
  if (validation) {
    errorMessage.value = validation
    emit('error', validation)
    return
  }

  errorMessage.value = ''
  setLocalPreview(file)
  uploading.value = true
  try {
    const result = await uploadImage(file)
    const url = result?.url || result?.path || ''
    emit('update:modelValue', url)
    emit('uploaded', result)
    revokeLocalPreview()
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || '图片上传失败，请稍后重试'
    emit('error', errorMessage.value)
  } finally {
    uploading.value = false
  }
}

function validateFile(file) {
  if (!file) return '请选择要上传的图片'
  const allowTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowTypes.includes(file.type)) return '仅支持 JPG、PNG、WEBP 格式图片'
  if (file.size > props.maxSizeMb * 1024 * 1024) return `图片大小不能超过 ${props.maxSizeMb}MB`
  return ''
}

function setLocalPreview(file) {
  revokeLocalPreview()
  localPreviewUrl.value = URL.createObjectURL(file)
}

function revokeLocalPreview() {
  if (localPreviewUrl.value) {
    URL.revokeObjectURL(localPreviewUrl.value)
    localPreviewUrl.value = ''
  }
}

function clearImage() {
  revokeLocalPreview()
  errorMessage.value = ''
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-uploader {
  width: 100%;
}

.image-uploader__dropzone {
  position: relative;
  min-height: 164px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1.5px dashed rgba(174, 232, 199, 0.72);
  border-radius: 18px;
  background: linear-gradient(145deg, #F7FCF9, #EEF8F3);
  color: var(--figma-text-muted, #7A8794);
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.image-uploader__dropzone--dragging {
  border-color: #7FD5B2;
  background: #ECF8F2;
}

.image-uploader__dropzone--busy {
  cursor: wait;
}

.image-uploader__input {
  display: none;
}

.image-uploader__preview,
.image-uploader__preview img {
  width: 100%;
  height: 100%;
}

.image-uploader__preview {
  position: absolute;
  inset: 0;
}

.image-uploader__preview img {
  object-fit: cover;
}

.image-uploader__empty {
  padding: 22px 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
}

.image-uploader__empty svg {
  width: 30px;
  height: 30px;
  color: #5BBF8A;
}

.image-uploader__empty strong {
  color: var(--figma-text-strong, #24323D);
  font-size: 14px;
}

.image-uploader__empty span,
.image-uploader__error {
  font-size: 12px;
  line-height: 1.6;
}

.image-uploader__actions {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.image-uploader__action {
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(174, 232, 199, 0.30);
  color: #4A8A6A;
  font-size: 12px;
  font-weight: 600;
}

.image-uploader__action--danger {
  background: rgba(239, 143, 143, 0.12);
  color: #E87878;
}

.image-uploader__action:disabled {
  opacity: 0.65;
}

.image-uploader__error {
  margin: 8px 0 0;
  color: #E87878;
}

.image-uploader__mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.72);
  color: #4A8A6A;
  font-size: 13px;
  font-weight: 600;
  backdrop-filter: blur(4px);
}

.image-uploader__spin {
  width: 22px;
  height: 22px;
  animation: image-uploader-spin 0.8s linear infinite;
}

@keyframes image-uploader-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
