<template>
  <h2 class="page-title">检测任务</h2>
  <el-card>
    <div style="display:flex;margin-bottom:12px">
      <div class="spacer"></div>
      <el-button type="primary" @click="openNew">新建任务</el-button>
    </div>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="name" label="任务" />
      <el-table-column prop="triggerType" label="触发" width="120" />
      <el-table-column prop="cron" label="周期(Cron)" width="180" />
      <el-table-column prop="gateSeverity" label="门禁" width="110" />
      <el-table-column prop="concurrency" label="并发" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag size="small" :type="row.enabled?'success':'info'">{{ row.enabled?'启用':'停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="run(row)">立即执行</el-button>
          <el-button link type="primary" @click="edit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top:12px" layout="total, prev, pager, next" :total="total"
      :current-page="query.page" :page-size="query.size" @current-change="(p:number)=>{query.page=p;load()}" />
  </el-card>

  <el-dialog v-model="dialog" title="新建/编辑任务" width="600">
    <el-form label-width="110px">
      <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="触发方式">
        <el-select v-model="form.triggerType">
          <el-option label="定时 Cron" value="CRON" />
          <el-option label="结账事件" value="EVENT" />
          <el-option label="CI 门禁" value="CI_GATE" />
          <el-option label="手动" value="MANUAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="Cron"><el-input v-model="form.cron" placeholder="0 */10 * * * ?" /></el-form-item>
      <el-form-item label="门禁级别">
        <el-select v-model="form.gateSeverity"><el-option v-for="s in sevs" :key="s" :label="s" :value="s" /></el-select>
      </el-form-item>
      <el-form-item label="并发"><el-input-number v-model="form.concurrency" :min="1" :max="32" /></el-form-item>
      <el-form-item label="范围(JSON)">
        <el-input v-model="form.scopeJson" type="textarea" :rows="2" placeholder='{"customerIds":[1,2]} 或 {"industry":"商贸"}，空=全部ACTIVE' />
      </el-form-item>
      <el-form-item label="启用"><el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { taskApi } from '@/api'

const rows = ref<any[]>([]); const total = ref(0); const loading = ref(false)
const query = reactive<any>({ page: 1, size: 20 })
const dialog = ref(false)
const sevs = ['INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const form = reactive<any>({ triggerType: 'CRON', gateSeverity: 'HIGH', concurrency: 4, enabled: 1 })

async function load() {
  loading.value = true
  try { const p = await taskApi.page(query); rows.value = p.records || []; total.value = p.total || 0 } catch {} finally { loading.value = false }
}
function openNew() { Object.assign(form, { id: undefined, name: '', triggerType: 'CRON', cron: '0 */10 * * * ?', gateSeverity: 'HIGH', concurrency: 4, scopeJson: '', enabled: 1 }); dialog.value = true }
function edit(row: any) { Object.assign(form, row); dialog.value = true }
async function save() { await taskApi.save(form); ElMessage.success('已保存'); dialog.value = false; load() }
async function run(row: any) { await taskApi.run(row.id); ElMessage.success('已触发执行'); load() }
onMounted(load)
</script>
