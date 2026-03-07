<template>
  <div class="page">
    <header class="header">
      <h1>秒杀指标</h1>
      <button @click="load">刷新</button>
    </header>
    <div v-if="loading" class="tip">加载中...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else-if="metrics" class="metrics">
      <div class="card">
        <div class="label">总请求数</div>
        <div class="value">{{ metrics.totalRequests }}</div>
      </div>
      <div class="card success">
        <div class="label">成功下单</div>
        <div class="value">{{ metrics.successCount }}</div>
      </div>
      <div class="card fail">
        <div class="label">失败次数</div>
        <div class="value">{{ metrics.failCount }}</div>
      </div>
      <div class="card">
        <div class="label">成功率</div>
        <div class="value">{{ (metrics.successRate || 0).toFixed(2) }}%</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMetrics } from '../api/seckill.js'

const metrics = ref(null)
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await getMetrics()
    if (res.code === 0) metrics.value = res.data
    else error.value = res.msg || '加载失败'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding: 20px; max-width: 600px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.metrics { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.card { border: 1px solid #eee; border-radius: 8px; padding: 20px; text-align: center; }
.card .label { font-size: 14px; color: #666; margin-bottom: 8px; }
.card .value { font-size: 24px; font-weight: bold; }
.card.success .value { color: #52c41a; }
.card.fail .value { color: #e4393c; }
</style>
