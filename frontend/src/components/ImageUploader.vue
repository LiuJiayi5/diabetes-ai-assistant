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
        <span>上传中...</span>
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

    <div v-if="cropperOpen" class="avatar-cropper-overlay" @click.self="cancelCrop">
      <section class="avatar-cropper-dialog" role="dialog" aria-modal="true" aria-label="调整头像位置">
        <header>
          <div>
            <h2>调整头像位置</h2>
            <p>拖动图片选择显示区域，也可以缩放到合适大小。</p>
          </div>
          <button type="button" class="avatar-cropper-close" :disabled="uploading" @click="cancelCrop">取消</button>
        </header>

        <div
          ref="cropArea"
          class="avatar-cropper-stage"
          @pointerdown.prevent="startDrag"
          @pointermove.prevent="moveDrag"
          @pointerup.prevent="endDrag"
          @pointercancel.prevent="endDrag"
          @wheel.prevent="handleWheel"
        >
          <img
            v-if="cropSourceUrl"
            class="avatar-cropper-image"
            :src="cropSourceUrl"
            alt="头像裁剪预览"
            :style="cropImageStyle"
            draggable="false"
            @load="handleCropImageLoad"
          />
          <div class="avatar-cropper-frame" />
        </div>

        <label class="avatar-cropper-zoom">
          <span>缩放</span>
          <input v-model.number="cropScale" type="range" min="1" max="3" step="0.05" />
        </label>

        <div class="avatar-cropper-actions">
          <button type="button" class="image-uploader__action" :disabled="uploading" @click="resetCrop">居中</button>
          <button type="button" class="image-uploader__action image-uploader__action--danger" :disabled="uploading" @click="cancelCrop">取消</button>
          <button type="button" class="avatar-cropper-confirm" :disabled="uploading" @click="confirmCrop">
            <LoaderCircle v-if="uploading" class="image-uploader__spin" />
            {{ uploading ? '上传中...' : '确认使用' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { ImagePlus, LoaderCircle } from 'lucide-vue-next'
import { uploadImage } from '@/api/upload'
import { resolveAssetUrl } from '@/utils/assets'

const props = defineProps({
  modelValue: { type: String, default: '' },
  title: { type: String, default: '上传图片' },
  hint: { type: String, default: '点击选择或拖拽图片到这里，支持 JPG、PNG、WEBP，最大 5MB' },
  maxSizeMb: { type: Number, default: 5 },
  avatarCrop: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue', 'uploaded', 'error'])

const fileInput = ref(null)
const dragging = ref(false)
const uploading = ref(false)
const localPreviewUrl = ref('')
const errorMessage = ref('')
const cropperOpen = ref(false)
const cropSourceUrl = ref('')
const cropSourceFile = ref(null)
const cropArea = ref(null)
const cropScale = ref(1)
const cropOffset = ref({ x: 0, y: 0 })
const cropImageNatural = ref({ width: 1, height: 1 })
const dragState = ref({ active: false, startX: 0, startY: 0, originX: 0, originY: 0 })

const previewUrl = computed(() => localPreviewUrl.value || resolveAssetUrl(props.modelValue))
const cropImageStyle = computed(() => ({
  transform: `translate(-50%, -50%) translate(${cropOffset.value.x}px, ${cropOffset.value.y}px) scale(${cropScale.value})`
}))

watch(() => props.modelValue, () => {
  revokeLocalPreview()
})

function openPicker() {
  if (!uploading.value) fileInput.value?.click()
}

function handleFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (file) handleSelectedFile(file)
}

function handleDrop(event) {
  dragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) handleSelectedFile(file)
}

function handleSelectedFile(file) {
  const validation = validateFile(file)
  if (validation) {
    errorMessage.value = validation
    emit('error', validation)
    return
  }

  if (props.avatarCrop) {
    openCropper(file)
    return
  }

  upload(file)
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
    revokeLocalPreview()
    errorMessage.value = error?.response?.data?.message || '图片上传失败，请稍后重试'
    emit('error', errorMessage.value)
  } finally {
    uploading.value = false
  }
}

function openCropper(file) {
  errorMessage.value = ''
  cleanupCropSource()
  cropSourceFile.value = file
  cropSourceUrl.value = URL.createObjectURL(file)
  cropperOpen.value = true
  resetCrop()
}

function handleCropImageLoad(event) {
  cropImageNatural.value = {
    width: event.target.naturalWidth || 1,
    height: event.target.naturalHeight || 1
  }
  nextTick(resetCrop)
}

function resetCrop() {
  cropScale.value = 1
  cropOffset.value = { x: 0, y: 0 }
}

function startDrag(event) {
  if (!cropperOpen.value || uploading.value) return
  event.currentTarget.setPointerCapture?.(event.pointerId)
  dragState.value = {
    active: true,
    startX: event.clientX,
    startY: event.clientY,
    originX: cropOffset.value.x,
    originY: cropOffset.value.y
  }
}

function moveDrag(event) {
  if (!dragState.value.active) return
  cropOffset.value = {
    x: dragState.value.originX + event.clientX - dragState.value.startX,
    y: dragState.value.originY + event.clientY - dragState.value.startY
  }
}

function endDrag(event) {
  dragState.value.active = false
  event.currentTarget?.releasePointerCapture?.(event.pointerId)
}

function handleWheel(event) {
  const next = cropScale.value + (event.deltaY > 0 ? -0.06 : 0.06)
  cropScale.value = Math.min(3, Math.max(1, Number(next.toFixed(2))))
}

async function confirmCrop() {
  if (!cropSourceFile.value || !cropSourceUrl.value) return
  uploading.value = true
  errorMessage.value = ''

  try {
    const file = await createCroppedAvatarFile()
    setLocalPreview(file)
    const result = await uploadImage(file)
    const url = result?.url || result?.path || ''
    emit('update:modelValue', url)
    emit('uploaded', result)
    closeCropper()
    revokeLocalPreview()
  } catch (error) {
    errorMessage.value = error?.response?.data?.message || '头像处理失败，请换一张图片重试'
    emit('error', errorMessage.value)
  } finally {
    uploading.value = false
  }
}

function createCroppedAvatarFile() {
  return new Promise((resolve, reject) => {
    const source = new Image()
    source.onload = () => {
      const outputSize = 512
      const stageSize = cropArea.value?.clientWidth || 260
      const coverScale = Math.max(stageSize / source.naturalWidth, stageSize / source.naturalHeight)
      const displayWidth = source.naturalWidth * coverScale * cropScale.value
      const displayHeight = source.naturalHeight * coverScale * cropScale.value
      const sx = (stageSize - displayWidth) / 2 + cropOffset.value.x
      const sy = (stageSize - displayHeight) / 2 + cropOffset.value.y
      const outputScale = outputSize / stageSize
      const canvas = document.createElement('canvas')
      canvas.width = outputSize
      canvas.height = outputSize
      const ctx = canvas.getContext('2d')
      if (!ctx) {
        reject(new Error('canvas unavailable'))
        return
      }
      ctx.fillStyle = '#EEF8F3'
      ctx.fillRect(0, 0, outputSize, outputSize)
      ctx.drawImage(source, sx * outputScale, sy * outputScale, displayWidth * outputScale, displayHeight * outputScale)
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error('crop failed'))
          return
        }
        const fileName = cropSourceFile.value?.name?.replace(/\.[^.]+$/, '') || 'avatar'
        resolve(new File([blob], `${fileName}-avatar.webp`, { type: 'image/webp' }))
      }, 'image/webp', 0.92)
    }
    source.onerror = () => reject(new Error('image load failed'))
    source.src = cropSourceUrl.value
  })
}

function cancelCrop() {
  if (uploading.value) return
  closeCropper()
}

function closeCropper() {
  cropperOpen.value = false
  cropSourceFile.value = null
  cleanupCropSource()
  resetCrop()
}

function cleanupCropSource() {
  if (cropSourceUrl.value) {
    URL.revokeObjectURL(cropSourceUrl.value)
    cropSourceUrl.value = ''
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
  closeCropper()
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

.avatar-cropper-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 22px;
  background: rgba(36, 50, 61, 0.36);
  backdrop-filter: blur(6px);
}

.avatar-cropper-dialog {
  width: min(360px, 100%);
  padding: 18px;
  border-radius: 24px;
  background: #FFFFFF;
  box-shadow: 0 24px 70px rgba(36, 50, 61, 0.22);
}

.avatar-cropper-dialog header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.avatar-cropper-dialog h2 {
  margin: 0;
  color: var(--figma-text-strong, #24323D);
  font-size: 17px;
  font-weight: 800;
}

.avatar-cropper-dialog p {
  margin: 6px 0 0;
  color: var(--figma-text-muted, #7A8794);
  font-size: 12px;
  line-height: 1.6;
}

.avatar-cropper-close {
  flex-shrink: 0;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(174, 232, 199, 0.30);
  color: #4A8A6A;
  font-size: 12px;
  font-weight: 700;
}

.avatar-cropper-stage {
  position: relative;
  width: min(280px, 100%);
  aspect-ratio: 1;
  margin: 18px auto 14px;
  overflow: hidden;
  border-radius: 999px;
  background: linear-gradient(145deg, #EEF8F3, #E4F3FB);
  touch-action: none;
  cursor: grab;
  user-select: none;
}

.avatar-cropper-stage:active {
  cursor: grabbing;
}

.avatar-cropper-image {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform-origin: center;
  will-change: transform;
}

.avatar-cropper-frame {
  position: absolute;
  inset: 0;
  border: 2px solid rgba(255, 255, 255, 0.95);
  border-radius: 999px;
  box-shadow: inset 0 0 0 1px rgba(74, 138, 106, 0.20);
  pointer-events: none;
}

.avatar-cropper-zoom {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  color: var(--figma-text-strong, #24323D);
  font-size: 13px;
  font-weight: 700;
}

.avatar-cropper-zoom input {
  width: 100%;
  accent-color: #5BBF8A;
}

.avatar-cropper-actions {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.avatar-cropper-confirm {
  min-height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 15px;
  border-radius: 999px;
  background: var(--figma-green-button, #5BBF8A);
  color: #FFFFFF;
  font-size: 12px;
  font-weight: 700;
  box-shadow: var(--figma-shadow-button, 0 10px 24px rgba(91, 191, 138, 0.24));
}

.avatar-cropper-confirm:disabled,
.avatar-cropper-close:disabled {
  opacity: 0.65;
}
</style>
