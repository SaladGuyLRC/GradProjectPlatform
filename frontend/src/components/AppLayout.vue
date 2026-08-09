<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Collection, Files, House, Key, List, Menu as MenuIcon, School, SwitchButton, FolderOpened } from '@element-plus/icons-vue'
import { api } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const mobileOpen = ref(false)
const passwordDialog = ref(false)
const savingPassword = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const items = computed(() => [
  { path: '/dashboard', label: 'Dashboard', icon: House, visible: true },
  { path: '/tasks', label: 'Tasks', icon: List, visible: auth.user?.role !== 'ADMIN' },
  { path: '/reports', label: 'Weekly Reports', icon: Files, visible: auth.user?.role !== 'ADMIN' },
  { path: '/project', label: auth.user?.role === 'MENTOR' ? 'Student Projects' : 'My Project', icon: FolderOpened, visible: auth.user?.role === 'STUDENT' || auth.user?.role === 'MENTOR' },
  { path: '/members', label: 'Members', icon: School, visible: true },
  { path: '/knowledge', label: 'Knowledge Base', icon: Collection, visible: true },
  { path: '/ai', label: 'AI Assistant', icon: ChatDotRound, visible: true }
].filter(i => i.visible))

onMounted(() => { auth.loadMe().catch(() => undefined) })
function navigate(path: string) { mobileOpen.value = false; router.push(path) }
function logout() { auth.logout(); router.replace('/login') }
function openPasswordDialog() {
  Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  passwordDialog.value = true
}
async function changePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('Complete all password fields')
    return
  }
  if (passwordForm.newPassword.length < 8) {
    ElMessage.warning('New password must be at least 8 characters')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('New passwords do not match')
    return
  }
  savingPassword.value = true
  try {
    await api.put('/auth/password', { oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    passwordDialog.value = false
    ElMessage.success('Password changed successfully. Please sign in again.')
    logout()
  } catch {
  } finally {
    savingPassword.value = false
  }
}
</script>

<template>
  <div class="app-shell">
    <div class="mobile-topbar">
      <el-button :icon="MenuIcon" circle aria-label="Open navigation" @click="mobileOpen = true" />
      <strong>ProjectHelper</strong>
    </div>
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">PH</span><div><strong>ProjectHelper</strong><small>Graduation Project Hub</small></div></div>
      <nav>
        <button v-for="item in items" :key="item.path" :class="{ active: route.path === item.path }" @click="navigate(item.path)">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
        </button>
      </nav>
      <div class="user-panel">
        <el-dropdown trigger="click" placement="top-start" @command="$event === 'password' && openPasswordDialog()">
          <button class="user-identity" type="button" aria-label="Open account menu"><span class="avatar">{{ auth.user?.realName?.slice(0, 1) || '?' }}</span><span class="user-copy"><strong>{{ auth.user?.realName || 'Loading' }}</strong><small>{{ auth.user?.role }}</small></span></button>
          <template #dropdown><el-dropdown-menu><el-dropdown-item command="password"><el-icon><Key /></el-icon>Change password</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
        <el-button :icon="SwitchButton" text circle title="Log out" aria-label="Log out" @click="logout" />
      </div>
    </aside>
    <main class="main-content"><router-view /></main>
    <el-drawer v-model="mobileOpen" direction="ltr" size="260px" :with-header="false" class="mobile-drawer">
      <div class="brand"><span class="brand-mark">PH</span><strong>ProjectHelper</strong></div>
      <nav>
        <button v-for="item in items" :key="item.path" :class="{ active: route.path === item.path }" @click="navigate(item.path)">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
        </button>
      </nav>
    </el-drawer>
    <el-dialog v-model="passwordDialog" title="Change password" width="460px">
      <el-form label-position="top" @submit.prevent="changePassword">
        <el-form-item label="Current password" required><el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
        <el-form-item label="New password" required><el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
        <el-form-item label="Confirm new password" required><el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="passwordDialog=false">Cancel</el-button><el-button type="primary" :loading="savingPassword" @click="changePassword">Save password</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-identity { min-width: 0; display: flex; flex: 1; align-items: center; gap: 10px; padding: 0; border: 0; color: inherit; background: transparent; cursor: pointer; text-align: left; }
.user-identity:focus-visible { outline: 2px solid #b8ddcf; outline-offset: 3px; border-radius: 4px; }
.user-panel :deep(.el-dropdown) { min-width: 0; flex: 1; }
</style>
