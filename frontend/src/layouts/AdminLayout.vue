<template>
  <div class="admin-layout">
    <aside>
      <strong>管理端</strong>
      <RouterLink to="/admin/dashboard">仪表盘</RouterLink>
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

.router-link-active {
  color: var(--color-primary);
}
</style>
