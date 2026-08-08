<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Collection, Files, House, List, Menu as MenuIcon, School, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const mobileOpen = ref(false)
const items = computed(() => [
  { path: '/dashboard', label: 'Dashboard', icon: House, visible: true },
  { path: '/tasks', label: 'Tasks', icon: List, visible: auth.user?.role !== 'ADMIN' },
  { path: '/reports', label: 'Weekly Reports', icon: Files, visible: auth.user?.role !== 'ADMIN' },
  { path: '/members', label: 'Members', icon: School, visible: true },
  { path: '/knowledge', label: 'Knowledge Base', icon: Collection, visible: true },
  { path: '/ai', label: 'AI Assistant', icon: ChatDotRound, visible: true }
].filter(i => i.visible))

onMounted(() => { auth.loadMe().catch(() => undefined) })
function navigate(path: string) { mobileOpen.value = false; router.push(path) }
function logout() { auth.logout(); router.replace('/login') }
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
        <div class="avatar">{{ auth.user?.realName?.slice(0, 1) || '?' }}</div>
        <div class="user-copy"><strong>{{ auth.user?.realName || 'Loading' }}</strong><small>{{ auth.user?.role }}</small></div>
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
  </div>
</template>
