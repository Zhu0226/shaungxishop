import request from './request.js'

// 统一登录接口
export function login(username, password) {
  return request.post('/auth/login', { username, password })
}