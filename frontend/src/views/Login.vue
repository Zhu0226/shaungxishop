<template>
  <div class="login-page">
    <header class="login-header container">
      <div class="logo-area" @click="$router.push('/')">
        <img src="/logo.png" alt="双喜" class="logo-img" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';" />
        <div class="logo-fallback" style="display: none;">
          <span class="icon">⚡</span> 双喜
        </div>
      </div>
      <div class="header-feedback">
        <a href="#">💬 登录页面，改进建议</a>
      </div>
    </header>

    <div class="main-tabs container">
      <div class="tab-item" :class="{ active: role === 'user' }" @click="role = 'user'">
        个人/企业买家登录
      </div>
      <div class="tab-item" :class="{ active: role === 'admin' }" @click="role = 'admin'">
        内部员工/后台登录
      </div>
    </div>

    <div class="login-body">
      <div class="login-card">
        <div class="card-left">
          <h3>系统已升级为 RBAC 权限体系</h3>
          <div class="qr-tips" style="margin-top: 15px; line-height: 1.8; color: #666; font-size: 13px; text-align: left;">
            <p><strong>测试账号体验指南：</strong></p>
            <p>👑 <b>超级管理员</b>: admin / 123456</p>
            <p>🎧 <b>客服人员</b>: kefu01 / 123456</p>
            <p>🏢 <b>企业老板</b>: boss / 123456</p>
            <p>🛍️ <b>个人买家</b>: buyer / 123456</p>
          </div>
        </div>

        <div class="card-divider"></div>

        <div class="card-right">
          <div class="form-tabs">
            <span class="active">密码登录</span>
            <span class="inactive" @click="handleRegisterClick">新用户注册</span>
          </div>

          <form class="login-form" @submit.prevent="handleLogin">
            <div class="input-group">
              <input 
                v-model="username" 
                type="text" 
                placeholder="请输入预设的账号名" 
                required 
              />
            </div>
            
            <div class="input-group">
              <input 
                v-model="password" 
                type="password" 
                placeholder="密码统一为 123456" 
                required 
              />
            </div>

            <div v-if="error" class="error-msg">{{ error }}</div>

            <button type="submit" class="submit-btn btn-active" :disabled="loading">
              {{ loading ? '权限校验中...' : '登 录' }}
            </button>
          </form>

          <div class="form-footer">
            <div class="third-party">
              <a href="#"><span class="icon-wx"></span> 微信登录</a>
            </div>
            <div class="links">
              <a href="#">忘记密码</a>
            </div>
          </div>
        </div>
      </div>
    </div>

    <footer class="login-footer">
      <div class="copyright">
        Copyright © 2004-2026 双喜 版权所有
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
// 确保这个文件已经按照之前的步骤建好了！
import { login } from '../api/auth.js' 

const router = useRouter()
const route = useRoute()

const role = ref('user')
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

onMounted(() => {
  if (route.query.role === 'admin') {
    role.value = 'admin'
    username.value = 'admin'
  }
})

function handleRegisterClick() {
  alert('系统已开启企业级 RBAC 权限控制，暂不支持外部自行注册。请使用左侧提供的预设账号体验不同角色的权限！')
}

async function handleLogin() {
  error.value = ''
  loading.value = true

  try {
    // 所有人统一调用后端唯一的 AuthController 接口
    const res = await login(username.value, password.value)
    
    if (res.code === 0 && res.data?.token) {
      // 1. 统一存储数据
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userId', res.data.userId)
      localStorage.setItem('username', res.data.realName || res.data.username)
      localStorage.setItem('perms', JSON.stringify(res.data.perms || []))

      // 2. 智能路由跳转：如果有后台查看权限，且选了后台登录，跳后台；否则全跳前台商城
      const perms = res.data.perms || []
      if (perms.includes('admin:view') && role.value === 'admin') {
        router.push(route.query.redirect || '/admin/goods')
      } else {
        router.push(route.query.redirect || '/')
      }
    } else {
      throw new Error(res.msg || '登录失败')
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #f9f9f9; 
  display: flex;
  flex-direction: column;
}

.login-header {
  height: 80px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
}
.logo-area {
  height: 60px;
  cursor: pointer;
  display: flex;
  align-items: center;
}
.logo-img { max-height: 100%; object-fit: contain; }
.logo-fallback { font-size: 36px; font-weight: 900; color: #e1251b; letter-spacing: 2px; }

.header-feedback a {
  color: #999;
  font-size: 12px;
  text-decoration: none;
}
.header-feedback a:hover { color: #e1251b; }

.main-tabs {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-top: 20px;
  margin-bottom: 30px;
}
.tab-item {
  font-size: 20px;
  color: #333;
  cursor: pointer;
  position: relative;
  padding-bottom: 8px;
  font-weight: 500;
  transition: all 0.2s;
}
.tab-item:hover { color: #e1251b; }
.tab-item.active {
  font-weight: bold;
  color: #333; 
}
.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0; left: 50%;
  transform: translateX(-50%);
  width: 100%;
  height: 3px;
  background: #e1251b;
  border-radius: 2px;
}

.login-body {
  flex: 1;
  display: flex;
  justify-content: center;
}
.login-card {
  background: #fff;
  width: 800px;
  height: 350px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  padding: 0 40px;
}

.card-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.card-left h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 12px 0;
  text-align: left;
  border-bottom: 2px solid #eee;
  padding-bottom: 10px;
}

.card-divider {
  width: 1px;
  height: 250px;
  background: #f0f0f0;
  margin: 0 40px;
}

.card-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 300px;
}
.form-tabs {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
  font-size: 16px;
  font-weight: bold;
}
.form-tabs .active { color: #e1251b; }
.form-tabs .inactive { color: #999; cursor: pointer; font-weight: normal;}
.form-tabs .inactive:hover { color: #e1251b; }

.login-form {
  width: 100%;
}
.input-group {
  margin-bottom: 16px;
  height: 44px;
}
.input-group input {
  width: 100%;
  height: 100%;
  background: #f3f3f3; 
  border: 1px solid transparent;
  padding: 0 16px;
  outline: none;
  font-size: 14px;
  border-radius: 4px;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.input-group input:focus {
  border-color: #ccc;
  background: #fff;
}

.error-msg {
  color: #e1251b; font-size: 12px; margin-bottom: 12px; display: flex; align-items: center;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 2px;
  border-radius: 4px;
  background: #f59b98; 
  color: #fff;
  border: none;
  transition: all 0.3s;
}
.submit-btn.btn-active {
  background: #e1251b; 
  cursor: pointer;
}
.submit-btn.btn-active:hover {
  background: #c81623;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  font-size: 12px;
}
.third-party { display: flex; gap: 12px; }
.third-party a { color: #666; display: flex; align-items: center; gap: 4px; }
.third-party a:hover { color: #e1251b; }
.icon-wx::before { content: '💬'; color: #00c250; }

.links a { color: #666; }
.links a:hover { color: #e1251b; }

.login-footer {
  text-align: center;
  padding: 40px 0;
  font-size: 12px;
  color: #999;
}
</style>