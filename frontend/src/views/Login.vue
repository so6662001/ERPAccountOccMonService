<template>
  <div class="login-wrap">
    <div class="login-left">
      <div style="display:flex;align-items:center;gap:12px">
        <div style="width:44px;height:44px;border-radius:12px;background:linear-gradient(135deg,#3b82f6,#22d3ee);display:grid;place-items:center;font-weight:800;font-size:18px">普</div>
        <div><b style="font-size:18px">普讯数据监测平台</b><div style="font-size:12px;color:#8aa0c4">Puxun Data Monitoring Platform</div></div>
      </div>
      <div class="title">守住<span style="color:#7dd3fc">账务正确性</span><br/>让每一次迭代都安心</div>
      <p style="color:#aab9d6;margin-top:14px;max-width:440px">声明式约束 + 迭代回归防护，发现问题即推送企业微信，把研发迭代引入的账务风险挡在生产之外。</p>
      <div style="margin-top:auto;color:#8aa0c4;font-size:12px">© 2026 普讯科技</div>
    </div>
    <div class="login-right">
      <div class="login-card">
        <h2>欢迎登录</h2>
        <p class="muted">请使用企业账号登录</p>
        <el-form @submit.prevent="onLogin">
          <el-form-item>
            <el-input v-model="username" placeholder="账号" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="password" type="password" placeholder="密码" size="large" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="onLogin">登录</el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function onLogin() {
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/dashboard')
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>
