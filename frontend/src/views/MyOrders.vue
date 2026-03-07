<template>
  <div class="page">
    <header class="header">
      <router-link to="/">← 返回首页</router-link>
      <div class="user-bar">
        <label>用户ID</label>
        <input v-model.number="userId" type="number" @change="load" />
      </div>
    </header>
    <h2>我的订单</h2>
    <div v-if="loading" class="tip">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <ul v-else class="order-list">
      <li v-for="o in orders" :key="o.id" class="order-card">
        <div>订单#{{ o.id }} | 商品ID {{ o.goodsId }}</div>
        <div>状态: {{ statusText(o.status) }}</div>
        <div class="time">{{ formatTime(o.createTime) }}</div>
        <div v-if="o.status === 0" class="actions">
          <button @click="pay(o.id)">支付</button>
          <button @click="cancel(o.id)">取消</button>
        </div>
      </li>
    </ul>
    <p v-if="!loading && !error && orders.length === 0">暂无订单</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listOrders, payOrder, cancelOrder } from '../api/seckill.js'

const userId = ref(Number(localStorage.getItem('userId')) || 1001)
const orders = ref([])
const loading = ref(true)
const error = ref('')

function statusText(s) {
  const map = { 0: '待支付', 1: '已支付', 2: '已取消' }
  return map[s] ?? '未知'
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await listOrders(userId.value)
    if (res.code === 0) orders.value = res.data || []
    else error.value = res.msg || '加载失败'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
  localStorage.setItem('userId', String(userId.value))
}

async function pay(id) {
  try {
    const res = await payOrder(id, userId.value)
    if (res.code === 0) load()
    else alert(res.msg || '支付失败')
  } catch (e) {
    alert(e.message)
  }
}

async function cancel(id) {
  try {
    const res = await cancelOrder(id, userId.value)
    if (res.code === 0) load()
    else alert(res.msg || '取消失败')
  } catch (e) {
    alert(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 20px; max-width: 600px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; margin-bottom: 20px; }
.user-bar input { width: 80px; padding: 6px; margin-left: 8px; }
.order-list { list-style: none; padding: 0; }
.order-card { border: 1px solid #eee; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.order-card .time { font-size: 12px; color: #999; margin-top: 4px; }
.order-card .actions { margin-top: 8px; }
.order-card button { margin-right: 8px; padding: 6px 12px; cursor: pointer; }
</style>
