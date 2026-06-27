<template>
  <div class="admin-layout">
    <aside>
      <strong>管理端</strong>
      <RouterLink to="/admin/dashboard">仪表盘</RouterLink>
      <RouterLink to="/admin/profiles">健康档案</RouterLink>
      <RouterLink to="/admin/health-metrics">健康数据</RouterLink>
      <RouterLink to="/admin/risk-assessments">风险评估</RouterLink>
      <hr class="admin-nav-divider" />
      <RouterLink to="/admin/users">用户</RouterLink>
      <RouterLink to="/admin/articles">内容</RouterLink>
      <RouterLink to="/admin/checkins">打卡</RouterLink>
    </aside>
    <main>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 220px 1fr;
}

aside {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px;
  border-right: 1px solid var(--color-border);
  background: var(--color-surface);
}

aside a {
  color: var(--figma-text-secondary, #475569);
  text-decoration: none;
  font-size: 14px;
}

.router-link-active {
  color: var(--color-primary);
  font-weight: 600;
}

.admin-nav-divider {
  width: 100%;
  border: 0;
  border-top: 1px solid var(--color-border);
  margin: 4px 0;
}

main {
  background: #f5f7fa;
  min-width: 0;
}
</style>
