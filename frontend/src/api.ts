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
    const message = status === 401 ? 'Your session has expired. Please sign in again.'
      : status === 403 ? 'You do not have permission to perform this action.'
        : status === 400 ? 'The request could not be completed. Check the submitted values.'
          : status && status >= 500 ? 'The server is temporarily unavailable.'
            : 'The request failed. Please try again.'
    if (status === 401) {
      localStorage.removeItem('ph_token')
      localStorage.removeItem('ph_user')
      if (location.pathname !== '/login') location.assign('/login')
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)
