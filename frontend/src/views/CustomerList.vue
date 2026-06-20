<template>
  <h2 class="page-title">客户与租户</h2>
  <el-card>
    <div style="display:flex;gap:10px;margin-bottom:12px">
      <el-input v-model="query.keyword" placeholder="搜索客户名/编码" style="width:240px" clearable @keyup.enter="load" />
      <el-select v-model="query.isolationMode" placeholder="隔离模式" clearable style="width:160px">
        <el-option label="独立数据库" value="DB_PER_CUSTOMER" />
        <el-option label="租户隔离" value="TENANT_SHARED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="name" label="客户" />
      <el-table-column prop="industry" label="行业" width="100" />
      <el-table-column label="隔离模式" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="row.isolationMode==='TENANT_SHARED'?'warning':'primary'">
            {{ row.isolationMode==='TENANT_SHARED'?'租户隔离':'独立库' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentPeriod" label="会计期间" width="110" />
      <el-table-column prop="slaLevel" label="SLA" width="90" />
      <el-table-column prop="deployVersion" label="部署版本" width="110" />
      <el-table-column prop="status" label="状态" width="90" />
    </el-table>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
      :current-page="query.page" :page-size="query.size" @current-change="(p:number)=>{query.page=p;load()}" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { customerApi } from '@/api'

const rows = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const query = reactive<any>({ keyword: '', isolationMode: '', page: 1, size: 20 })

async function load() {
  loading.value = true
  try {
    const p = await customerApi.page(query)
    rows.value = p.records || []
    total.value = p.total || 0
  } catch {} finally { loading.value = false }
}
onMounted(load)
</script>
