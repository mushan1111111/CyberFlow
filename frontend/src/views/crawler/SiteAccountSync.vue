<!--
  SiteAccountSyncPage - 个人站点账号同步页面
  建站平台的 site/site/list 接口受账号权限限制，只返回「当前登录账号」名下站点的
  主题与分类。因此每位成员可以在这里录入自己的建站平台账号，系统会以该账号登录，
  只拉取并合并本人站点的主题、分类、站点标签等字段（只更新、不删除）。
-->
<template>
  <div class="account-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">PERSONAL SITE SYNC</p>
        <h1>站点账号同步</h1>
        <span>录入你自己的建站平台账号，只同步你名下站点的主题与分类。</span>
      </div>
      <el-button type="primary" :disabled="editing" @click="startCreate">新增账号</el-button>
    </section>

    <el-alert type="info" :closable="false" class="hint">
      <template #title>
        为什么需要自己的账号？建站平台的站点接口按登录账号返回数据，主题和分类只有本人账号才拿得到。
        这里保存的账号只用于你自己的站点同步，只会更新你本人站点的资料，不会删除任何站点。
      </template>
    </el-alert>

    <el-card v-if="editing" shadow="never" class="form-card">
      <template #header><strong>{{ form.id ? '编辑账号' : '新增账号' }}</strong></template>
      <el-form :model="form" label-position="top">
        <div class="form-grid">
          <el-form-item label="建站平台账号" required>
            <el-input v-model="form.remoteUsername" placeholder="登录建站平台用的账号" autocomplete="off" />
          </el-form-item>
          <el-form-item label="建站平台密码" required>
            <el-input v-model="form.remotePassword" type="password" show-password placeholder="留空表示不修改" autocomplete="new-password" />
          </el-form-item>
          <el-form-item label="负责人名称">
            <el-input v-model="form.ownerName" placeholder="选填，如 C-王志彬，用于进一步限定同步范围" />
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="form.enabled" />
          </el-form-item>
        </div>
        <div class="form-actions">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="accounts" v-loading="loading" stripe empty-text="还没有配置站点账号">
        <el-table-column prop="remoteUsername" label="平台账号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ownerName" label="负责人" min-width="140">
          <template #default="{ row }">{{ row.ownerName || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近同步" min-width="170">
          <template #default="{ row }">{{ formatTime(row.lastSyncedAt) }}</template>
        </el-table-column>
        <el-table-column label="最近结果" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.lastStatus" :type="row.lastStatus === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.lastStatus === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
            <span class="result-note">{{ row.lastMessage || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="归属" min-width="120">
          <template #default="{ row }">{{ row.ownerUsername || '本人' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.enabled || syncingId === row.id" @click="syncAccount(row)">
              {{ syncingId === row.id ? '同步中' : '同步站点' }}
            </el-button>
            <el-button link type="primary" @click="startEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeAccount(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <TaskProgress :task="currentTask" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import TaskProgress from '@/components/TaskProgress.vue'
import { useTaskProgress } from '@/composables/useTaskProgress'
import { deleteSiteAccount, listSiteAccounts, saveSiteAccount, syncSiteAccount } from '@/api/crawler'

const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const accounts = ref([])
const syncingId = ref('')
const progress = useTaskProgress()
const currentTask = computed(() => progress.task.value)

const emptyForm = () => ({ id: '', remoteUsername: '', remotePassword: '', ownerName: '', enabled: true })
const form = ref(emptyForm())

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 19)
}

async function loadAccounts() {
  loading.value = true
  try { accounts.value = (await listSiteAccounts()).data || [] }
  finally { loading.value = false }
}

function startCreate() {
  form.value = emptyForm()
  editing.value = true
}

function startEdit(row) {
  form.value = { id: row.id, remoteUsername: row.remoteUsername, remotePassword: '', ownerName: row.ownerName || '', enabled: !!row.enabled }
  editing.value = true
}

function cancelEdit() {
  form.value = emptyForm()
  editing.value = false
}

async function submitForm() {
  if (!form.value.remoteUsername?.trim()) return ElMessage.error('请填写建站平台账号')
  if (!form.value.id && !form.value.remotePassword) return ElMessage.error('请填写建站平台密码')
  saving.value = true
  try {
    await saveSiteAccount({
      id: form.value.id || undefined,
      remoteUsername: form.value.remoteUsername.trim(),
      remotePassword: form.value.remotePassword,
      ownerName: form.value.ownerName?.trim() || '',
      enabled: form.value.enabled,
    })
    ElMessage.success('站点账号已保存')
    cancelEdit()
    await loadAccounts()
  } finally { saving.value = false }
}

async function removeAccount(row) {
  try {
    await ElMessageBox.confirm(`确定删除账号「${row.remoteUsername}」？`, '删除确认', { type: 'warning' })
  } catch { return }
  await deleteSiteAccount(row.id)
  ElMessage.success('已删除')
  await loadAccounts()
}

async function syncAccount(row) {
  if (!row.enabled || syncingId.value === row.id) return
  syncingId.value = row.id
  try {
    const response = await syncSiteAccount(row.id)
    progress.track(response.data.task_id)
    ElMessage.success('同步任务已下发，只会更新你本人站点的资料')
  } finally { syncingId.value = '' }
}

onMounted(loadAccounts)

// Refresh the last-run columns as soon as a dispatched sync reaches a
// terminal state instead of waiting for a manual reload.
watch(() => currentTask.value?.state, state => {
  if (state && !['PENDING', 'RUNNING', 'PAUSED'].includes(state)) loadAccounts()
})
</script>

<style scoped>
.account-page { display: grid; max-width: 1180px; gap: 16px; margin: 0 auto; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 4px; }
.page-heading h1 { margin: 0; color: var(--cf-ink); font-size: 27px; letter-spacing: -.04em; }
.page-heading span { display: block; margin-top: 7px; color: var(--cf-muted); font-size: 12px; }
.page-heading .eyebrow { margin: 0 0 6px; color: var(--cf-blue); font-size: 9px; font-weight: 800; letter-spacing: .15em; }
.hint { border-radius: 12px; }
.hint :deep(.el-alert__title) { font-size: 12px; line-height: 1.8; }
.form-card, .table-card, .progress-card { border-radius: 14px; }
.form-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0 16px; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; }
.result-note { margin-left: 8px; color: var(--cf-muted); font-size: 11px; }
@media (max-width: 900px) { .form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 560px) { .page-heading { align-items: flex-start; flex-direction: column; } .form-grid { grid-template-columns: 1fr; } }
</style>
