import request from './request.js'

/**
 * C 端秒杀接口，需在 Header 中传 userId（模拟当前用户）
 */
function headers(userId) {
  return { headers: { userId: String(userId || 1001) } }
}

export function listGoods(userId) {
  return request.get('/seckill/goods', headers(userId))
}

export function getGoodsDetail(goodsId, userId) {
  return request.get(`/seckill/goods/${goodsId}`, headers(userId))
}

export function getStatus(goodsId, userId) {
  return request.get(`/seckill/status/${goodsId}`, headers(userId))
}

export function getPath(goodsId, userId) {
  return request.get(`/seckill/path?goodsId=${goodsId}`, headers(userId))
}

export function doSeckill(path, goodsId, userId) {
  return request.post(`/seckill/${path}/doSeckill?goodsId=${goodsId}`, null, headers(userId))
}

export function listOrders(userId) {
  return request.get('/seckill/orders', headers(userId))
}

export function getOrderResult(goodsId, userId) {
  return request.get(`/seckill/orders/result?goodsId=${goodsId}`, headers(userId))
}

export function payOrder(orderId, userId) {
  return request.post(`/seckill/orders/${orderId}/pay`, null, headers(userId))
}

export function cancelOrder(orderId, userId) {
  return request.post(`/seckill/orders/${orderId}/cancel`, null, headers(userId))
}

export function getMetrics() {
  return request.get('/seckill/metrics')
}
