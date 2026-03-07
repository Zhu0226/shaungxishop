import request from './request.js'

// 获取商品列表
export function listGoods() {
  return request.get('/admin/seckill/goods')
}

// 新增商品
export function createGoods(data) {
  return request.post('/admin/seckill/goods', data)
}

// 修改商品
export function updateGoods(id, data) {
  return request.put(`/admin/seckill/goods/${id}`, data)
}

// 删除商品
export function deleteGoods(id) {
  return request.delete(`/admin/seckill/goods/${id}`)
}

// 预热库存 (注意这里的路径取决于你 SeckillController 里的定义，通常是 /seckill/preheat)
export function preheat(id, stock) {
  return request.post(`/seckill/preheat/${id}/${stock}`) 
  // 如果你的后端预热是 GET 请求，请改为 request.get(...)
}