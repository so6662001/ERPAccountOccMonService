<template>
  <h2 class="page-title">检测运行详情</h2>
  <el-card v-if="run">
    <el-descriptions :column="4" border>
      <el-descriptions-item label="运行ID">{{ run.id }}</el-descriptions-item>
      <el-descriptions-item label="检查项">{{ run.total }}</el-descriptions-item>
      <el-descriptions-item label="失败">{{ run.failed }}</el-descriptions-item>
      <el-descriptions-item label="门禁">
        <el-tag :type="run.gatePassed?'success':'danger'">{{ run.gatePassed?'通过':'阻断' }}</el-tag>
      </el-descriptions-item>
    </el-descriptions>
  </el-card>

  <el-card style="margin-top:16px">
    <template #header>
      <el-radio-group v-model="statusFilter" @change="load">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="FAILED">失败</el-radio-button>
        <el-radio-button label="PASSED">通过</el-radio-button>
      </el-radio-group>
    </template>
    <div v-for="item in results" :key="item.result.id" style="margin-bottom:10px">
      <el-tag size="small" :type="stat(item.result.status)">{{ item.result.status }}</el-tag>
      <b style="margin:0 8px">{{ item.result.ruleName }}</b>
      <span class="muted">{{ item.result.message }}</span>
      <el-table v-if="item.samples?.length" :data="parseSamples(item.samples)" size="small" border style="margin-top:6px">
        <el-table-column v-for="k in sampleKeys(item.samples)" :key="k" :prop="k" :label="k" />
      </el-table>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { resultApi } from '@/api'
import { STATUS_TYPE } from '@/types'

const route = useRoute()
const run = ref<any>(null)
const results = ref<any[]>([])
const statusFilter = ref('')
const stat = (s: string) => STATUS_TYPE[s] || 'info'

function parseSamples(samples: any[]) {
  return samples.map((s) => { try { return JSON.parse(s.sampleJson) } catch { return {} } })
}
function sampleKeys(samples: any[]) {
  const first = parseSamples(samples)[0] || {}
  return Object.keys(first)
}
async function load() {
  const d = await resultApi.detail(Number(route.params.id), { status: statusFilter.value || undefined })
  run.value = d.run; results.value = d.results || []
}
onMounted(load)
</script>
