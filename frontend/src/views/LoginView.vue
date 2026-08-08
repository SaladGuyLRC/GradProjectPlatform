<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
async function submit() {
  if (!form.username || !form.password) return
  loading.value = true
  try { await auth.login(form.username, form.password); await router.replace('/dashboard') }
  finally { loading.value = false }
}
</script>

<template>
  <main class="login-page">
    <section class="login-brand">
      <span class="brand-mark">PH</span>
      <h1>ProjectHelper</h1>
      <p>Graduation Project Hub</p>
    </section>
    <section class="login-form-wrap">
      <div class="login-form">
        <div><h2>Sign in</h2><p>Use the account provided by your school</p></div>
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="Username"><el-input v-model="form.username" :prefix-icon="User" size="large" autocomplete="username" /></el-form-item>
          <el-form-item label="Password"><el-input v-model="form.password" :prefix-icon="Lock" type="password" show-password size="large" autocomplete="current-password" @keyup.enter="submit" /></el-form-item>
          <el-button native-type="submit" type="primary" size="large" :loading="loading" :disabled="!form.username || !form.password">Sign in</el-button>
        </el-form>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: minmax(300px, 42%) 1fr; background: white; }
.login-brand { display: flex; flex-direction: column; justify-content: center; padding: clamp(40px, 8vw, 100px); color: white; background: #172723; }
.login-brand .brand-mark { width: 50px; height: 50px; flex: 0 0 50px; }.login-brand h1 { margin: 26px 0 8px; font-size: 38px; letter-spacing: 0; }.login-brand p { margin: 0; color: #b7c9c2; font-size: 17px; }
.login-form-wrap { display: grid; place-items: center; padding: 28px; }.login-form { width: min(390px, 100%); }.login-form h2 { margin: 0; font-size: 28px; letter-spacing: 0; }.login-form > div > p { margin: 8px 0 28px; color: #697772; }.login-form .el-button { width: 100%; margin-top: 8px; }
@media (max-width: 700px) { .login-page { grid-template-columns: 1fr; }.login-brand { min-height: 210px; padding: 38px 28px; }.login-brand h1 { margin-top: 18px; font-size: 30px; }.login-form-wrap { align-items: start; padding-top: 42px; } }
</style>
