import axios from 'axios'
import router from '@/router'

const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (res) => {
    // blob 响应返回完整 response 对象，以便调用方访问 res.data (Blob) 和 res.headers
    if (res.config.responseType === 'blob') {
      return res
    }
    return res.data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    return Promise.reject(error.response?.data || error)
  },
)

export default apiClient
