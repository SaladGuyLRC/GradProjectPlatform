<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { api, type ApiResponse } from '../api'
import { useAuthStore, type UserInfo } from '../stores/auth'

interface Task { id: string; studentId: string; creatorId: string; title: string; description?: string; type: string; priority: string; status: string; deadlineAt: string }
const auth = useAuthStore()
const loading = ref(false), dialog = ref(false), editingId = ref<string | null>(null)
const tasks = ref<Task[]>([]), students = ref<UserInfo[]>([]), status = ref(''), selectedStudent = ref('')
const form = reactive({ studentId: '', title: '', description: '', type: 'PROGRESS', priority: 'MEDIUM', deadlineAt: '' })
const isMentor = computed(() => auth.user?.role === 'MENTOR')
const types = ['MEETING', 'PROGRESS', 'DOCUMENT', 'CODE', 'EXPERIMENT', 'OTHER']
const priorities = ['LOW', 'MEDIUM', 'HIGH']
const statuses = ['TODO', 'IN_PROGRESS', 'COMPLETED']

onMounted(async () => {
  if (isMentor.value) {
    students.value = (await api.get<ApiResponse<UserInfo[]>>('/mentor/students')).data.data
    selectedStudent.value = students.value[0]?.id || ''
  }
  await load()
})
async function load() {
  if (isMentor.value && !selectedStudent.value) { tasks.value = []; return }
  loading.value = true
  try {
    const { data } = await api.get<ApiResponse<{ content: Task[] }>>('/tasks', { params: { page: 0, size: 50, status: status.value || undefined, studentId: isMentor.value ? selectedStudent.value : undefined } })
    tasks.value = data.data.content
  } finally { loading.value = false }
}
function openCreate() { editingId.value = null; Object.assign(form, { studentId: selectedStudent.value, title: '', description: '', type: 'PROGRESS', priority: 'MEDIUM', deadlineAt: '' }); dialog.value = true }
function openEdit(task: Task) { editingId.value = task.id; Object.assign(form, { studentId: task.studentId, title: task.title, description: task.description || '', type: task.type, priority: task.priority, deadlineAt: task.deadlineAt.slice(0, 16) }); dialog.value = true }
async function save() {
  const payload = { ...form, studentId: isMentor.value ? form.studentId : undefined, deadlineAt: new Date(form.deadlineAt).toISOString() }
  if (editingId.value) await api.put(`/tasks/${editingId.value}`, payload); else await api.post('/tasks', payload)
  ElMessage.success(editingId.value ? '任务已更新' : '任务已创建'); dialog.value = false; await load()
}
async function changeStatus(task: Task, value: string) { await api.patch(`/tasks/${task.id}/status`, { status: value }); ElMessage.success('状态已更新'); await load() }
async function remove(task: Task) { await ElMessageBox.confirm(`确认删除“${task.title}”？`, '删除任务', { type: 'warning' }); await api.delete(`/tasks/${task.id}`); ElMessage.success('任务已删除'); await load() }
const overdue = (task: Task) => task.status !== 'COMPLETED' && new Date(task.deadlineAt) < new Date()
const date = (v: string) => new Date(v).toLocaleString('zh-CN')
</script>

<template>
  <div>
    <header class="page-header"><div><h1>Tasks</h1><p>Track graduation project work and deadlines</p></div><el-button type="primary" :icon="Plus" @click="openCreate">Create task</el-button></header>
    <section class="surface">
      <div class="toolbar">
        <el-select v-if="isMentor" v-model="selectedStudent" placeholder="Select student" style="width: 180px" @change="load"><el-option v-for="s in students" :key="s.id" :label="s.realName" :value="s.id" /></el-select>
        <el-select v-model="status" placeholder="All statuses" clearable style="width: 160px" @change="load"><el-option v-for="s in statuses" :key="s" :label="s" :value="s" /></el-select>
        <el-button :icon="Refresh" circle title="刷新" aria-label="刷新" @click="load" />
      </div>
      <el-table v-loading="loading" :data="tasks" empty-text="No tasks" style="width:100%">
        <el-table-column prop="title" label="Task" min-width="190"><template #default="{ row }"><strong>{{ row.title }}</strong><div class="task-desc">{{ row.description }}</div></template></el-table-column>
        <el-table-column prop="type" label="Type" width="110" />
        <el-table-column prop="priority" label="Priority" width="105"><template #default="{ row }"><el-tag :type="row.priority === 'HIGH' ? 'danger' : row.priority === 'LOW' ? 'info' : 'warning'" size="small">{{ row.priority }}</el-tag></template></el-table-column>
        <el-table-column label="Status" width="155"><template #default="{ row }"><el-select v-if="!isMentor" :model-value="row.status" size="small" @change="changeStatus(row, $event)"><el-option v-for="s in statuses" :key="s" :label="s" :value="s" /></el-select><el-tag v-else size="small">{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column label="Deadline" min-width="170"><template #default="{ row }"><span :class="{ danger: overdue(row) }">{{ date(row.deadlineAt) }}</span><small v-if="overdue(row)" class="overdue-label">Overdue</small></template></el-table-column>
        <el-table-column label="Actions" width="110" fixed="right"><template #default="{ row }"><template v-if="row.creatorId === auth.user?.id"><el-button :icon="Edit" text circle title="Edit" @click="openEdit(row)" /><el-button :icon="Delete" text circle type="danger" title="Delete" @click="remove(row)" /></template></template></el-table-column>
      </el-table>
    </section>
    <el-dialog v-model="dialog" :title="editingId ? 'Edit task' : 'Create task'" width="560px">
      <el-form label-position="top">
        <el-form-item v-if="isMentor" label="Student" required><el-select v-model="form.studentId" style="width:100%"><el-option v-for="s in students" :key="s.id" :label="s.realName" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="Title" required><el-input v-model="form.title" maxlength="100" /></el-form-item>
        <el-form-item label="Description"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <div class="form-row"><el-form-item label="Type" required><el-select v-model="form.type"><el-option v-for="t in types" :key="t" :label="t" :value="t" /></el-select></el-form-item><el-form-item label="Priority" required><el-select v-model="form.priority"><el-option v-for="p in priorities" :key="p" :label="p" :value="p" /></el-select></el-form-item></div>
        <el-form-item label="Deadline" required><el-date-picker v-model="form.deadlineAt" type="datetime" value-format="YYYY-MM-DDTHH:mm" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">Cancel</el-button><el-button type="primary" :disabled="!form.title || !form.deadlineAt || (isMentor && !form.studentId)" @click="save">Save</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.task-desc { margin-top: 4px; color: var(--muted); font-size: 12px; white-space: normal; }.overdue-label { display: block; margin-top: 3px; color: #b42318; }.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }.form-row .el-select { width: 100%; }
@media(max-width:600px){.form-row{grid-template-columns:1fr}}
</style>
