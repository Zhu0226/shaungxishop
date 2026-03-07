<template>
  <div class="page">
    <router-link to="/" class="back-link">
      <span class="icon">←</span> 返回星际枢纽
    </router-link>

    <div v-if="loading" class="state-tip">数据同步中...</div>
    <div v-else-if="error" class="state-tip error">{{ error }}</div>
    
    <div v-else-if="goods" class="glass-panel detail-card">
      <div class="user-badge">
        ID: <input v-model.number="userId" type="number" @change="saveUserId" class="inline-input"/>
      </div>
      
      <h1 class="title">{{ goods.goodsName }}</h1>
      <div class="price-huge">¥{{ goods.price }}</div>
      
      <div class="status-panel glass-panel">
        <div class="status-item">
          <span class="label">当前状态</span>
          <span class="value" :class="statusColorClass">{{ status?.statusDesc || '未知' }}</span>
        </div>
        <div class="status-item">
          <span class="label">剩余库存</span>
          <span class="value font-mono">{{ status?.remainStock ?? '-' }}</span>
        </div>
        <div class="status-item full-width">
          <span class="label">活动窗口</span>
          <span class="value">{{ activityTime }}</span>
        </div>
      </div>

      <div v-if="myOrder" class="success-banner glass-panel">
        🎉 抢购成功！订单 #{{ myOrder.id }} [{{ orderStatusText(myOrder.status) }}]
      </div>

      <div class="action-area">
        <button 
          class="btn-seckill"
          :class="{ 'processing': seckilling }"
          :disabled="status?.status !== 1 || seckilling || !!myOrder" 
          @click="handleSeckill"
        >
          {{ myOrder ? '已入舱' : (seckilling ? '数据跃迁中...' : (status?.status === 1 ? '启动抢购协议' : '协议未开启')) }}
        </button>
      </div>
      
      <transition name="fade">
        <p v-if="result" class="result-msg" :class="resultSuccess ? 'success' : 'fail'">
          {{ result }}
        </p>
      </transition>
    </div>
  </div>
</template>

<script setup>
// Script 逻辑保持与之前完全一致，仅保留样式修改
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getGoodsDetail, getStatus, getPath, doSeckill, getOrderResult } from '../api/seckill.js'

const route = useRoute()
const goodsId = computed(() => Number(route.params.id))
const userId = ref(Number(localStorage.getItem('userId')) || 1001)

function saveUserId() {
  localStorage.setItem('userId', String(userId.value))
  load()
}

const activityTime = computed(() => {
  const g = goods.value
  if (!g?.startTime || !g?.endTime) return ''
  const s = new Date(g.startTime).toLocaleString('zh-CN')
  const e = new Date(g.endTime).toLocaleString('zh-CN')
  return `${s} ~ ${e}`
})

const goods = ref(null)
const status = ref(null)
const myOrder = ref(null)
const loading = ref(true)
const error = ref('')
const seckilling = ref(false)
const result = ref('')
const resultSuccess = ref(false)

const statusColorClass = computed(() => {
  if (status.value?.status === 1) return 'text-success'
  if (status.value?.status === 3) return 'text-danger'
  return 'text-muted'
})

function orderStatusText(s) {
  const map = { 0: '待支付', 1: '已完成', 2: '已取消' }
  return map[s] ?? '未知'
}

async function load() {
  try {
    const [gRes, sRes, oRes] = await Promise.all([
      getGoodsDetail(goodsId.value, userId.value),
      getStatus(goodsId.value, userId.value),
      getOrderResult(goodsId.value, userId.value).catch(() => ({ code: 404 })),
    ])
    if (gRes.code === 0) goods.value = gRes.data
    if (sRes.code === 0) status.value = sRes.data
    if (oRes.code === 0 && oRes.data) myOrder.value = oRes.data
    else myOrder.value = null
    if (gRes.code !== 0) error.value = gRes.msg || '加载失败'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function handleSeckill() {
  if (!goods.value || status.value?.status !== 1 || seckilling.value) return
  seckilling.value = true
  result.value = ''
  try {
    const pathRes = await getPath(goodsId.value, userId.value)
    if (pathRes.code !== 0) {
      result.value = pathRes.msg || '获取路径失败'
      seckilling.value = false
      return
    }
    const doRes = await doSeckill(pathRes.data, goodsId.value, userId.value)
    if (doRes.code === 0) {
      resultSuccess.value = true
      result.value = doRes.data || '请求已受理，排队抢购中...'
      startPolling()
    } else {
      resultSuccess.value = false
      result.value = doRes.msg || '抢购失败'
      seckilling.value = false
    }
  } catch (e) {
    result.value = e.message; resultSuccess.value = false; seckilling.value = false
  }
}

function startPolling() {
  let attempts = 0
  const timer = setInterval(async () => {
    attempts++
    try {
      const oRes = await getOrderResult(goodsId.value, userId.value)
      if (oRes.code === 0 && oRes.data) {
        clearInterval(timer)
        myOrder.value = oRes.data
        result.value = '抢购成功！订单已生成'
        status.value = { ...status.value, remainStock: Math.max(0, (status.value?.remainStock ?? 1) - 1) }
        seckilling.value = false
      } else if (attempts >= 10) {
        clearInterval(timer)
        result.value = '排队超时，请前往“我的订单”查询结果'
        resultSuccess.value = false; seckilling.value = false
      }
    } catch (e) {
      if (attempts >= 10) {
        clearInterval(timer); result.value = '排队超时异常'; resultSuccess.value = false; seckilling.value = false
      }
    }
  }, 1000)
}

onMounted(load)
watch(() => route.params.id, () => { loading.value = true; error.value = ''; load() })
</script>

<style scoped>
.page { max-width: 680px; margin: 0 auto; }

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  color: var(--obsidian-muted);
}
.back-link:hover { color: var(--obsidian-text); }

.detail-card {
  padding: 40px;
  position: relative;
}

.user-badge {
  position: absolute;
  top: 24px;
  right: 24px;
  font-size: 12px;
  color: var(--obsidian-muted);
  display: flex;
  align-items: center;
  gap: 8px;
}
.inline-input {
  width: 60px;
  padding: 4px 8px;
  font-size: 12px;
  background: transparent;
}

.title {
  font-size: 32px;
  margin: 0 0 8px 0;
  background: linear-gradient(to right, #fff, #94a3b8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.price-huge {
  font-size: 48px;
  font-weight: 800;
  color: var(--accent-color);
  margin-bottom: 32px;
  text-shadow: 0 0 20px rgba(124, 58, 237, 0.4);
}

.status-panel {
  background: rgba(0, 0, 0, 0.2); /* 更深的一层玻璃 */
  box-shadow: none;
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 32px;
}

.status-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 120px;
}
.status-item.full-width { width: 100%; }

.label { font-size: 12px; color: var(--obsidian-muted); text-transform: uppercase; letter-spacing: 1px; }
.value { font-size: 16px; font-weight: 500; }
.font-mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; }

.text-success { color: var(--success-color); }
.text-danger { color: var(--danger-color); }
.text-muted { color: var(--obsidian-muted); }

.success-banner {
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.3);
  color: var(--success-color);
  padding: 16px;
  border-radius: var(--radius-md);
  margin-bottom: 32px;
  text-align: center;
  font-weight: bold;
}

.action-area {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.btn-seckill {
  width: 100%;
  padding: 16px;
  font-size: 18px;
  border-radius: var(--radius-md);
  letter-spacing: 2px;
}
.btn-seckill.processing {
  background: var(--accent-hover);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(124, 58, 237, 0.4); }
  70% { box-shadow: 0 0 0 15px rgba(124, 58, 237, 0); }
  100% { box-shadow: 0 0 0 0 rgba(124, 58, 237, 0); }
}

.result-msg {
  text-align: center;
  margin-top: 16px;
  padding: 12px;
  border-radius: var(--radius-sm);
  background: rgba(0,0,0,0.2);
}
.result-msg.success { color: var(--success-color); }
.result-msg.fail { color: var(--danger-color); }
</style>