<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Edit, Plus, Refresh, Upload } from '@element-plus/icons-vue'
import { api, type ApiResponse } from '../api'
import { useAuthStore, type UserInfo } from '../stores/auth'

interface Project {
  id: string
  studentId: string
  mentorId?: string
  title: string
  summary?: string
  techStack?: string[]
  repositoryUrl?: string
  status: string
  startDate?: string
  plannedEndDate?: string
}
interface Submission { id: string; originalFilename: string; fileSize: number; version: number; uploadedAt: string }
interface ReportPage { totalElements: number }

const auth = useAuthStore()
const isMentor = computed(() => auth.user?.role === 'MENTOR')
const title = computed(() => isMentor.value ? 'Student Projects' : 'My Project')
const description = computed(() => isMentor.value ? 'View projects and submissions from your assigned students' : 'Manage your graduation project and submit your paper')
const loading = ref(false), saving = ref(false), uploading = ref(false), dialog = ref(false)
const startDateLocked = ref(false)
const project = ref<Project | null>(null), submissions = ref<Submission[]>([]), selectedStudent = ref('')
const students = ref<UserInfo[]>([]), selectedFile = ref<File | null>(null)
const form = reactive({ title: '', summary: '', techStack: '', repositoryUrl: '', status: 'IN_PROGRESS', startDate: '', plannedEndDate: '' })

onMounted(async () => {
  if (isMentor.value) {
    students.value = (await api.get<ApiResponse<UserInfo[]>>('/mentor/students')).data.data
    selectedStudent.value = students.value[0]?.id || ''
  }
  await load()
})

async function load() {
  if (isMentor.value && !selectedStudent.value) { project.value = null; submissions.value = []; return }
  loading.value = true
  try {
    const projectUrl = isMentor.value ? `/mentor/students/${selectedStudent.value}/project` : '/projects/me'
    const submissionUrl = isMentor.value ? `/mentor/students/${selectedStudent.value}/project/submissions` : '/projects/me/submissions'
    project.value = (await api.get<ApiResponse<Project | null>>(projectUrl)).data.data
    submissions.value = (await api.get<ApiResponse<Submission[]>>(submissionUrl)).data.data
    if (!isMentor.value) {
      const reports = (await api.get<ApiResponse<ReportPage>>('/weekly-reports/me', { params: { page: 0, size: 1 } })).data.data
      startDateLocked.value = reports.totalElements > 0
    }
  } finally { loading.value = false }
}

function openProjectForm() {
  Object.assign(form, {
    title: project.value?.title || '', summary: project.value?.summary || '', techStack: project.value?.techStack?.join(', ') || '',
    repositoryUrl: project.value?.repositoryUrl || '', status: project.value?.status || 'IN_PROGRESS',
    startDate: project.value?.startDate || '', plannedEndDate: project.value?.plannedEndDate || ''
  })
  dialog.value = true
}

async function saveProject() {
  saving.value = true
  try {
    const creating = !project.value
    const payload = { ...form, techStack: form.techStack.split(',').map(item => item.trim()).filter(Boolean), repositoryUrl: form.repositoryUrl || null, startDate: form.startDate || null, plannedEndDate: form.plannedEndDate || null }
    const method = project.value ? 'put' : 'post'
    project.value = (await api[method]<ApiResponse<Project>>('/projects/me', payload)).data.data
    ElMessage.success(creating ? 'Project created' : 'Project saved')
    dialog.value = false
    await load()
  } finally { saving.value = false }
}

function chooseFile(uploadFile: any) {
  selectedFile.value = uploadFile.raw || null
}

async function uploadSubmission() {
  if (!selectedFile.value || !project.value) return
  uploading.value = true
  try {
    const body = new FormData()
    body.append('file', selectedFile.value)
    await api.post('/projects/me/submissions', body)
    ElMessage.success('Submission uploaded successfully')
    selectedFile.value = null
    await load()
  } finally { uploading.value = false }
}

async function downloadSubmission(submission: Submission) {
  const path = isMentor.value ? `/mentor/students/${selectedStudent.value}/project/submissions/${submission.id}/download` : `/projects/me/submissions/${submission.id}/download`
  const response = await api.get(path, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url; link.download = submission.originalFilename; link.click(); URL.revokeObjectURL(url)
}

const size = (bytes: number) => bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`
const date = (value: string) => new Date(value).toLocaleString('en-GB', { dateStyle: 'medium' })
const statusLabel = (value: string) => value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, letter => letter.toUpperCase())
</script>

<template>
  <div v-loading="loading">
    <header class="page-header"><div><h1>{{ title }}</h1><p>{{ description }}</p></div><template v-if="!isMentor"><el-button type="primary" :icon="project ? Edit : Plus" @click="openProjectForm">{{ project ? 'Edit project' : 'Create project' }}</el-button></template></header>

    <section v-if="isMentor" class="surface mentor-selector"><el-select v-model="selectedStudent" placeholder="Select student" style="width:240px" @change="load"><el-option v-for="student in students" :key="student.id" :label="student.realName" :value="student.id" /></el-select><el-button :icon="Refresh" circle title="Refresh" aria-label="Refresh" @click="load" /></section>

    <template v-if="project">
      <div class="project-grid">
        <section class="surface"><div class="section-heading"><h2 class="section-title">Project overview</h2><el-tag size="small">{{ statusLabel(project.status) }}</el-tag></div><dl class="info-list"><dt>Project title</dt><dd>{{ project.title }}</dd><dt>Project summary</dt><dd class="long-text">{{ project.summary || 'No summary provided' }}</dd><dt>Technology stack</dt><dd>{{ project.techStack?.join(', ') || 'Not specified' }}</dd><dt>Repository URL</dt><dd>{{ project.repositoryUrl || 'Not provided' }}</dd><dt>Start date</dt><dd>{{ project.startDate || 'Not provided' }}</dd><dt>Planned end date</dt><dd>{{ project.plannedEndDate || 'Not provided' }}</dd></dl></section>
        <section class="surface"><div class="section-heading"><h2 class="section-title">Project submissions</h2><el-upload v-if="!isMentor" :auto-upload="false" :show-file-list="false" accept="application/pdf,.pdf" :on-change="chooseFile"><el-button :icon="Upload" :disabled="uploading">Select PDF</el-button></el-upload></div><div v-if="!isMentor && selectedFile" class="selected-file"><span>{{ selectedFile.name }}</span><el-button type="primary" size="small" :loading="uploading" @click="uploadSubmission">Upload</el-button></div><el-table :data="submissions" empty-text="No submissions" size="small"><el-table-column label="Version" width="85"><template #default="{ row }">v{{ row.version }}</template></el-table-column><el-table-column prop="originalFilename" label="Filename" min-width="170" show-overflow-tooltip /><el-table-column label="Uploaded" min-width="135"><template #default="{ row }">{{ date(row.uploadedAt) }}</template></el-table-column><el-table-column label="Size" width="85"><template #default="{ row }">{{ size(row.fileSize) }}</template></el-table-column><el-table-column label="Actions" width="95" fixed="right"><template #default="{ row }"><el-button :icon="Download" text circle title="Download" aria-label="Download" @click="downloadSubmission(row)" /></template></el-table-column></el-table><p class="upload-tip">PDF only, maximum 20 MB. Uploading a new file creates a new version.</p></section>
      </div>
    </template>
    <section v-else class="surface empty-project"><h2>No project yet</h2><p>{{ isMentor ? 'This student has not created a project.' : 'Create your graduation project profile before uploading a submission.' }}</p><el-button v-if="!isMentor" type="primary" :icon="Plus" @click="openProjectForm">Create project</el-button></section>

    <el-dialog v-model="dialog" :title="project ? 'Edit project' : 'Create project'" width="620px"><el-form label-position="top"><el-form-item label="Project title" required><el-input v-model="form.title" maxlength="150" /></el-form-item><el-form-item label="Project summary"><el-input v-model="form.summary" type="textarea" :rows="5" maxlength="2000" show-word-limit /></el-form-item><el-form-item label="Technology stack"><el-input v-model="form.techStack" placeholder="Vue, Spring Boot, MongoDB" /></el-form-item><el-form-item label="Repository URL"><el-input v-model="form.repositoryUrl" placeholder="https://github.com/..." /></el-form-item><div class="form-row"><el-form-item label="Status" required><el-select v-model="form.status" style="width:100%"><el-option v-for="value in ['IN_PROGRESS','PAUSED','COMPLETED']" :key="value" :label="statusLabel(value)" :value="value" /></el-select></el-form-item><el-form-item label="Start date"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" :disabled="startDateLocked" style="width:100%" /></el-form-item></div><el-form-item label="Planned end date"><el-date-picker v-model="form.plannedEndDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-form><template #footer><el-button @click="dialog=false">Cancel</el-button><el-button type="primary" :loading="saving" :disabled="!form.title.trim()" @click="saveProject">Save project</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.mentor-selector { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; }.project-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18px; }.section-heading { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; }.section-title { margin-bottom: 0; }.info-list { display: grid; grid-template-columns: 120px 1fr; gap: 14px 18px; margin: 0; font-size: 14px; }.info-list dt { color: var(--muted); }.info-list dd { margin: 0; font-weight: 550; overflow-wrap: anywhere; }.long-text { white-space: pre-wrap; line-height: 1.6; font-weight: 400 !important; }.selected-file { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: -2px 0 13px; padding: 9px 11px; background: #f1f7f4; border: 1px solid #d7e8df; font-size: 13px; }.upload-tip { margin: 13px 0 0; color: var(--muted); font-size: 12px; }.empty-project { padding: 42px 24px; text-align: center; }.empty-project h2 { margin-top: 0; }.empty-project p { margin: 8px 0 22px; color: var(--muted); }.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }.form-row .el-date-editor { width: 100%; }
@media(max-width:900px){.project-grid{grid-template-columns:1fr}} @media(max-width:600px){.info-list{grid-template-columns:1fr;gap:5px 0}.info-list dt{margin-top:9px}.info-list dd{margin-bottom:5px}.form-row{grid-template-columns:1fr}}
</style>
