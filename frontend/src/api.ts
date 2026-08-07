import axios from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResponse<T> { code: string; message: string; data: T; requestId: string }

export const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api', timeout: 20000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ph_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => {
    if (response.data?.code && response.data.code !== 'SUCCESS') return Promise.reject(new Error(response.data.message))
    return response
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || (status === 403 ? '没有访问权限' : '请求失败，请稍后重试')
    if (status === 401) {
      localStorage.removeItem('ph_token')
      localStorage.removeItem('ph_user')
      if (location.pathname !== '/login') location.assign('/login')
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)
