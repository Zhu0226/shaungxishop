<template>
  <div class="page container">
    <div class="floor-title">
      <h2>双喜秒杀 <span>| 限量折扣，手慢无</span></h2>
    </div>

    <div v-if="loadingList" class="tip">数据加载中...</div>
    <div v-else-if="errorList" class="tip error-text">{{ errorList }}</div>
    
    <div v-else class="split-layout">
      <div class="left-list">
        <div class="goods-grid">
          <div 
            class="jd-card goods-item" 
            v-for="g in goodsList" 
            :key="g.id" 
            :class="{ 'active': selectedGoodsId === g.id }"
            @click="selectGoods(g.id)"
          >
            <div class="img-placeholder">
              <span v-if="g.activityTag" class="tag">{{ g.activityTag }}</span>
              <span class="icon">📦</span>
            </div>
            <div class="info">
              <div class="name" :title="g.goodsName">{{ g.goodsName }}</div>
              <div class="price-row">
                <span class="currency">¥</span>
                <span class="price">{{ g.price }}</span>
              </div>
              <div class="meta">
                <span class="stock">库存: {{ g.stock }}件</span>
              </div>
              <div class="time-box">
                {{ formatTime(g.startTime) }} 开始
              </div>
            </div>
          </div>
        </div>
      </div>

      <transition name="ios-slide">
        <div class="right-detail-wrapper" v-if="selectedGoodsId">
          <div class="right-detail">
            <div class="jd-card detail-panel">
              <button class="close-btn" @click="closeDetail" title="关闭详情">✕</button>
              
              <div v-if="loadingDetail" class="tip">加载详情中...</div>
              <div v-else-if="errorDetail" class="tip error-text">{{ errorDetail }}</div>
              
              <div v-else-if="goodsDetail" class="detail-content">
                <div class="detail-img-box">🎁</div>
                
                <div class="detail-price-box">
                  <span class="currency">¥</span>
                  <span class="price-huge">{{ goodsDetail.price }}</span>
                </div>
                
                <h1 class="detail-title">{{ goodsDetail.goodsName }}</h1>
                
                <div class="status-box">
                  <div class="status-item">
                    <span class="label">抢购状态：</span>
                    <span class="value font-bold" :class="statusColorClass">
                      {{ status?.statusDesc || '未知' }}
                    </span>
                  </div>
                  <div class="status-item">
                    <span class="label">剩余库存：</span>
                    <span class="value text-jd-red font-bold">{{ status?.remainStock ?? '-' }} 件</span>
                  </div>
                  <div class="status-item">
                    <span class="label">活动窗口：</span>
                    <span class="value time-text">{{ activityTime }}</span>
                  </div>
                </div>

                <div v-if="myOrder" class="success-banner">
                  ✔ 抢购成功！订单编号 #{{ myOrder.id }} 
                  <br><span class="order-status">[{{ orderStatusText(myOrder.status) }}]</span>
                </div>

                <div class="action-area">
                  <button 
                    class="jd-btn buy-btn"
                    :class="{ 'processing': seckilling }"
                    :disabled="status?.status !== 1 || seckilling || !!myOrder" 
                    @click="handleSeckill"
                  >
                    {{ myOrder ? '已抢到，查看订单' : (seckilling ? '排队抢购中...' : (status?.status === 1 ? '立即抢购' : '暂不可抢')) }}
                  </button>
                  <p v-if="result" class="result-msg" :class="resultSuccess ? 'text-success' : 'text-danger'">
                    {{ result }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listGoods, getGoodsDetail, getStatus, getPath, doSeckill, getOrderResult } from '../api/seckill.js'

const router = useRouter()
const userId = ref(localStorage.getItem('userId') || '1001')

const goodsList = ref([])
const loadingList = ref(true)
const errorList = ref('')

const selectedGoodsId = ref(null)
const goodsDetail = ref(null)
const status = ref(null)
const myOrder = ref(null)
const loadingDetail = ref(false)
const errorDetail = ref('')
const seckilling = ref(false)
const result = ref('')
const resultSuccess = ref(false)
let pollTimer = null

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  return `${d.getMonth()+1}月${d.getDate()}日 ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

const activityTime = computed(() => {
  const g = goodsDetail.value
  if (!g?.startTime || !g?.endTime) return ''
  return `${formatTime(g.startTime)} ~ ${formatTime(g.endTime)}`
})

const statusColorClass = computed(() => {
  if (status.value?.status === 1) return 'text-jd-red'
  if (status.value?.status === 3) return 'text-muted'
  return 'text-main'
})

function orderStatusText(s) {
  const map = { 0: '待支付', 1: '已支付', 2: '已取消' }
  return map[s] ?? '未知'
}

async function loadList() {
  loadingList.value = true
  try {
    const res = await listGoods(userId.value)
    if (res.code === 0) goodsList.value = res.data || []
    else errorList.value = res.msg || '加载失败'
  } catch (e) {
    errorList.value = e.message
  } finally {
    loadingList.value = false
  }
}

function selectGoods(id) {
  if (selectedGoodsId.value === id) return
  selectedGoodsId.value = id
  loadDetail(id)
}

function closeDetail() {
  selectedGoodsId.value = null
  if (pollTimer) clearInterval(pollTimer)
}

async function loadDetail(id) {
  loadingDetail.value = true
  errorDetail.value = ''
  result.value = ''
  seckilling.value = false
  if (pollTimer) clearInterval(pollTimer)

  try {
    const [gRes, sRes, oRes] = await Promise.all([
      getGoodsDetail(id, userId.value),
      getStatus(id, userId.value),
      getOrderResult(id, userId.value).catch(() => ({ code: 404 })),
    ])
    if (gRes.code === 0) goodsDetail.value = gRes.data
    if (sRes.code === 0) status.value = sRes.data
    if (oRes.code === 0 && oRes.data) myOrder.value = oRes.data
    else myOrder.value = null
    
    if (gRes.code !== 0) errorDetail.value = gRes.msg || '加载详情失败'
  } catch (e) {
    errorDetail.value = e.message
  } finally {
    loadingDetail.value = false
  }
}

async function handleSeckill() {
  if (myOrder.value) {
    router.push('/orders')
    return
  }

  if (!goodsDetail.value || status.value?.status !== 1 || seckilling.value) return
  seckilling.value = true
  result.value = ''
  try {
    const pathRes = await getPath(selectedGoodsId.value, userId.value)
    if (pathRes.code !== 0) {
      result.value = pathRes.msg || '获取路径失败'; seckilling.value = false; return
    }
    const doRes = await doSeckill(pathRes.data, selectedGoodsId.value, userId.value)
    if (doRes.code === 0) {
      resultSuccess.value = true
      result.value = doRes.data || '请求已受理，排队中...'
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
  pollTimer = setInterval(async () => {
    attempts++
    try {
      const oRes = await getOrderResult(selectedGoodsId.value, userId.value)
      if (oRes.code === 0 && oRes.data) {
        clearInterval(pollTimer)
        myOrder.value = oRes.data
        result.value = '抢购成功！'
        status.value = { ...status.value, remainStock: Math.max(0, (status.value?.remainStock ?? 1) - 1) }
        seckilling.value = false
      } else if (attempts >= 10) {
        clearInterval(pollTimer)
        result.value = '排队超时，请前往订单页查询'
        resultSuccess.value = false; seckilling.value = false
      }
    } catch (e) {
      if (attempts >= 10) {
        clearInterval(pollTimer); result.value = '排队超时异常'; resultSuccess.value = false; seckilling.value = false
      }
    }
  }, 1000)
}

window.addEventListener('storage', () => {
  const newId = localStorage.getItem('userId')
  if (newId && newId !== userId.value) {
    userId.value = newId
    loadList()
    if (selectedGoodsId.value) loadDetail(selectedGoodsId.value)
  }
})

onMounted(() => {
  document.body.style.overflow = 'hidden'
  loadList()
})
import { onUnmounted } from 'vue'
onUnmounted(() => {
  document.body.style.overflow = ''
})
</script>

<style scoped>
.page.container {
  height: calc(100vh - 172px);
  display: flex;
  flex-direction: column;
  overflow: hidden; 
}

.split-layout {
  flex: 1; 
  display: flex;
  align-items: flex-start;
  position: relative;
  min-height: 0; 
}

.left-list {
  flex: 1;
  min-width: 0; 
  height: 100%; 
  overflow-y: auto; 
  padding-bottom: 40px; 
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.left-list::-webkit-scrollbar { display: none; }

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 16px;
  justify-content: flex-start;
}

.goods-item {
  width: 100%;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  border: 2px solid transparent;
  background: #fff;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  border-radius: var(--radius-sm);
}
.goods-item.active {
  border-color: var(--jd-red);
  box-shadow: 0 4px 16px rgba(225, 37, 27, 0.2);
  transform: translateY(-2px);
}

.img-placeholder {
  width: 100%; 
  height: auto;
  aspect-ratio: 1 / 1; 
  background: #fafafa; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  font-size: 48px; 
  position: relative;
}
.tag {
  position: absolute; top: 0; left: 0; background: var(--jd-red); color: #fff; font-size: 12px; padding: 2px 8px; border-bottom-right-radius: 8px;
}
.info { padding: 12px; }
.name { font-size: 14px; line-height: 20px; height: 40px; overflow: hidden; color: #333; margin-bottom: 8px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;}
.price-row { color: var(--jd-red); margin-bottom: 6px; }
.currency { font-size: 14px; }
.price { font-size: 20px; font-weight: bold; }
.meta { font-size: 12px; color: #999; margin-bottom: 8px; }
.time-box { background: #fdf0f0; color: var(--jd-red); font-size: 12px; text-align: center; padding: 4px; border-radius: 4px; }

.right-detail-wrapper {
  height: 100%;
  overflow: hidden; 
  flex-shrink: 0;
}

.right-detail {
  width: 400px;
  margin-left: 20px;
  height: 100%; 
  overflow-y: auto; 
  -ms-overflow-style: none; scrollbar-width: none;
}
.right-detail::-webkit-scrollbar { display: none; }

.ios-slide-enter-active,
.ios-slide-leave-active { transition: all 0.5s cubic-bezier(0.32, 0.72, 0, 1); }
.ios-slide-enter-from,
.ios-slide-leave-to { max-width: 0; opacity: 0; transform: translateX(40px); }
.ios-slide-enter-to,
.ios-slide-leave-from { max-width: 420px; opacity: 1; transform: translateX(0); }

.detail-panel { padding: 24px; position: relative; background: #fff; border-radius: var(--radius-sm); }
.close-btn {
  position: absolute; top: 12px; right: 12px; background: none; border: none; font-size: 20px; color: #999; cursor: pointer;
  width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 50%; transition: all 0.2s;
}
.close-btn:hover { color: var(--jd-red); background: #fdf0f0; transform: rotate(90deg); }

.detail-img-box { height: 200px; background: #f4f4f4; border-radius: 8px; display: flex; justify-content: center; align-items: center; font-size: 64px; margin-bottom: 20px; }
.detail-price-box { color: var(--jd-red); margin-bottom: 12px; }
.detail-price-box .price-huge { font-size: 36px; font-weight: bold; }
.detail-title { font-size: 18px; color: #333; margin: 0 0 20px 0; line-height: 1.4; font-weight: 600; }
.status-box { background: #f9f9f9; padding: 16px; border-radius: 8px; margin-bottom: 24px; }
.status-item { margin-bottom: 8px; font-size: 13px; display: flex; }
.status-item:last-child { margin-bottom: 0; }
.status-item .label { color: #999; width: 70px; }
.success-banner { background: #fdf0f0; border: 1px solid #fecaca; color: var(--jd-red); padding: 12px; border-radius: 4px; margin-bottom: 20px; text-align: center; font-weight: bold; }
.order-status { font-size: 12px; font-weight: normal; }
.buy-btn { width: 100%; height: 48px; font-size: 18px; border-radius: 4px; }
.buy-btn.processing { opacity: 0.8; }
.result-msg { text-align: center; margin-top: 12px; font-size: 14px; }

.text-jd-red { color: var(--jd-red); }
.text-success { color: #059669; }
.text-danger { color: var(--jd-red); }
.text-muted { color: #999; }
.text-main { color: #333; }
.font-bold { font-weight: bold; }

.floor-title { margin-bottom: 20px; flex-shrink: 0; }
.floor-title h2 { margin: 0; font-size: 24px; color: #333; }
.floor-title span { font-size: 14px; color: #999; font-weight: normal; margin-left: 10px; }
.tip { padding: 40px; text-align: center; color: #999; }
.error-text { color: var(--jd-red); }
</style>