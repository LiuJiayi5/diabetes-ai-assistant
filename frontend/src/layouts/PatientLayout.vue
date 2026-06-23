<template>
  <div>
    <header class="layout-header">
      <strong>糖尿病预治智能助手</strong>
      <nav>
        <RouterLink to="/app/home">首页</RouterLink>
        <RouterLink to="/app/profile">档案</RouterLink>
        <RouterLink to="/app/risk">风险</RouterLink>
        <RouterLink to="/app/ai-chat">AI 医生</RouterLink>
        <RouterLink to="/app/checkin">打卡</RouterLink>
      </nav>
    </header>
    <RouterView />
  </div>
</template>

<style scoped>
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 24px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
}

nav {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--color-muted);
  font-size: 14px;
}

.router-link-active {
  color: var(--color-primary);
}
</style>
