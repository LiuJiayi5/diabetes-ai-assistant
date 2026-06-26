<template>
  <div class="module-entry">
    <div class="entry-panel">
      <h2>患者首页</h2>
      <p>查看健康资讯、生活方案和每日打卡入口，快速回到常用控糖功能。</p>
      <RouterLink class="entry-link" to="/app/checkin">今日打卡</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.entry-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  margin-top: 12px;
  padding: 0 16px;
  border-radius: 999px;
  background: var(--figma-secondary-green);
  color: var(--figma-tabbar-active);
  font-size: 14px;
  font-weight: 600;
}
</style>
