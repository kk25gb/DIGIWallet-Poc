<template>
  <div v-if="authStore.isAdmin" class="credential-type-create">
    <el-card>
      <template #header>
        <h3>Create Credential Type</h3>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="160px"
        @submit.prevent="handleSubmit"
      >
        <!-- Basic Info -->
        <el-divider content-position="left">Basic Info</el-divider>

        <el-form-item label="Business ID" prop="businessId">
          <el-input v-model="form.businessId" placeholder="e.g. moda" />
        </el-form-item>

        <el-form-item label="Type Name" prop="typeName">
          <el-input v-model="form.typeName" placeholder="e.g. VirtualCardCredential" />
        </el-form-item>

        <el-form-item label="Credential Type">
          <el-tag v-if="composedType" type="info" size="large">{{ composedType }}</el-tag>
          <span v-else class="text-muted">Fill in Business ID and Type Name</span>
        </el-form-item>

        <el-form-item label="Schema ID" prop="schemaId">
          <el-input v-model="form.schemaId" placeholder="https://example.com/schema/v1.json" />
        </el-form-item>

        <!-- Effective Duration -->
        <el-divider content-position="left">Effective Duration</el-divider>

        <el-form-item label="Time Unit" prop="effectiveTimeUnit">
          <el-select v-model="form.effectiveTimeUnit" placeholder="Select">
            <el-option label="Year" value="YEAR" />
            <el-option label="Month" value="MONTH" />
            <el-option label="Day" value="DAY" />
            <el-option label="Hour" value="HOUR" />
            <el-option label="Minute" value="MINUTE" />
          </el-select>
        </el-form-item>

        <el-form-item label="Time Value" prop="effectiveTimeValue">
          <el-input-number v-model="form.effectiveTimeValue" :min="1" :max="1000" />
        </el-form-item>

        <!-- Credential Subject Fields -->
        <el-divider content-position="left">Credential Subject Fields</el-divider>

        <div v-for="(field, index) in form.fields" :key="index" class="field-row">
          <el-form-item
            :label="'Field ' + (index + 1)"
            :prop="'fields.' + index + '.name'"
            :rules="[{ required: true, message: 'Field name is required', trigger: 'blur' }]"
          >
            <div class="field-inputs">
              <el-input
                v-model="field.name"
                placeholder="Field name (e.g. id_number)"
                style="flex: 2"
              />
              <el-select v-model="field.type" placeholder="Type" style="flex: 1">
                <el-option label="string" value="string" />
                <el-option label="number" value="number" />
                <el-option label="boolean" value="boolean" />
                <el-option label="integer" value="integer" />
              </el-select>
              <el-checkbox v-model="field.required" label="Required" />
              <el-button
                type="danger"
                :icon="Delete"
                circle
                :disabled="form.fields.length <= 1"
                @click="removeField(index)"
              />
            </div>
          </el-form-item>
        </div>

        <el-form-item>
          <el-button type="success" plain @click="addField">+ Add Field</el-button>
        </el-form-item>

        <!-- Submit -->
        <el-divider />
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            Create Credential Type
          </el-button>
          <el-button @click="router.push({ name: 'CredentialTypes' })">Cancel</el-button>
        </el-form-item>
      </el-form>

      <!-- Result -->
      <el-result
        v-if="result"
        :icon="result.success ? 'success' : 'error'"
        :title="result.success ? 'Created Successfully' : 'Creation Failed'"
        :sub-title="result.message"
      >
        <template v-if="result.success" #extra>
          <el-button type="primary" @click="router.push({ name: 'CredentialTypes' })">
            Back to List
          </el-button>
        </template>
      </el-result>
    </el-card>
  </div>
  <div v-else>
    <el-result icon="warning" title="Access Denied" sub-title="This page requires ADMIN role." />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const result = ref<{ success: boolean; message: string } | null>(null)

const form = reactive({
  businessId: '',
  typeName: '',
  schemaId: '',
  effectiveTimeUnit: 'YEAR',
  effectiveTimeValue: 5,
  fields: [{ name: '', type: 'string', required: true }] as { name: string; type: string; required: boolean }[],
})

const composedType = computed(() => {
  if (form.businessId && form.typeName) {
    return `${form.businessId}_${form.typeName}`
  }
  return ''
})

const rules: FormRules = {
  businessId: [{ required: true, message: 'Business ID is required', trigger: 'blur' }],
  typeName: [{ required: true, message: 'Type Name is required', trigger: 'blur' }],
  schemaId: [{ required: true, message: 'Schema ID is required', trigger: 'blur' }],
  effectiveTimeUnit: [{ required: true, message: 'Time unit is required', trigger: 'change' }],
  effectiveTimeValue: [{ required: true, message: 'Time value is required', trigger: 'blur' }],
}

function addField() {
  form.fields.push({ name: '', type: 'string', required: true })
}

function removeField(index: number) {
  form.fields.splice(index, 1)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // Validate fields have names
  const emptyField = form.fields.find((f) => !f.name.trim())
  if (emptyField) {
    ElMessage.warning('All field names are required')
    return
  }

  // Check for "id" field (not allowed)
  const hasIdField = form.fields.some((f) => f.name.trim() === 'id')
  if (hasIdField) {
    ElMessage.error('Field name "id" is reserved and cannot be used')
    return
  }

  submitting.value = true
  result.value = null

  try {
    await request.post('/credential-types', {
      businessId: form.businessId,
      typeName: form.typeName,
      schemaId: form.schemaId,
      effectiveTimeUnit: form.effectiveTimeUnit,
      effectiveTimeValue: form.effectiveTimeValue,
      fields: form.fields,
    })
    result.value = {
      success: true,
      message: `Credential type "${composedType.value}" created successfully`,
    }
    ElMessage.success('Credential type created')
  } catch (err: unknown) {
    const errorMsg =
      (err as { response?: { data?: { error?: string } } })?.response?.data?.error ||
      'Unknown error'
    result.value = { success: false, message: errorMsg }
    ElMessage.error('Creation failed')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.credential-type-create {
  max-width: 800px;
}

.field-row {
  margin-bottom: 0;
}

.field-inputs {
  display: flex;
  gap: 8px;
  width: 100%;
}

.text-muted {
  color: #909399;
  font-size: 14px;
}
</style>
