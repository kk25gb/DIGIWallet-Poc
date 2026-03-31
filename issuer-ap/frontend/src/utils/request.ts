import axios from 'axios'
import router from '@/router'

const request = axios.create({
  baseURL: '/api/issuer-ap',
  timeout: 15000,
})

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('ap_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('ap_token')
      router.push({ name: 'Login' })
    }
    return Promise.reject(error)
  },
)

export default request
