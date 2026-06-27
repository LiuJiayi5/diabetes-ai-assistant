<template>
  <div class="markdown-content" v-html="safeHtml"></div>
</template>

<script setup>
import { computed } from 'vue'
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: false
})

const safeHtml = computed(() => DOMPurify.sanitize(markdown.render(props.content || '')))
</script>

<style scoped>
.markdown-content {
  color: inherit;
  font-size: 13px;
  line-height: 1.72;
}

.markdown-content :deep(*) {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.markdown-content :deep(p),
.markdown-content :deep(ul),
.markdown-content :deep(ol),
.markdown-content :deep(blockquote),
.markdown-content :deep(table) {
  margin: 0 0 8px;
}

.markdown-content :deep(*:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4) {
  margin: 10px 0 6px;
  color: var(--figma-text-strong, #1f2937);
  font-weight: 700;
  line-height: 1.35;
}

.markdown-content :deep(h1) {
  font-size: 16px;
}

.markdown-content :deep(h2) {
  font-size: 15px;
}

.markdown-content :deep(h3),
.markdown-content :deep(h4) {
  font-size: 14px;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 18px;
}

.markdown-content :deep(li + li) {
  margin-top: 4px;
}

.markdown-content :deep(strong) {
  color: var(--figma-text-strong, #1f2937);
  font-weight: 700;
}

.markdown-content :deep(blockquote) {
  padding: 7px 10px;
  border-left: 3px solid rgba(79, 183, 131, 0.45);
  border-radius: 0 8px 8px 0;
  background: rgba(229, 246, 238, 0.55);
  color: var(--figma-text-secondary, #4b5563);
}

.markdown-content :deep(code) {
  padding: 1px 4px;
  border-radius: 4px;
  background: rgba(15, 23, 42, 0.06);
  color: #3f6f5a;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
}

.markdown-content :deep(pre) {
  max-width: 100%;
  margin: 8px 0;
  padding: 9px 10px;
  overflow-x: auto;
  border-radius: 8px;
  background: #f6faf8;
}

.markdown-content :deep(pre code) {
  padding: 0;
  background: transparent;
}

.markdown-content :deep(table) {
  display: block;
  width: 100%;
  overflow-x: auto;
  border-collapse: collapse;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 6px 8px;
  border: 1px solid rgba(174, 232, 199, 0.65);
  text-align: left;
  vertical-align: top;
}

.markdown-content :deep(th) {
  background: rgba(229, 246, 238, 0.65);
  color: var(--figma-text-strong, #1f2937);
  font-weight: 700;
}

.markdown-content :deep(a) {
  color: #3f9d70;
  font-weight: 600;
  text-decoration: none;
}
</style>
