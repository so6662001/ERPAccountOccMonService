<template>
  <h2 class="page-title">趋势与统计</h2>
  <el-row :gutter="16">
    <el-col :span="6"><el-card><div class="muted">告警 MTTR(分钟)</div><div class="kpi-value">{{ mttr }}</div></el-card></el-col>
    <el-col :span="18">
      <el-card>
        <template #header><b>通过率趋势（近 14 天）</b></template>
        <svg viewBox="0 0 720 160" style="width:100%;height:160px">
          <polyline :points="trendPoints" fill="none" stroke="#2563eb" stroke-width="2.5" />
        </svg>
      </el-card>
    </el-col>
  </el-row>

  <el-card style="margin-top:16px">
    <template #header><b>Top 高频失败规则</b></template>
    <el-table :data="topRules" size="small">
      <el-table-column prop="ruleName" label="规则" />
      <el-table-column prop="ruleKey" label="Key" width="260" />
      <el-table-column prop="failures" label="失败次数" width="120" />
      <el-table-column prop="customers" label="影响客户" width="120" />
    </el-table>
  </el-card>

  <el-card style="margin-top:16px">
    <template #header><b>各行业健康度</b></template>
    <el-table :data="industry" size="small">
      <el-table-column prop="industry" label="行业" />
      <el-table-column label="通过率"><template #default="{ row }">
        <el-progress :percentage="Number(row.passRate||0)" />
      </template></el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { analyticsApi } from '@/api'

const mttr = ref(0)
const trend = ref<any[]>([])
const topRules = ref<any[]>([])
const industry = ref<any[]>([])

const trendPoints = computed(() => {
  if (!trend.value.length) return ''
  const w = 720, h = 160
  return trend.value.map((d, i) => {
    const x = (i / Math.max(1, trend.value.length - 1)) * w
    const pr = Number(d.passRate || 0)
    const y = h - ((pr - 95) / 5) * h // 95%~100% 映射
    return `${x.toFixed(0)},${Math.max(0, Math.min(h, y)).toFixed(0)}`
  }).join(' ')
})

onMounted(async () => {
  try {
    mttr.value = await analyticsApi.mttr(7)
    trend.value = await analyticsApi.passRateTrend(14)
    topRules.value = await analyticsApi.topFailingRules(14, 10)
    industry.value = await analyticsApi.industryHealth(7)
  } catch {}
})
</script>
