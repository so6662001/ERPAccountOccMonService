<template>
  <h2 class="page-title">检测结果</h2>
  <el-card>
    <el-table :data="rows" v-loading="loading" @row-click="(r:any)=>$router.push(`/runs/${r.id}`)" style="cursor:pointer">
      <el-table-column prop="id" label="运行ID" width="160" />
      <el-table-column prop="label" label="标签" />
      <el-table-column prop="triggerType" label="触发" width="110" />
      <el-table-column label="结果" width="220">
        <template #default="{ row }">
          通过 <b style="color:#18a058">{{ row.passed }}</b> / 失败 <b style="color:#d92d20">{{ row.failed }}</b> / 错误 {{ row.errored }}
        </template>
      </el-table-column>
      <el-table-column label="门禁" width="100">
        <template #default="{ row }"><el-tag size="small" :type="row.gatePassed?'success':'danger'">{{ row.gatePassed?'通过':'阻断' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="maxSeverity" label="最高级别" width="110" />
    </el-table>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
      :current-page="query.page" :page-size="query.size" @current-change="(p:number)=>{query.page=p;load()}" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { resultApi } from '@/api'

const rows = ref<any[]>([]); const total = ref(0); const loading = ref(false)
const query = reactive<any>({ page: 1, size: 20 })
async function load() {
  loading.value = true
  try { const p = await resultApi.pageRuns(query); rows.value = p.records||[]; total.value = p.total||0 } catch {} finally { loading.value = false }
}
onMounted(load)
</script>
