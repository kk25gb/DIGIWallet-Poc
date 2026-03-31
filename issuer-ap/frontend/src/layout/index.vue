<template>
  <el-container class="layout-container">
    <!-- Sidebar -->
    <el-aside width="220px">
      <div class="sidebar-title">Issuer AP</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>Dashboard</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/did/register">
          <el-icon><Key /></el-icon>
          <span>DID Register</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/settings">
          <el-icon><Setting /></el-icon>
          <span>Settings</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/credential-types">
          <el-icon><Document /></el-icon>
          <span>Credential Types</span>
        </el-menu-item>
        <el-menu-item v-if="authStore.isAdmin" index="/issue">
          <el-icon><Tickets /></el-icon>
          <span>Issue Credential</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- Main Area -->
    <el-container>
      <!-- Header -->
      <el-header class="layout-header">
        <div class="header-right">
          <span class="username">{{ authStore.account?.username }}</span>
          <el-tag v-if="authStore.isAdmin" type="danger" size="small">ADMIN</el-tag>
          <el-tag v-if="authStore.isOperator" type="warning" size="small">OPERATOR</el-tag>
          <el-button type="text" @click="handleLogout">Logout</el-button>
        </div>
      </el-header>

      <!-- Content -->
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { HomeFilled, Key, Setting, Document, Tickets } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

onMounted(async () => {
  if (authStore.isAuthenticated && !authStore.account) {
    await authStore.fetchAccount()
  }
})

function handleLogout() {
  authStore.logout()
  router.push({ name: 'Login' })
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.el-aside {
  background-color: #304156;
}

.sidebar-title {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  border-bottom: 1px solid #3a4a5e;
}

.layout-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  font-weight: 500;
  color: #303133;
}
</style>
