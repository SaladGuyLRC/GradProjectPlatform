<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Edit, Plus, Refresh, Upload } from '@element-plus/icons-vue'
import { api, type ApiResponse } from '../api'
import { useAuthStore } from '../stores/auth'

interface Report { id: string; weekStart: string; weekEnd: string; completedWork: string; currentProblems?: string; nextWeekPlan: string; status: string; studentId: string; review?: { content: string; reviewedAt: string } }
const auth = useAuthStore(), isMentor = computed(() => auth.user?.role === 'MENTOR')
const reports = ref<Report[]>([]), loading = ref(false), dialog = ref(false), detail = ref<Report | null>(null), reviewText = ref(''), status = ref('')
const editingId = ref<string | null>(null)
const form = reactive({ weekStart: '', completedWork: '', currentProblems: '', nextWeekPlan: '' })
onMounted(() => load())
async function load() {
  loading.value = true
  try {
    const url = isMentor.value ? '/mentor/weekly-reports' : '/weekly-reports/me'
    const { data } = await api.get<ApiResponse<{ content: Report[] }>>(url, { params: { page: 0, size: 50, status: isMentor.value && status.value ? status.value : undefined } })
    reports.value = data.data.content
  } finally { loading.value = false }
}
function openCreate() { editingId.value = null; Object.assign(form, { weekStart: '', completedWork: '', currentProblems: '', nextWeekPlan: '' }); dialog.value = true }
function openEdit(r: Report) { editingId.value = r.id; Object.assign(form, { weekStart: r.weekStart, completedWork: r.completedWork, currentProblems: r.currentProblems || '', nextWeekPlan: r.nextWeekPlan }); dialog.value = true }
async function save() { if (editingId.value) await api.put(`/weekly-reports/${editingId.value}`, form); else await api.post('/weekly-reports', form); ElMessage.success(editingId.value ? 'Draft updated' : 'Draft created'); dialog.value = false; await load() }
async function submit(r: Report) { await ElMessageBox.confirm('Submitted reports cannot be edited. Continue?', 'Submit weekly report', { type: 'warning' }); await api.post(`/weekly-reports/${r.id}/submit`); ElMessage.success('Weekly report submitted'); await load() }
function openDetail(r: Report) { detail.value = r; reviewText.value = r.review?.content || '' }
async function review() { if (!detail.value) return; await api.put(`/mentor/weekly-reports/${detail.value.id}/review`, { content: reviewText.value }); ElMessage.success('Review saved'); detail.value = null; await load() }
const tagType = (s: string) => s === 'DRAFT' ? 'info' : s === 'SUBMITTED' ? 'warning' : 'success'
</script>

<template>
  <div>
    <header class="page-header"><div><h1>Weekly reports</h1><p>{{ isMentor ? 'Review submissions from your students' : 'Record your work and submit it for review' }}</p></div><el-button v-if="!isMentor" type="primary" :icon="Plus" @click="openCreate">Create draft</el-button></header>
    <section class="surface">
      <div class="toolbar"><el-select v-if="isMentor" v-model="status" placeholder="All statuses" clearable style="width:170px" @change="load"><el-option v-for="s in ['DRAFT','SUBMITTED','REVIEWED']" :key="s" :label="s" :value="s" /></el-select><el-button :icon="Refresh" circle title="Refresh" @click="load" /></div>
      <el-table v-loading="loading" :data="reports" empty-text="No weekly reports" @row-click="isMentor ? openDetail($event) : undefined">
        <el-table-column label="Period" min-width="180"><template #default="{ row }"><strong>{{ row.weekStart }}</strong> to {{ row.weekEnd }}</template></el-table-column>
        <el-table-column v-if="isMentor" prop="studentId" label="Student ID" min-width="150" />
        <el-table-column prop="completedWork" label="Completed work" min-width="230" show-overflow-tooltip />
        <el-table-column label="Status" width="120"><template #default="{ row }"><el-tag :type="tagType(row.status)" size="small">{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column v-if="!isMentor" label="Actions" width="130" fixed="right"><template #default="{ row }"><template v-if="row.status === 'DRAFT'"><el-button :icon="Edit" text circle title="Edit" @click.stop="openEdit(row)" /><el-button :icon="Upload" text circle type="primary" title="Submit" @click.stop="submit(row)" /></template><el-button v-else text @click.stop="openDetail(row)">Details</el-button></template></el-table-column>
        <el-table-column v-else label="Actions" width="100" fixed="right"><template #default="{ row }"><el-button text type="primary" @click.stop="openDetail(row)">{{ row.status === 'DRAFT' ? 'View' : 'Review' }}</el-button></template></el-table-column>
      </el-table>
    </section>
    <el-dialog v-model="dialog" :title="editingId ? 'Edit weekly report draft' : 'Create weekly report draft'" width="650px">
      <el-form label-position="top"><el-form-item label="Week start (Monday)" required><el-date-picker v-model="form.weekStart" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item><el-form-item label="Completed work" required><el-input v-model="form.completedWork" type="textarea" :rows="4" /></el-form-item><el-form-item label="Current problems"><el-input v-model="form.currentProblems" type="textarea" :rows="3" /></el-form-item><el-form-item label="Next week plan" required><el-input v-model="form.nextWeekPlan" type="textarea" :rows="3" /></el-form-item></el-form>
      <template #footer><el-button @click="dialog=false">Cancel</el-button><el-button type="primary" :disabled="!form.weekStart || !form.completedWork || !form.nextWeekPlan" @click="save">Save draft</el-button></template>
    </el-dialog>
    <el-dialog :model-value="Boolean(detail)" title="Weekly report details" width="680px" @close="detail=null">
      <template v-if="detail"><div class="report-detail"><div><label>Period</label><p>{{ detail.weekStart }} to {{ detail.weekEnd }}</p></div><div><label>Completed work</label><p>{{ detail.completedWork }}</p></div><div><label>Current problems</label><p>{{ detail.currentProblems || 'None' }}</p></div><div><label>Next week plan</label><p>{{ detail.nextWeekPlan }}</p></div><div v-if="detail.review && !isMentor"><label>Mentor comments</label><p class="review-box">{{ detail.review.content }}</p></div></div><el-form v-if="isMentor && detail.status !== 'DRAFT'" label-position="top" class="review-form"><el-form-item label="Review comments" required><el-input v-model="reviewText" type="textarea" :rows="4" /></el-form-item></el-form></template>
      <template #footer><el-button @click="detail=null">Close</el-button><el-button v-if="isMentor && detail?.status !== 'DRAFT'" type="primary" :icon="Check" :disabled="!reviewText.trim()" @click="review">Save review</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.report-detail { display: grid; gap: 16px; }.report-detail label { color: var(--muted); font-size: 12px; }.report-detail p { margin: 5px 0 0; line-height: 1.7; white-space: pre-wrap; }.review-box { padding: 12px; background: #f1f7f4; border-left: 3px solid var(--green); }.review-form { margin-top: 22px; padding-top: 18px; border-top: 1px solid var(--line); }
</style>
