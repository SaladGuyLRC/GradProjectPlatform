<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, type ApiResponse } from '../api'
import { useAuthStore } from '../stores/auth'

interface Task { id: string; title: string; deadlineAt: string }
interface Dashboard {
  studentProfile: { realName: string; studentNo: string; college: string; major: string }
  mentorProfile?: { realName: string; teacherNo: string }
  project?: { title: string; status: string; progressPercentage?: number }
  currentWeekReport?: { status: string; progressPercentage: number }
  deadlineBuckets: { overdue: Bucket; nextThreeDays: Bucket; nextSevenDays: Bucket }
}
interface Bucket { count: number; items: Task[] }
const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const data = ref<Dashboard | null>(null)
const roleName = computed(() => ({ ADMIN: 'Administrator', MENTOR: 'Mentor', STUDENT: 'Student' }[auth.user?.role || 'STUDENT']))
onMounted(async () => {
  if (auth.user?.role !== 'STUDENT') return
  loading.value = true
  try { data.value = (await api.get<ApiResponse<Dashboard>>('/dashboard/student')).data.data }
  finally { loading.value = false }
})
const formatDate = (value: string) => new Date(value).toLocaleString('en-GB', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
</script>

<template>
  <div v-loading="loading">
    <header class="page-header"><div><h1>Welcome, {{ data?.studentProfile.realName || auth.user?.realName }}</h1><p>{{ roleName }} dashboard · {{ data?.studentProfile.major || 'Graduation project collaboration' }}</p></div></header>
    <template v-if="auth.user?.role === 'STUDENT' && data">
      <div class="grid-3">
        <article class="metric danger"><label>Overdue tasks</label><strong>{{ data.deadlineBuckets.overdue.count }}</strong><small>Needs your attention</small></article>
        <article class="metric warn"><label>Due in 3 days</label><strong>{{ data.deadlineBuckets.nextThreeDays.count }}</strong><small>Plan your near-term work</small></article>
        <article class="metric"><label>Due in 3–7 days</label><strong>{{ data.deadlineBuckets.nextSevenDays.count }}</strong><small>Keep your plan on track</small></article>
      </div>
      <div class="grid-2 dashboard-sections">
        <section class="surface"><h2 class="section-title">Profile & project</h2><dl class="info-list"><dt>Student number</dt><dd>{{ data.studentProfile.studentNo }}</dd><dt>College / major</dt><dd>{{ data.studentProfile.college }} / {{ data.studentProfile.major }}</dd><dt>Mentor</dt><dd>{{ data.mentorProfile?.realName || 'Not assigned' }}</dd><dt>Project</dt><dd>{{ data.project?.title || 'Not created' }}</dd><dt>Project status</dt><dd><el-tag size="small">{{ data.project?.status || 'None' }}</el-tag></dd><dt>Current report</dt><dd><el-tag size="small" type="success">{{ data.currentWeekReport?.status || 'Not created' }}</el-tag></dd></dl></section>
        <section class="surface"><h2 class="section-title">Upcoming deadlines</h2><div v-if="![...data.deadlineBuckets.overdue.items, ...data.deadlineBuckets.nextThreeDays.items].length" class="empty-state">No upcoming deadlines</div><ul v-else class="deadline-list"><li v-for="task in [...data.deadlineBuckets.overdue.items, ...data.deadlineBuckets.nextThreeDays.items]" :key="task.id"><span>{{ task.title }}</span><time :class="{ danger: new Date(task.deadlineAt) < new Date() }">{{ formatDate(task.deadlineAt) }}</time></li></ul></section>
      </div>
    </template>
    <section v-else class="surface role-home"><h2>{{ roleName }} workspace</h2><p>{{ auth.user?.role === 'MENTOR' ? 'Review your students’ tasks and weekly reports.' : 'Manage members and the public graduation project knowledge base.' }}</p><el-button type="primary" @click="router.push(auth.user?.role === 'MENTOR' ? '/reports' : '/members')">Open workspace</el-button></section>
  </div>
</template>

<style scoped>
.dashboard-sections { margin-top: 18px; }.info-list { display: grid; grid-template-columns: 110px 1fr; gap: 14px 18px; margin: 0; font-size: 14px; }.info-list dt { color: var(--muted); }.info-list dd { margin: 0; font-weight: 550; }.deadline-list { margin: 0; padding: 0; list-style: none; }.deadline-list li { display: flex; justify-content: space-between; gap: 15px; padding: 13px 0; border-bottom: 1px solid #edf0ee; font-size: 14px; }.deadline-list time { white-space: nowrap; color: var(--muted); }.role-home { max-width: 760px; padding: 34px; }.role-home h2 { margin-top: 0; }.role-home p { color: var(--muted); margin-bottom: 24px; }
</style>
