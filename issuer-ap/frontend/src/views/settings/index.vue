<template>
  <div v-if="authStore.isAdmin" class="settings-page">
    <el-card>
      <template #header>
        <h3>System Settings</h3>
      </template>

      <el-button type="primary" :loading="loading" @click="loadSettings">
        Refresh Settings
      </el-button>

      <el-table v-if="settings.length" :data="settings" style="margin-top: 16px" stripe>
        <el-table-column prop="name" label="Setting" width="300" />
        <el-table-column prop="preloaded" label="Preloaded Value" show-overflow-tooltip />
        <el-table-column prop="db" label="DB Value" show-overflow-tooltip />
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.preloaded === row.db ? 'success' : 'warning'" size="small">
              {{ row.preloaded === row.db ? 'OK' : 'Diff' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && settings.length === 0" description="No settings loaded" />
    </el-card>
  </div>
  <div v-else>
    <el-result icon="warning" title="Access Denied" sub-title="This page requires ADMIN role." />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const authStore = useAuthStore()
const loading = ref(false)
const settings = ref<{ name: string; preloaded: string; db: string }[]>([])

async function loadSettings() {
  loading.value = true
  try {
    const response = await request.get('/settings')
    const data = response.data?.data || response.data || {}
    settings.value = Object.entries(data).map(([name, values]) => {
      const arr = values as string[]
      return {
        name,
        preloaded: arr?.[0] ?? '',
        db: arr?.[1] ?? '',
      }
    })
  } catch (err: unknown) {
    const errorMsg =
      (err as { response?: { data?: { error?: string } } })?.response?.data?.error ||
      'Failed to load settings'
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<style scoped>
.settings-page {
  max-width: 1000px;
}
</style>
