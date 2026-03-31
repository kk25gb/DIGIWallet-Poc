<template>
  <div class="dashboard">
    <el-card>
      <template #header>
        <h3>Welcome to Issuer AP System</h3>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="Username">
          {{ authStore.account?.username ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="Roles">
          <el-tag
            v-for="auth in authStore.account?.authorities"
            :key="auth"
            style="margin-right: 6px"
          >
            {{ auth }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<style scoped>
.dashboard {
  max-width: 600px;
}
</style>
