<template>
  <h2 class="page-title">告警中心</h2>
  <el-card>
    <div style="display:flex;gap:10px;margin-bottom:12px">
      <el-select v-model="query.status" placeholder="状态" clearable style="width:150px" @change="load">
        <el-option label="待处理" value="PENDING" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="已闭环" value="CLOSED" />
      </el-select>
    </div>
    <el-table :data="rows" v-loading="loading">
      <el-table-column label="级别" width="100">
        <template #default="{ row }"><el-tag size="small" :type="sev(row.severity)">{{ row.severity }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="title" label="告警内容" />
      <el-table-column prop="ruleKey" label="规则" width="220" />
      <el-table-column label="企业微信" width="100">
        <template #default="{ row }"><el-tag size="small" :type="row.wecomPushStatus==='SENT'?'success':'info'">{{ row.wecomPushStatus||'-' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" @click="act(row,'claim')" v-if="row.status==='PENDING'">认领</el-button>
          <el-button link type="success" @click="act(row,'close')" v-if="row.status!=='CLOSED'">闭环</el-button>
          <el-button link type="warning" @click="act(row,'fp')" v-if="row.status!=='CLOSED'">误报</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
      :current-page="query.page" :page-size="query.size" @current-change="(p:number)=>{query.page=p;load()}" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { alertApi } from '@/api'
import { SEVERITY_TYPE } from '@/types'

const rows = ref<any[]>([]); const total = ref(0); const loading = ref(false)
const query = reactive<any>({ status: 'PENDING', page: 1, size: 20 })
const sev = (s: string) => SEVERITY_TYPE[s] || 'info'

async function load() {
  loading.value = true
  try { const p = await alertApi.page(query); rows.value = p.records||[]; total.value = p.total||0 } catch {} finally { loading.value = false }
}
async function act(row: any, a: string) {
  if (a === 'claim') await alertApi.claim(row.id)
  else if (a === 'close') await alertApi.close(row.id)
  else if (a === 'fp') await alertApi.falsePositive(row.id)
  ElMessage.success('操作成功'); load()
}
onMounted(load)
</script>
