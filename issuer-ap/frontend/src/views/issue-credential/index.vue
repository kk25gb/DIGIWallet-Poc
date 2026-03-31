<template>
  <div v-if="authStore.isAdmin" class="issue-credential">
    <el-card>
      <template #header>
        <h3>Issue Credential</h3>
      </template>

      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="Select Type" />
        <el-step title="Enter Holder Data" />
        <el-step title="Generate QR Code" />
      </el-steps>

      <div class="step-content">
        <!-- Step 1: Select Credential Type -->
        <div v-if="activeStep === 0">
          <el-form label-width="160px">
            <el-form-item label="Credential Type">
              <el-select
                v-model="selectedType"
                placeholder="Select a credential type"
                style="width: 100%"
                @change="handleTypeChange"
              >
                <el-option
                  v-for="ct in credentialTypes"
                  :key="ct.credentialType"
                  :label="ct.credentialType"
                  :value="ct.credentialType"
                />
              </el-select>
            </el-form-item>

            <template v-if="selectedTypeDetail">
              <el-form-item label="Schema ID">
                <span>{{ selectedTypeDetail.schemaId }}</span>
              </el-form-item>
              <el-form-item label="Effective Duration">
                <span>{{ selectedTypeDetail.effectiveTimeValue }} {{ selectedTypeDetail.effectiveTimeUnit }}</span>
              </el-form-item>
              <el-form-item label="TX Code">
                <el-tag :type="selectedTypeDetail.enableTxCode ? 'success' : 'info'" size="small">
                  {{ selectedTypeDetail.enableTxCode ? 'ON' : 'OFF' }}
                </el-tag>
              </el-form-item>
              <el-form-item label="VC Transfer">
                <el-tag :type="selectedTypeDetail.enableVcTransfer ? 'success' : 'info'" size="small">
                  {{ selectedTypeDetail.enableVcTransfer ? 'ON' : 'OFF' }}
                </el-tag>
              </el-form-item>
            </template>
          </el-form>

          <div class="step-actions">
            <el-button type="primary" :disabled="!selectedType" @click="goToStep2">
              Next
            </el-button>
          </div>
        </div>

        <!-- Step 2: Enter Holder Data -->
        <div v-if="activeStep === 1">
          <el-form ref="dataFormRef" :model="dataForm" label-width="160px">
            <el-form-item label="Transaction ID">
              <el-input v-model="dataForm.transactionId" placeholder="Auto-generated UUID">
                <template #append>
                  <el-button @click="regenerateTransactionId">Regenerate</el-button>
                </template>
              </el-input>
            </el-form-item>

            <el-divider content-position="left">Credential Subject Fields</el-divider>

            <el-form-item
              v-for="field in schemaFields"
              :key="field.name"
              :label="field.name"
            >
              <el-input-number
                v-if="field.type === 'number' || field.type === 'integer'"
                v-model="dataForm.data[field.name]"
                :placeholder="field.name"
                style="width: 100%"
              />
              <el-switch
                v-else-if="field.type === 'boolean'"
                v-model="dataForm.data[field.name]"
              />
              <el-input
                v-else
                v-model="dataForm.data[field.name]"
                :placeholder="field.name"
              />
            </el-form-item>

            <template v-if="selectedTypeDetail?.enableTxCode">
              <el-divider content-position="left">TX Code</el-divider>
              <el-form-item label="TX Code">
                <el-input v-model="txCode" placeholder="Enter TX code for OTP verification" />
              </el-form-item>
            </template>
          </el-form>

          <div class="step-actions">
            <el-button @click="activeStep = 0">Previous</el-button>
            <el-button type="primary" :disabled="!isDataFormValid" @click="goToStep3">
              Preload & Generate QR Code
            </el-button>
          </div>
        </div>

        <!-- Step 3: QR Code Result -->
        <div v-if="activeStep === 2">
          <div v-loading="generating" class="qr-result">
            <template v-if="qrResult">
              <div v-if="qrResult.qr_code" class="qr-code-display">
                <img :src="'data:image/png;base64,' + qrResult.qr_code" alt="QR Code" />
                <p class="qr-link">
                  <el-text type="info" size="small">{{ qrResult.link }}</el-text>
                </p>
              </div>

              <el-alert
                v-if="qrResult.resp_code && qrResult.resp_code !== 10000"
                type="error"
                :title="'Error code: ' + qrResult.resp_code"
                :description="JSON.stringify(qrResult)"
                :closable="false"
                style="margin-top: 16px"
              />

              <el-alert
                v-if="qrResult.warnings"
                type="warning"
                title="Warnings"
                :description="JSON.stringify(qrResult.warnings)"
                :closable="false"
                style="margin-top: 16px"
              />

              <el-descriptions v-if="qrResult.qr_code" :column="1" border style="margin-top: 16px">
                <el-descriptions-item label="Credential Type">{{ selectedType }}</el-descriptions-item>
                <el-descriptions-item label="Transaction ID">{{ dataForm.transactionId }}</el-descriptions-item>
                <el-descriptions-item label="Status">
                  <el-tag type="success">QR Code Generated</el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </template>

            <el-result
              v-if="error"
              icon="error"
              title="Generation Failed"
              :sub-title="error"
            />
          </div>

          <div class="step-actions">
            <el-button @click="resetWizard">Issue Another</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
  <div v-else>
    <el-result icon="warning" title="Access Denied" sub-title="This page requires ADMIN role." />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

interface CredentialType {
  id: number
  credentialType: string
  businessId: string
  schemaId: string
  vcSchema: string
  effectiveTimeUnit: string
  effectiveTimeValue: number
  enableTxCode: boolean
  enableVcTransfer: boolean
  status: string
}

interface SchemaField {
  name: string
  type: string
}

const authStore = useAuthStore()
const activeStep = ref(0)
const generating = ref(false)
const error = ref('')

// Step 1
const credentialTypes = ref<CredentialType[]>([])
const selectedType = ref('')
const selectedTypeDetail = ref<CredentialType | null>(null)
const schemaFields = ref<SchemaField[]>([])

// Step 2
const dataForm = reactive<{
  transactionId: string
  data: Record<string, unknown>
}>({
  transactionId: crypto.randomUUID(),
  data: {},
})
const txCode = ref('')

// Step 3
const qrResult = ref<Record<string, unknown> | null>(null)

const isDataFormValid = computed(() => {
  if (!dataForm.transactionId) return false
  return schemaFields.value.every((f) => {
    const val = dataForm.data[f.name]
    if (f.type === 'boolean') return true
    if (f.type === 'number' || f.type === 'integer') return val !== undefined && val !== null
    return val !== undefined && val !== null && String(val).trim() !== ''
  })
})

async function loadCredentialTypes() {
  try {
    const response = await request.get('/credential-types')
    credentialTypes.value = response.data || []
  } catch {
    ElMessage.error('Failed to load credential types')
  }
}

function handleTypeChange(credentialType: string) {
  const ct = credentialTypes.value.find((c) => c.credentialType === credentialType)
  selectedTypeDetail.value = ct || null
  parseSchemaFields(ct)
}

function parseSchemaFields(ct: CredentialType | undefined) {
  schemaFields.value = []
  dataForm.data = {}
  if (!ct?.vcSchema) return

  try {
    const schema = typeof ct.vcSchema === 'string' ? JSON.parse(ct.vcSchema) : ct.vcSchema
    const subjectProps = schema?.properties?.credentialSubject?.properties
    if (subjectProps && typeof subjectProps === 'object') {
      for (const [name, def] of Object.entries(subjectProps)) {
        // "id" is auto-injected by vc-handler, not user-editable
        if (name === 'id') continue
        const fieldType = (def as Record<string, string>)?.type || 'string'
        schemaFields.value.push({ name, type: fieldType })
        // Initialize default values
        if (fieldType === 'boolean') {
          dataForm.data[name] = false
        } else if (fieldType === 'number' || fieldType === 'integer') {
          dataForm.data[name] = 0
        } else {
          dataForm.data[name] = ''
        }
      }
    }
  } catch {
    ElMessage.error('Failed to parse VC schema')
  }
}

function regenerateTransactionId() {
  dataForm.transactionId = crypto.randomUUID()
}

function goToStep2() {
  if (!selectedType.value) return
  activeStep.value = 1
}

async function goToStep3() {
  activeStep.value = 2
  generating.value = true
  error.value = ''
  qrResult.value = null

  try {
    // Step 1: Preload data
    await request.post('/issue/preload', {
      credentialType: selectedType.value,
      transactionId: dataForm.transactionId,
      data: dataForm.data,
    })
    ElMessage.success('Data preloaded successfully')

    // Step 2: Generate QR code
    const qrPayload: Record<string, string> = {
      credentialType: selectedType.value,
      transactionId: dataForm.transactionId,
    }
    if (txCode.value) {
      qrPayload.txCode = txCode.value
    }

    const response = await request.post('/issue/qr-code', qrPayload)
    qrResult.value = response.data
    if (response.data?.resp_code === 10000) {
      ElMessage.success('QR code generated successfully')
    } else {
      ElMessage.warning('QR code generated with issues')
    }
  } catch (err: unknown) {
    const errorMsg =
      (err as { response?: { data?: { error?: string } } })?.response?.data?.error ||
      'Failed to generate QR code'
    error.value = errorMsg
    ElMessage.error(errorMsg)
  } finally {
    generating.value = false
  }
}

function resetWizard() {
  activeStep.value = 0
  selectedType.value = ''
  selectedTypeDetail.value = null
  schemaFields.value = []
  dataForm.transactionId = crypto.randomUUID()
  dataForm.data = {}
  txCode.value = ''
  qrResult.value = null
  error.value = ''
}

onMounted(() => {
  loadCredentialTypes()
})
</script>

<style scoped>
.issue-credential {
  max-width: 900px;
}

.step-content {
  margin-top: 30px;
  min-height: 300px;
}

.step-actions {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.qr-result {
  min-height: 200px;
}

.qr-code-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 20px 0;
}

.qr-code-display img {
  width: 241px;
  height: 241px;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
}

.qr-link {
  margin-top: 12px;
  word-break: break-all;
  max-width: 600px;
  text-align: center;
}
</style>
