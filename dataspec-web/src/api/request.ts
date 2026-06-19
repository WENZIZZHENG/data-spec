import axios from 'axios'
import { ElMessage } from 'element-plus'
import { AUTH_CLEARED_EVENT, clearAuthStorage, readAuthToken } from '@/api/authStorage'

// 创建 Axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = readAuthToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：解包 R<T> 格式响应
request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response.data
    }
    const res = response.data
    // 约定 code === 200 为成功
    if (res.code === 200) {
      return res.data
    }
    // 业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络错误'
    if (error.response?.status === 401 || error.response?.status === 403) {
      clearAuthStorage()
      window.dispatchEvent(new Event(AUTH_CLEARED_EVENT))
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
