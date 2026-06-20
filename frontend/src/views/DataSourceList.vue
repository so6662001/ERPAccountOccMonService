<template>
  <h2 class="page-title">数据源</h2>
  <el-card>
    <div style="display:flex;gap:10px;margin-bottom:12px">
      <el-input v-model.number="customerId" placeholder="客户ID(可空=全部)" style="width:200px" clearable @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <div class="spacer"></div>
      <el-button @click="openNew">新增数据源</el-button>
    </div>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="customerId" label="客户ID" width="110" />
      <el-table-column prop="dbType" label="类型" width="120" />
      <el-table-column prop="jdbcUrl" label="连接" show-overflow-tooltip />
      <el-table-column label="读写模式" width="120">
        <template #default="{ row }"><el-tag size="small" :type="row.mode==='READ_REPLICA'?'success':'warning'">{{ row.mode==='READ_REPLICA'?'只读副本':'主库' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag size="small" :type="dsType(row.status)">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="latencyMs" label="响应(ms)" width="100" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="probe(row)">探活</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialog" title="新增/编辑数据源" width="600">
    <el-form label-width="100px">
      <el-form-item label="客户ID"><el-input v-model.number="form.customerId" /></el-form-item>
      <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.dbType">
          <el-option v-for="t in dbTypes" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>
      <el-form-item label="连接串"><el-input v-model="form.jdbcUrl" /></el-form-item>
      <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="口令"><el-input v-model="form.password" type="password" show-password placeholder="留空表示不修改" /></el-form-item>
      <el-form-item label="读写模式">
        <el-select v-model="form.mode"><el-option label="只读副本" value="READ_REPLICA" /><el-option label="主库" value="PRIMARY" /></el-select>
      </el-form-item>
      <el-form-item label="租户字段"><el-input v-model="form.tenantColumn" placeholder="tenant_id" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="testConn">测试连接</el-button>
      <el-button @click="dialog=false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { datasourceApi } from '@/api'

const rows = ref<any[]>([]); const loading = ref(false)
const customerId = ref<number | undefined>(undefined)
const dialog = ref(false)
const dbTypes = ['MYSQL', 'ORACLE', 'SQLSERVER', 'DM', 'POSTGRES']
const form = reactive<any>({ dbType: 'MYSQL', mode: 'READ_REPLICA', tenantColumn: 'tenant_id' })
const dsType = (s: string) => ({ ONLINE: 'success', DELAY: 'warning', FAILED: 'danger' } as any)[s] || 'info'

async function load() {
  loading.value = true
  try { rows.value = await datasourceApi.list(customerId.value) } catch {} finally { loading.value = false }
}
function openNew() { Object.assign(form, { id: undefined, customerId: undefined, name: '', dbType: 'MYSQL', jdbcUrl: '', username: '', password: '', mode: 'READ_REPLICA', tenantColumn: 'tenant_id' }); dialog.value = true }
async function save() { await datasourceApi.save(form); ElMessage.success('已保存'); dialog.value = false; load() }
async function testConn() {
  const r: any = await datasourceApi.test({ dbType: form.dbType, jdbcUrl: form.jdbcUrl, username: form.username, password: form.password })
  ElMessage[r.ok ? 'success' : 'warning'](r.message || (r.ok ? '连接成功' : '连接失败'))
}
async function probe(row: any) { const r: any = await datasourceApi.probe(row.id); ElMessage[r.ok ? 'success' : 'warning'](r.message); load() }
onMounted(load)
</script>
