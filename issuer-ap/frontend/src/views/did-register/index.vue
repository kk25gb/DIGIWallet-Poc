<template>
  <div v-if="authStore.isAdmin" class="did-register">
    <el-card>
      <template #header>
        <h3>DID Registration</h3>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="140px"
        @submit.prevent="handleSubmit"
      >
        <!-- Organization Info -->
        <el-divider content-position="left">Organization Info</el-divider>

        <el-form-item label="Name" prop="orgName">
          <el-input v-model="form.orgName" placeholder="Organization name" />
        </el-form-item>

        <el-form-item label="Description" prop="orgDescription">
          <el-input
            v-model="form.orgDescription"
            type="textarea"
            :rows="3"
            placeholder="Organization description"
          />
        </el-form-item>

        <el-form-item label="URL" prop="orgUrl">
          <el-input v-model="form.orgUrl" placeholder="https://example.org" />
        </el-form-item>

        <el-form-item label="Tax ID / Code" prop="orgCode">
          <el-input v-model="form.orgCode" placeholder="Tax ID or identifier" />
        </el-form-item>

        <!-- P7 Data -->
        <el-divider content-position="left">Signature Data (Optional)</el-divider>

        <el-form-item label="P7 Data">
          <el-input
            v-model="form.p7data"
            type="textarea"
            :rows="4"
            placeholder="Leave empty for POC"
          />
        </el-form-item>

        <!-- Submit -->
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
          >
            Register DID
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Result -->
      <el-result
        v-if="result"
        :icon="result.success ? 'success' : 'error'"
        :title="result.success ? 'DID Registered' : 'Registration Failed'"
        :sub-title="result.message"
      />
    </el-card>
  </div>
  <div v-else>
    <el-result icon="warning" title="Access Denied" sub-title="This page requires ADMIN role." />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const result = ref<{ success: boolean; message: string } | null>(null)

const form = reactive({
  orgName: '',
  orgDescription: '',
  orgUrl: '',
  orgCode: '',
  p7data: '',
})

const rules: FormRules = {
  orgName: [{ required: true, message: 'Organization name is required', trigger: 'blur' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  result.value = null

  const payload = {
    org: {
      name: form.orgName,
      description: form.orgDescription,
      url: form.orgUrl,
      code: form.orgCode,
    },
    p7data: form.p7data || '',
  }

  try {
    const response = await request.post('/did/register', payload)
    result.value = {
      success: true,
      message: `DID: ${response.data?.did || 'Created successfully'}`,
    }
    ElMessage.success('DID registered successfully')
  } catch (err: unknown) {
    const errorMsg =
      (err as { response?: { data?: { error?: string } } })?.response?.data?.error ||
      'Unknown error'
    result.value = {
      success: false,
      message: errorMsg,
    }
    ElMessage.error('DID registration failed')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.did-register {
  max-width: 700px;
}
</style>
