import axios, { type AxiosInstance } from 'axios'
import type { ApiResult } from '@/types'

const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data as ApiResult<any>
    if (body && typeof body.code === 'number') {
      if (body.code !== 0) {
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(body)
      }
      return body.data
    }
    return resp.data
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem('accessToken')
      if (location.hash !== '#/login') location.hash = '#/login'
      ElMessage.error('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error('无访问权限')
    } else {
      ElMessage.error(error?.response?.data?.message || error.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export default http
