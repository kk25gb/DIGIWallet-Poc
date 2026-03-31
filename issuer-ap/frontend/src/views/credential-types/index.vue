<template>
  <div v-if="authStore.isAdmin" class="credential-types">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>Credential Types</h3>
          <el-button type="primary" @click="router.push({ name: 'CredentialTypeCreate' })">
            Create New
          </el-button>
        </div>
      </template>

      <el-table :data="credentialTypes" v-loading="loading" stripe>
        <el-table-column prop="credentialType" label="Credential Type" min-width="200" />
        <el-table-column prop="schemaId" label="Schema ID" min-width="200" show-overflow-tooltip />
        <el-table-column label="Effective Duration" width="160">
          <template #default="{ row }">
            {{ row.effectiveTimeValue }} {{ row.effectiveTimeUnit }}
          </template>
        </el-table-column>
        <el-table-column label="TX Code" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enableTxCode ? 'success' : 'info'" size="small">
              {{ row.enableTxCode ? 'ON' : 'OFF' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="VC Transfer" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enableVcTransfer ? 'success' : 'info'" size="small">
              {{ row.enableVcTransfer ? 'ON' : 'OFF' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openFuncSwitchDialog(row)">Switches</el-button>
            <el-popconfirm
              title="Delete this credential type?"
              @confirm="handleDelete(row.credentialType)"
            >
              <template #reference>
                <el-button size="small" type="danger">Delete</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Func Switch Dialog -->
    <el-dialog v-model="switchDialogVisible" title="Function Switches" width="400px">
      <el-form label-width="130px">
        <el-form-item label="Enable TX Code">
          <el-switch v-model="switchForm.enableTxCode" />
        </el-form-item>
        <el-form-item label="Enable VC Transfer">
          <el-switch v-model="switchForm.enableVcTransfer" />
        </el-form-item>
        <el-alert
          v-if="switchForm.enableTxCode && switchForm.enableVcTransfer"
          type="error"
          title="TX Code and VC Transfer cannot both be enabled"
          :closable="false"
          style="margin-bottom: 16px"
        />
      </el-form>
      <template #footer>
        <el-button @click="switchDialogVisible = false">Cancel</el-button>
        <el-button
          type="primary"
          :loading="switchSubmitting"
          :disabled="switchForm.enableTxCode && switchForm.enableVcTransfer"
          @click="handleFuncSwitch"
        >
          Save
        </el-button>
      </template>
    </el-dialog>
  </div>
  <div v-else>
    <el-result icon="warning" title="Access Denied" sub-title="This page requires ADMIN role." />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

interface CredentialType {
  id: number
  credentialType: string
  businessId: string
  schemaId: string
  effectiveTimeUnit: string
  effectiveTimeValue: number
  enableTxCode: boolean
  enableVcTransfer: boolean
  status: string
}

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const credentialTypes = ref<CredentialType[]>([])

// Func switch dialog
const switchDialogVisible = ref(false)
const switchSubmitting = ref(false)
const switchCredentialType = ref('')
const switchForm = reactive({
  enableTxCode: false,
  enableVcTransfer: false,
})

async function loadCredentialTypes() {
  loading.value = true
  try {
    const response = await request.get('/credential-types')
    credentialTypes.value = response.data || []
  } catch (err: unknown) {
    const errorMsg =
      (err as { response?: { data?: { error?: string } } })?.response?.data?.error ||
      'Failed to load credential types'
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}

function openFuncSwitchDialog(row: CredentialType) {
  switchCredentialType.value = row.credentialType
  switchForm.enableTxCode = row.enableTxCode
  switchForm.enableVcTransfer = row.enableVcTransfer
  switchDialogVisible.value = true
}

async function handleFuncSwitch() {
  switchSubmitting.value = true
  try {
    await request.put(`/credential-types/${switchCredentialType.value}/func-switch`, switchForm)
    ElMessage.success('Function switches updated')
    switchDialogVisible.value = false
    await loadCredentialTypes()
  } catch (err: unknown) {
    const errorMsg =
      (err as { response?: { data?: { error?: string } } })?.response?.data?.error ||
      'Failed to update switches'
    ElMessage.error(errorMsg)
  } finally {
    switchSubmitting.value = false
  }
}

async function handleDelete(credentialType: string) {
  try {
    await request.delete(`/credential-types/${credentialType}`)
    ElMessage.success('Credential type deleted')
    await loadCredentialTypes()
  } catch (err: unknown) {
    const errorMsg =
      (err as { response?: { data?: { error?: string } } })?.response?.data?.error ||
      'Failed to delete credential type'
    ElMessage.error(errorMsg)
  }
}

onMounted(() => {
  loadCredentialTypes()
})
</script>

<style scoped>
.credential-types {
  max-width: 1200px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}
</style>
