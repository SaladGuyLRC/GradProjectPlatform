<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CircleClose, Delete, Edit, Refresh, School, User } from '@element-plus/icons-vue'
import { api, type ApiResponse } from '../api'
import { useAuthStore } from '../stores/auth'

type Role = 'ADMIN' | 'MENTOR' | 'STUDENT'
type Status = 'ACTIVE' | 'DISABLED'
type OrganizationType = 'COLLEGE' | 'MAJOR'

interface Organization { id: string; name: string; type: OrganizationType; parentId?: string; sortOrder: number; active: boolean }
interface User { id: string; username: string; realName: string; role: Role; status: Status; studentNo?: string; teacherNo?: string; collegeId?: string; majorId?: string; mentorId?: string }
interface TreeNode { id: string; name: string; studentNo?: string; majors?: TreeNode[]; mentors?: TreeNode[]; students?: TreeNode[]; children?: TreeNode[] }

const auth = useAuthStore()
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const activeTab = ref('tree')
const treeRef = ref<any>()
const tree = ref<TreeNode[]>([])
const users = ref<User[]>([])
const organizations = ref<Organization[]>([])
const loading = ref(false)
const query = ref('')
const memberDialog = ref(false)
const organizationDialog = ref(false)
const savingMember = ref(false)
const savingOrganization = ref(false)

const blankMember = () => ({
  id: '', username: '', initialPassword: '', realName: '', role: 'STUDENT' as Role,
  studentNo: '', teacherNo: '', collegeId: '', majorId: '', mentorId: ''
})
const memberForm = reactive(blankMember())
const organizationForm = reactive({ id: '', name: '', type: 'COLLEGE' as OrganizationType, parentId: '', sortOrder: 10 })

const colleges = computed(() => organizations.value.filter(org => org.type === 'COLLEGE' && org.active))
const majors = computed(() => organizations.value.filter(org => org.type === 'MAJOR' && org.active && org.parentId === memberForm.collegeId))
const mentors = computed(() => users.value.filter(user => user.role === 'MENTOR' && user.status === 'ACTIVE' && user.majorId === memberForm.majorId))
const organizationMap = computed(() => new Map(organizations.value.map(org => [org.id, org])))
const memberDialogTitle = computed(() => memberForm.id ? 'Edit member' : 'Add member')
const organizationDialogTitle = computed(() => organizationForm.id ? 'Edit organization' : 'Add organization')

function normalize(nodes: TreeNode[]): TreeNode[] {
  return nodes.map(node => ({ ...node, children: normalize(node.majors || node.mentors || node.students || []) }))
}

async function load() {
  loading.value = true
  try {
    const treeResponse = await api.get<ApiResponse<TreeNode[]>>('/members/tree')
    tree.value = normalize(treeResponse.data.data)
    if (isAdmin.value) {
      const [usersResponse, organizationsResponse] = await Promise.all([
        api.get<ApiResponse<User[]>>('/admin/users'),
        api.get<ApiResponse<Organization[]>>('/admin/organizations')
      ])
      users.value = usersResponse.data.data
      organizations.value = organizationsResponse.data.data
    }
  } finally {
    loading.value = false
  }
}

function filterTree() { treeRef.value?.filter(query.value) }
function roleLabel(role: Role) { return role === 'ADMIN' ? 'Admin' : role === 'MENTOR' ? 'Mentor' : 'Student' }
function organizationLabel(id?: string) { return id ? organizationMap.value.get(id)?.name || 'Unknown' : 'Not assigned' }

function openMember(user?: User) {
  Object.assign(memberForm, blankMember())
  if (user) Object.assign(memberForm, {
    id: user.id, username: user.username, realName: user.realName, role: user.role,
    studentNo: user.studentNo || '', teacherNo: user.teacherNo || '', collegeId: user.collegeId || '',
    majorId: user.majorId || '', mentorId: user.mentorId || ''
  })
  memberDialog.value = true
}

async function saveMember() {
  if (!memberForm.username.trim() || !memberForm.realName.trim()) return
  if (!memberForm.id && !memberForm.initialPassword.trim()) {
    ElMessage.warning('An initial password is required for a new member')
    return
  }
  if (memberForm.role !== 'ADMIN' && (!memberForm.collegeId || !memberForm.majorId)) {
    ElMessage.warning('Select a college and major for this member')
    return
  }
  if (memberForm.role === 'STUDENT' && (!memberForm.studentNo.trim() || !memberForm.mentorId)) {
    ElMessage.warning('Students need a student number and mentor')
    return
  }
  savingMember.value = true
  try {
    const payload = {
      username: memberForm.username,
      initialPassword: memberForm.initialPassword || null,
      realName: memberForm.realName,
      role: memberForm.role,
      studentNo: memberForm.role === 'STUDENT' ? memberForm.studentNo : null,
      teacherNo: memberForm.role === 'MENTOR' ? memberForm.teacherNo : null,
      collegeId: memberForm.role === 'ADMIN' ? null : memberForm.collegeId,
      majorId: memberForm.role === 'ADMIN' ? null : memberForm.majorId,
      mentorId: memberForm.role === 'STUDENT' ? memberForm.mentorId : null
    }
    if (memberForm.id) await api.put(`/admin/users/${memberForm.id}`, payload)
    else await api.post('/admin/users', payload)
    ElMessage.success(memberForm.id ? 'Member updated' : 'Member created')
    memberDialog.value = false
    await load()
  } finally {
    savingMember.value = false
  }
}

async function toggleStatus(user: User) {
  if (user.id === auth.user?.id) return
  const nextStatus: Status = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await ElMessageBox.confirm(`${nextStatus === 'ACTIVE' ? 'Enable' : 'Disable'} ${user.username}?`, `${nextStatus === 'ACTIVE' ? 'Enable' : 'Disable'} member`, { type: nextStatus === 'ACTIVE' ? 'info' : 'warning' })
  await api.patch(`/admin/users/${user.id}/status`, { status: nextStatus })
  ElMessage.success(nextStatus === 'ACTIVE' ? 'Member enabled' : 'Member disabled')
  await load()
}

function openOrganization(organization?: Organization) {
  Object.assign(organizationForm, { id: '', name: '', type: 'COLLEGE', parentId: '', sortOrder: 10 })
  if (organization) Object.assign(organizationForm, {
    id: organization.id, name: organization.name, type: organization.type,
    parentId: organization.parentId || '', sortOrder: organization.sortOrder
  })
  organizationDialog.value = true
}

async function saveOrganization() {
  if (!organizationForm.name.trim()) return
  if (organizationForm.type === 'MAJOR' && !organizationForm.parentId) {
    ElMessage.warning('Select a parent college for a major')
    return
  }
  savingOrganization.value = true
  try {
    const payload = {
      name: organizationForm.name,
      type: organizationForm.type,
      parentId: organizationForm.type === 'COLLEGE' ? null : organizationForm.parentId,
      sortOrder: organizationForm.sortOrder
    }
    if (organizationForm.id) await api.put(`/admin/organizations/${organizationForm.id}`, payload)
    else await api.post('/admin/organizations', payload)
    ElMessage.success(organizationForm.id ? 'Organization updated' : 'Organization created')
    organizationDialog.value = false
    await load()
  } finally {
    savingOrganization.value = false
  }
}

async function removeOrganization(organization: Organization) {
  await ElMessageBox.confirm(`Delete ${organization.name}? Child organizations and assigned members must be moved first.`, 'Delete organization', { type: 'warning' })
  await api.delete(`/admin/organizations/${organization.id}`)
  ElMessage.success('Organization deleted')
  await load()
}

onMounted(load)
const filterNode = (value: string, data: TreeNode) => !value || data.name.toLowerCase().includes(value.toLowerCase()) || Boolean(data.studentNo?.includes(value))
</script>

<template>
  <div>
    <header class="page-header">
      <div><h1>Members</h1><p>Colleges, majors, mentors and students</p></div>
      <div v-if="isAdmin" class="header-actions">
        <el-button type="primary" :icon="User" @click="openMember()">Add member</el-button>
        <el-button :icon="School" @click="openOrganization()">Add organization</el-button>
      </div>
    </header>

    <section class="surface">
      <el-tabs v-if="isAdmin" v-model="activeTab">
        <el-tab-pane label="Member tree" name="tree">
          <div class="toolbar"><el-input v-model="query" placeholder="Search name or student number" clearable style="max-width:320px" @input="filterTree" /><el-button :icon="Refresh" circle title="Refresh" @click="load" /></div>
          <el-tree ref="treeRef" v-loading="loading" :data="tree" node-key="id" :props="{ label: 'name', children: 'children' }" :filter-node-method="filterNode" default-expand-all class="member-tree"><template #default="{ data }"><span class="tree-node"><span>{{ data.name }}</span><small v-if="data.studentNo">{{ data.studentNo }}</small></span></template></el-tree>
        </el-tab-pane>
        <el-tab-pane label="User accounts" name="users">
          <div class="toolbar"><span class="muted">{{ users.length }} accounts</span><el-button :icon="Refresh" circle title="Refresh" @click="load" /></div>
          <el-table v-loading="loading" :data="users" empty-text="No members">
            <el-table-column label="Member" min-width="190"><template #default="{ row }"><strong>{{ row.realName }}</strong><div class="member-meta mono">{{ row.username }}</div></template></el-table-column>
            <el-table-column label="Role" width="110"><template #default="{ row }"><el-tag size="small">{{ roleLabel(row.role) }}</el-tag></template></el-table-column>
            <el-table-column label="Organization" min-width="210"><template #default="{ row }"><span>{{ organizationLabel(row.collegeId) }}</span><div class="member-meta">{{ organizationLabel(row.majorId) }}</div></template></el-table-column>
            <el-table-column label="Status" width="110"><template #default="{ row }"><el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status === 'ACTIVE' ? 'Active' : 'Disabled' }}</el-tag></template></el-table-column>
            <el-table-column label="Actions" width="135" fixed="right"><template #default="{ row }"><el-button :icon="Edit" text circle title="Edit member" @click="openMember(row)" /><el-button v-if="row.id !== auth.user?.id" :icon="row.status === 'ACTIVE' ? CircleClose : CircleCheck" text circle :type="row.status === 'ACTIVE' ? 'danger' : 'success'" :title="row.status === 'ACTIVE' ? 'Disable member' : 'Enable member'" @click="toggleStatus(row)" /></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="Organizations" name="organizations">
          <div class="toolbar"><span class="muted">{{ organizations.length }} organizations</span><el-button :icon="Refresh" circle title="Refresh" @click="load" /></div>
          <el-table v-loading="loading" :data="organizations" empty-text="No organizations">
            <el-table-column prop="name" label="Name" min-width="220" />
            <el-table-column label="Type" width="120"><template #default="{ row }">{{ row.type === 'COLLEGE' ? 'College' : 'Major' }}</template></el-table-column>
            <el-table-column label="Parent college" min-width="220"><template #default="{ row }">{{ row.type === 'MAJOR' ? organizationLabel(row.parentId) : '—' }}</template></el-table-column>
            <el-table-column prop="sortOrder" label="Order" width="90" />
            <el-table-column label="Actions" width="135" fixed="right"><template #default="{ row }"><el-button :icon="Edit" text circle title="Edit organization" @click="openOrganization(row)" /><el-button :icon="Delete" text circle type="danger" title="Delete organization" @click="removeOrganization(row)" /></template></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template v-else>
        <div class="toolbar"><el-input v-model="query" placeholder="Search name or student number" clearable style="max-width:320px" @input="filterTree" /><el-button :icon="Refresh" circle title="Refresh" @click="load" /></div>
        <el-tree ref="treeRef" v-loading="loading" :data="tree" node-key="id" :props="{ label: 'name', children: 'children' }" :filter-node-method="filterNode" default-expand-all class="member-tree"><template #default="{ data }"><span class="tree-node"><span>{{ data.name }}</span><small v-if="data.studentNo">{{ data.studentNo }}</small></span></template></el-tree>
      </template>
    </section>

    <el-dialog v-model="memberDialog" :title="memberDialogTitle" width="620px">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="Username" required><el-input v-model="memberForm.username" :disabled="Boolean(memberForm.id)" autocomplete="username" /></el-form-item>
          <el-form-item label="Display name" required><el-input v-model="memberForm.realName" /></el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="Role" required><el-select v-model="memberForm.role" style="width:100%"><el-option label="Admin" value="ADMIN" /><el-option label="Mentor" value="MENTOR" /><el-option label="Student" value="STUDENT" /></el-select></el-form-item>
          <el-form-item v-if="!memberForm.id" label="Initial password" required><el-input v-model="memberForm.initialPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
        </div>
        <template v-if="memberForm.role !== 'ADMIN'">
          <div class="form-grid">
            <el-form-item label="College" required><el-select v-model="memberForm.collegeId" clearable style="width:100%" @change="memberForm.majorId=''; memberForm.mentorId=''" ><el-option v-for="college in colleges" :key="college.id" :label="college.name" :value="college.id" /></el-select></el-form-item>
            <el-form-item label="Major" required><el-select v-model="memberForm.majorId" clearable :disabled="!memberForm.collegeId" style="width:100%" @change="memberForm.mentorId=''" ><el-option v-for="major in majors" :key="major.id" :label="major.name" :value="major.id" /></el-select></el-form-item>
          </div>
          <div class="form-grid">
            <el-form-item v-if="memberForm.role === 'STUDENT'" label="Student number" required><el-input v-model="memberForm.studentNo" /></el-form-item>
            <el-form-item v-else label="Teacher number"><el-input v-model="memberForm.teacherNo" /></el-form-item>
            <el-form-item v-if="memberForm.role === 'STUDENT'" label="Mentor" required><el-select v-model="memberForm.mentorId" clearable :disabled="!memberForm.majorId" style="width:100%"><el-option v-for="mentor in mentors" :key="mentor.id" :label="mentor.realName" :value="mentor.id" /></el-select></el-form-item>
          </div>
        </template>
      </el-form>
      <template #footer><el-button @click="memberDialog=false">Cancel</el-button><el-button type="primary" :loading="savingMember" @click="saveMember">Save member</el-button></template>
    </el-dialog>

    <el-dialog v-model="organizationDialog" :title="organizationDialogTitle" width="520px">
      <el-form label-position="top">
        <el-form-item label="Name" required><el-input v-model="organizationForm.name" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="Type" required><el-select v-model="organizationForm.type" style="width:100%" @change="organizationForm.parentId=''" ><el-option label="College" value="COLLEGE" /><el-option label="Major" value="MAJOR" /></el-select></el-form-item>
          <el-form-item v-if="organizationForm.type === 'MAJOR'" label="Parent college" required><el-select v-model="organizationForm.parentId" style="width:100%"><el-option v-for="college in colleges" :key="college.id" :label="college.name" :value="college.id" /></el-select></el-form-item>
          <el-form-item label="Display order"><el-input-number v-model="organizationForm.sortOrder" :min="0" :max="999" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="organizationDialog=false">Cancel</el-button><el-button type="primary" :loading="savingOrganization" @click="saveOrganization">Save organization</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-actions { display:flex; flex-wrap:wrap; gap:10px; }
.member-tree { --el-tree-node-hover-bg-color: #f1f7f4; }.member-tree :deep(.el-tree-node__content) { height: 42px; border-bottom: 1px solid #f0f3f1; }
.tree-node { display:flex; align-items:center; gap:10px; font-size:14px; }.tree-node small { color: var(--muted); font-family: ui-monospace,monospace; }
.member-meta { margin-top:4px; color:var(--muted); font-size:12px; }.form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:0 16px; }
@media (max-width: 640px) { .form-grid { grid-template-columns:1fr; } .header-actions { width:100%; }.header-actions .el-button { flex:1; } }
</style>
