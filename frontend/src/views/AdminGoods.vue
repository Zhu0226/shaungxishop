<template>
  <div class="page">
    <header class="header">
      <h1>商品管理</h1>
      <div class="header-actions">
        <router-link to="/metrics" class="btn-link">秒杀指标</router-link>
        <button @click="logout">退出登录</button>
      </div>
    </header>
    <div class="toolbar">
      <button @click="showForm(null)">新增商品</button>
    </div>
    <div v-if="loading" class="tip">加载中...</div>
    <div v-else-if="list.length === 0" class="empty">
      <p>暂无商品，点击「新增商品」添加</p>
    </div>
    <table v-else class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>价格</th>
          <th>库存</th>
          <th>活动时间</th>
          <th>上架</th>
          <th>标签</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="g in list" :key="g.id">
          <td>{{ g.id }}</td>
          <td>{{ g.goodsName }}</td>
          <td>{{ g.price }}</td>
          <td>{{ g.stock }}</td>
          <td class="time-cell">{{ formatTime(g.startTime) }} ~ {{ formatTime(g.endTime) }}</td>
          <td>{{ g.onShelf === 1 ? '是' : '否' }}</td>
          <td>{{ g.activityTag || '-' }}</td>
          <td>
            <button @click="showForm(g)">编辑</button>
            <button @click="doPreheat(g)">预热</button>
            <button @click="doDelete(g.id)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="formVisible" class="modal">
      <div class="modal-content">
        <h3>{{ editing ? '编辑商品' : '新增商品' }}</h3>
        <form @submit.prevent="save">
          <div>
            <label>名称</label>
            <input v-model="form.goodsName" required />
          </div>
          <div>
            <label>价格</label>
            <input v-model.number="form.price" type="number" step="0.01" required />
          </div>
          <div>
            <label>库存</label>
            <input v-model.number="form.stock" type="number" required />
          </div>
          <div>
            <label>开始时间</label>
            <input v-model="form.startTime" type="datetime-local" required />
          </div>
          <div>
            <label>结束时间</label>
            <input v-model="form.endTime" type="datetime-local" required />
          </div>
          <div>
            <label>上架</label>
            <select v-model.number="form.onShelf">
              <option :value="1">是</option>
              <option :value="0">否</option>
            </select>
          </div>
          <div>
            <label>活动标签</label>
            <input v-model="form.activityTag" placeholder="限时/爆款" />
          </div>
          <button type="submit">保存</button>
          <button type="button" @click="formVisible = false">取消</button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
// 务必确保你的 src/api/admin.js 文件已经按照上一步的指导重新建立好了！
import { listGoods, createGoods, updateGoods, deleteGoods, preheat } from '../api/admin.js'

const router = useRouter()
const list = ref([])
const loading = ref(true)
const formVisible = ref(false)
const editing = ref(null)
const form = ref({
  goodsName: '',
  price: 0,
  stock: 0,
  startTime: '',
  endTime: '',
  onShelf: 1,
  activityTag: '',
})

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function showForm(g) {
  editing.value = g
  if (g) {
    form.value = {
      goodsName: g.goodsName,
      price: g.price,
      stock: g.stock,
      startTime: g.startTime ? g.startTime.slice(0, 16) : '',
      endTime: g.endTime ? g.endTime.slice(0, 16) : '',
      onShelf: g.onShelf ?? 1,
      activityTag: g.activityTag || '',
    }
  } else {
    form.value = {
      goodsName: '',
      price: 0,
      stock: g?.stock ?? 100,
      startTime: new Date().toISOString().slice(0, 16),
      endTime: new Date(Date.now() + 86400000).toISOString().slice(0, 16),
      onShelf: 1,
      activityTag: '',
    }
  }
  formVisible.value = true
}

async function load() {
  loading.value = true
  try {
    const res = await listGoods()
    if (res.code === 0) list.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function save() {
  const g = form.value
  const payload = {
    goodsName: g.goodsName,
    price: g.price,
    stock: g.stock,
    startTime: g.startTime + ':00',
    endTime: g.endTime + ':00',
    onShelf: g.onShelf,
    activityTag: g.activityTag,
  }
  try {
    if (editing.value) {
      await updateGoods(editing.value.id, payload)
    } else {
      await createGoods(payload)
    }
    formVisible.value = false
    load()
  } catch (e) {
    alert(e.message)
  }
}

async function doPreheat(g) {
  const raw = prompt(`预热库存数量（1 ~ ${g.stock}）`, String(g.stock))
  if (raw == null || raw === '') return
  const stock = Number(raw)
  if (!Number.isInteger(stock) || stock < 1) {
    alert('请输入大于 0 的整数')
    return
  }
  if (stock > g.stock) {
    alert(`预热库存不能超过数据库库存（${g.stock}）`)
    return
  }
  try {
    await preheat(g.id, stock)
    alert('预热成功')
    load()
  } catch (e) {
    alert(e.message)
  }
}

async function doDelete(id) {
  if (!confirm('确定删除？')) return
  try {
    await deleteGoods(id)
    load()
  } catch (e) {
    alert(e.message)
  }
}

function logout() {
  // 统一清理新版 RBAC 权限缓存
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('perms')
  
  // 统一跳回前台登录页
  router.push('/login')
}

onMounted(load)
</script>

<style scoped>
.page { padding: 20px; max-width: 1000px; margin: 0 auto; }
.header { display: flex; justify-content: space-between; margin-bottom: 20px; }
.toolbar { margin-bottom: 16px; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { border: 1px solid #ddd; padding: 8px; text-align: left; }
.table button { margin-right: 8px; padding: 4px 8px; cursor: pointer; }
.modal { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; }
.modal-content { background: #fff; padding: 24px; border-radius: 8px; min-width: 400px; }
.modal-content form > div { margin-bottom: 12px; }
.modal-content label { display: block; margin-bottom: 4px; }
.modal-content input, .modal-content select { width: 100%; padding: 6px; box-sizing: border-box; }
.modal-content button { margin-right: 8px; margin-top: 8px; padding: 8px 16px; cursor: pointer; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.btn-link { color: #1890ff; text-decoration: none; }
.empty { padding: 40px; text-align: center; color: #666; }
.time-cell { font-size: 12px; max-width: 180px; }
</style>