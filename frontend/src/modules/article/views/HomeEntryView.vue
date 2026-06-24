<template>
  <div class="module-entry">
    <div class="entry-panel">
      <h2>患者首页</h2>
      <p>所属模块：内容模块</p>
      <p>后续接入：Spring Boot 内容接口、MySQL 首页内容数据。</p>
      <p>是否涉及 Dify：否。</p>
      <p>后续可扩展：首页轮播、推荐资讯、AI 医生入口卡片。</p>
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
