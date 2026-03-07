<template>
  <div class="page">
    <h1>后台登录</h1>
    <form @submit.prevent="submit" class="form">
      <div>
        <label>用户名</label>
        <input v-model="username" type="text" required placeholder="admin" />
      </div>
      <div>
        <label>密码</label>
        <input v-model="password" type="password" required placeholder="123456" />
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="submit" :disabled="loading">登录</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '../api/admin.js'

const router = useRouter()
const route = useRoute()
const username = ref('admin')
const password = ref('123456')
const loading = ref(false)
const error = ref('')

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const res = await login(username.value, password.value)
    if (res.code === 0 && res.data?.token) {
      localStorage.setItem('adminToken', res.data.token)
      const redirect = route.query.redirect || '/admin/goods'
      router.push(redirect)
    } else {
      error.value = res.msg || '登录失败'
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { padding: 40px; max-width: 400px; margin: 60px auto; }
.form > div { margin-bottom: 16px; }
.form label { display: block; margin-bottom: 4px; }
.form input { width: 100%; padding: 8px; box-sizing: border-box; }
.form button { padding: 10px 24px; cursor: pointer; margin-top: 8px; }
.form .error { color: #e4393c; margin-top: 8px; }
</style>
