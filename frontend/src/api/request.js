import axios from 'axios'

const request = axios.create({
  baseURL: '',
  timeout: 15000,
})

// 统一拦截器：不管是前台还是后台请求，统统带上唯一的 token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function extractErrorMessage(err) {
  const data = err.response?.data
  if (data && typeof data === 'object' && 'msg' in data) {
    return data.msg
  }
  if (err.response?.status === 429) return '请求太频繁，请稍后再试'
  if (err.response?.status === 403) return '权限不足，拒绝访问'
  if (err.response?.status === 404) return '接口或资源不存在'
  if (err.response?.status === 503) return '缓存服务暂时不可用'
  if (err.response?.status >= 500) return '系统繁忙，请稍后重试'
  return err.message || '请求失败'
}

request.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401) {
      // 统一过期处理
      localStorage.clear()
      window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
      return Promise.reject(new Error('身份已过期，请重新登录'))
    }
    return Promise.reject(new Error(extractErrorMessage(err)))
  }
)

export default request