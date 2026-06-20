<template>
  <h2 class="page-title">检查规则</h2>
  <el-card>
    <div style="display:flex;gap:10px;margin-bottom:12px">
      <el-input v-model="query.keyword" placeholder="名称/key/SQL" style="width:260px" clearable @keyup.enter="load" />
      <el-select v-model="query.category" placeholder="类别" clearable style="width:160px">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <div class="spacer" style="flex:1"></div>
      <el-button type="primary" @click="$router.push('/rules/edit')">新建规则</el-button>
    </div>
    <el-table :data="rows" v-loading="loading">
      <el-table-column label="规则">
        <template #default="{ row }"><div>{{ row.name }}</div><small class="muted">{{ row.ruleKey }}</small></template>
      </el-table-column>
      <el-table-column prop="category" label="类别" width="130" />
      <el-table-column prop="type" label="类型" width="200" />
      <el-table-column label="级别" width="110">
        <template #default="{ row }"><el-tag size="small" :type="sev(row.severity)">{{ row.severity }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="currentVersion" label="版本" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag size="small" :type="row.enabled?'success':'info'">{{ row.enabled?'启用':'停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="170">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push({ path: '/rules/edit', query: { key: row.ruleKey } })">编辑</el-button>
          <el-button link type="primary" @click="showVersions(row)">历史</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
      :current-page="query.page" :page-size="query.size" @current-change="(p:number)=>{query.page=p;load()}" />
  </el-card>

  <el-drawer v-model="drawer" title="历史版本" size="40%">
    <el-timeline>
      <el-timeline-item v-for="v in versions" :key="v.id" :timestamp="`v${v.version} · ${v.author||''}`">
        <div>{{ v.changeSummary || '(无说明)' }}</div>
        <el-button v-if="v.version!==current.currentVersion" link type="primary" @click="rollback(v.version)">回滚到此版本</el-button>
      </el-timeline-item>
    </el-timeline>
  </el-drawer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ruleApi } from '@/api'
import { SEVERITY_TYPE } from '@/types'

const rows = ref<any[]>([]); const total = ref(0); const loading = ref(false)
const categories = ['balance','reconciliation','continuity','cost','integrity','cross_system','regression']
const query = reactive<any>({ keyword: '', category: '', page: 1, size: 20 })
const sev = (s: string) => SEVERITY_TYPE[s] || 'info'
const drawer = ref(false); const versions = ref<any[]>([]); const current = ref<any>({})

async function load() {
  loading.value = true
  try { const p = await ruleApi.page(query); rows.value = p.records||[]; total.value = p.total||0 } catch {} finally { loading.value = false }
}
async function showVersions(row: any) {
  current.value = row; drawer.value = true
  versions.value = await ruleApi.versions(row.ruleKey)
}
async function rollback(v: number) {
  await ruleApi.rollback(current.value.ruleKey, v)
  ElMessage.success('已回滚（生成新版本）')
  drawer.value = false; load()
}
onMounted(load)
</script>
