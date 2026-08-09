import { defineStore } from 'pinia'
import { api, type ApiResponse } from '../api'

export interface UserInfo {
  id: string; username: string; realName: string; role: 'ADMIN' | 'MENTOR' | 'STUDENT'
  studentNo?: string; teacherNo?: string; collegeId?: string; majorId?: string; mentorId?: string; status?: 'ACTIVE' | 'DISABLED'
}

export const useAuthStore = defineStore('auth', {
  state: () => ({ user: JSON.parse(localStorage.getItem('ph_user') || 'null') as UserInfo | null }),
  getters: { isAuthenticated: () => Boolean(localStorage.getItem('ph_token')) },
  actions: {
    async login(username: string, password: string) {
      const { data } = await api.post<ApiResponse<{ token: string; user: UserInfo }>>('/auth/login', { username, password })
      localStorage.setItem('ph_token', data.data.token)
      localStorage.setItem('ph_user', JSON.stringify(data.data.user))
      this.user = data.data.user
    },
    async loadMe() {
      const { data } = await api.get<ApiResponse<UserInfo>>('/auth/me')
      this.user = data.data
      localStorage.setItem('ph_user', JSON.stringify(data.data))
    },
    logout() {
      localStorage.removeItem('ph_token')
      localStorage.removeItem('ph_user')
      this.user = null
    }
  }
})
