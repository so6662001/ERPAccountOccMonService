<template>
  <h2 class="page-title">企业微信通知</h2>
  <el-card>
    <template #header><b>告警渠道</b>
      <el-button type="primary" size="small" style="float:right" @click="openNew">新增渠道</el-button>
    </template>
    <el-table :data="channels" v-loading="loading">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column prop="msgType" label="消息类型" width="130" />
      <el-table-column prop="gateSeverity" label="门禁级别" width="120" />
      <el-table-column prop="scopeType" label="分流" width="130" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }"><el-button link type="primary" @click="test(row)">发送测试</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialog" title="新增/编辑渠道" width="560">
    <el-form label-width="110px">
      <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.type"><el-option label="群机器人 Webhook" value="WEBHOOK" /><el-option label="自建应用" value="APP" /></el-select>
      </el-form-item>
      <el-form-item label="配置">
        <el-input v-model="form.config" type="textarea" :rows="3"
          placeholder="WEBHOOK: webhook 地址；APP: {corpId,corpSecret,agentId,toUser} JSON" />
      </el-form-item>
      <el-form-item label="消息类型">
        <el-select v-model="form.msgType"><el-option label="Markdown" value="MARKDOWN" /><el-option label="文本" value="TEXT" /></el-select>
      </el-form-item>
      <el-form-item label="门禁级别">
        <el-select v-model="form.gateSeverity"><el-option v-for="s in sevs" :key="s" :label="s" :value="s" /></el-select>
      </el-form-item>
      <el-form-item label="@手机号"><el-input v-model="form.mentionMobiles" placeholder="逗号分隔" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { alertApi } from '@/api'

const channels = ref<any[]>([]); const loading = ref(false)
const dialog = ref(false)
const sevs = ['INFO','LOW','MEDIUM','HIGH','CRITICAL']
const form = reactive<any>({ type: 'WEBHOOK', msgType: 'MARKDOWN', gateSeverity: 'HIGH', scopeType: 'DEFAULT' })

async function load() { loading.value = true; try { channels.value = await alertApi.channels() } catch {} finally { loading.value = false } }
function openNew() { Object.assign(form, { id: undefined, name: '', config: '', type: 'WEBHOOK', msgType: 'MARKDOWN', gateSeverity: 'HIGH', scopeType: 'DEFAULT', mentionMobiles: '' }); dialog.value = true }
async function save() { await alertApi.saveChannel(form); ElMessage.success('已保存'); dialog.value = false; load() }
async function test(row: any) { const r: any = await alertApi.testChannel(row.id); ElMessage[r.ok?'success':'warning'](r.message || '已发送') }
onMounted(load)
</script>
