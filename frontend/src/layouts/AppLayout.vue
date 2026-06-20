<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <div class="brand">
        <div class="logo">普</div>
        <div><b>普讯数据监测平台</b><small>Puxun Data Monitoring</small></div>
      </div>
      <el-menu :default-active="route.path" router class="flex-1" style="flex:1">
        <el-menu-item index="/dashboard"><el-icon><DataBoard /></el-icon><span>监控总览</span></el-menu-item>
        <el-menu-item index="/customers"><el-icon><OfficeBuilding /></el-icon><span>客户与租户</span></el-menu-item>
        <el-menu-item index="/rules"><el-icon><Finished /></el-icon><span>检查规则</span></el-menu-item>
        <el-menu-item index="/templates"><el-icon><Files /></el-icon><span>规则模板库</span></el-menu-item>
        <el-menu-item index="/datasources"><el-icon><Coin /></el-icon><span>数据源</span></el-menu-item>
        <el-menu-item index="/tasks"><el-icon><Timer /></el-icon><span>检测任务</span></el-menu-item>
        <el-menu-item index="/runs"><el-icon><Document /></el-icon><span>检测结果</span></el-menu-item>
        <el-menu-item index="/analytics"><el-icon><TrendCharts /></el-icon><span>趋势与统计</span></el-menu-item>
        <el-menu-item index="/rollouts"><el-icon><Promotion /></el-icon><span>灰度发布</span></el-menu-item>
        <el-menu-item index="/alerts"><el-icon><Bell /></el-icon><span>告警中心</span></el-menu-item>
        <el-menu-item index="/wecom"><el-icon><ChatDotRound /></el-icon><span>企业微信通知</span></el-menu-item>
      </el-menu>
    </aside>
    <div class="app-main">
      <header class="app-topbar">
        <span class="muted">{{ route.name }}</span>
        <div class="spacer"></div>
        <el-tag type="primary" effect="plain">生产环境 · 华东集群</el-tag>
        <el-dropdown @command="onCommand">
          <span style="cursor:pointer">
            <el-avatar :size="30" style="background:#6366f1">{{ (auth.user?.displayName || 'U').slice(0,1) }}</el-avatar>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ auth.user?.username }}（{{ (auth.roles || []).join(',') }}）</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>
      <main class="app-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataBoard, OfficeBuilding, Finished, Files, Coin, Timer,
  Document, TrendCharts, Promotion, Bell, ChatDotRound,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

onMounted(() => {
  if (!auth.user) auth.loadMe().catch(() => {})
})

function onCommand(cmd: string) {
  if (cmd === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>
