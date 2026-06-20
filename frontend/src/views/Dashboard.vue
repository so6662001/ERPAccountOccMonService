<template>
  <h2 class="page-title">监控总览</h2>
  <el-row :gutter="16">
    <el-col :span="6"><el-card><div class="muted">接入客户 / 租户群</div><div class="kpi-value">{{ overview.customers ?? '-' }}</div></el-card></el-col>
    <el-col :span="6"><el-card><div class="muted">今日通过率</div><div class="kpi-value" style="color:#18a058">{{ overview.todayPassRate ?? '-' }}%</div></el-card></el-col>
    <el-col :span="6"><el-card><div class="muted">今日检测执行</div><div class="kpi-value">{{ overview.todayExecutions ?? '-' }}</div></el-card></el-col>
    <el-col :span="6"><el-card><div class="muted">待处理告警</div><div class="kpi-value" style="color:#d92d20">{{ overview.pendingAlerts ?? '-' }}</div></el-card></el-col>
  </el-row>

  <el-card style="margin-top:16px">
    <template #header><b>最新告警</b><router-link to="/alerts" style="float:right">查看全部 →</router-link></template>
    <el-table :data="alerts" v-loading="loading" size="small">
      <el-table-column label="级别" width="100">
        <template #default="{ row }"><el-tag :type="sev(row.severity)" size="small">{{ row.severity }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="title" label="告警内容" />
      <el-table-column prop="ruleKey" label="规则" width="240" />
      <el-table-column label="企业微信" width="100">
        <template #default="{ row }"><el-tag size="small" :type="row.wecomPushStatus==='SENT'?'success':'info'">{{ row.wecomPushStatus || '-' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { analyticsApi, alertApi } from '@/api'
import { SEVERITY_TYPE } from '@/types'

const overview = ref<any>({})
const alerts = ref<any[]>([])
const loading = ref(false)
const sev = (s: string) => SEVERITY_TYPE[s] || 'info'

onMounted(async () => {
  loading.value = true
  try {
    overview.value = await analyticsApi.overview()
    const page = await alertApi.page({ status: 'PENDING', page: 1, size: 8 })
    alerts.value = page.records || []
  } catch {} finally { loading.value = false }
})
</script>
