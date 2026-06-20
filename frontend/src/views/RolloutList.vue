<template>
  <h2 class="page-title">灰度发布</h2>
  <el-card>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="targetType" label="对象" width="120" />
      <el-table-column prop="targetRef" label="目标" />
      <el-table-column prop="dimension" label="维度" width="140" />
      <el-table-column prop="advanceMode" label="推进" width="100" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }"><el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button link type="primary" @click="op(row,'start')" v-if="row.status==='DRAFT'">启动</el-button>
          <el-button link type="primary" @click="op(row,'advance')" v-if="row.status==='RUNNING'">推进下一批</el-button>
          <el-button link type="danger" @click="op(row,'rollback')" v-if="row.status==='RUNNING'">回滚</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { rolloutApi } from '@/api'

const rows = ref<any[]>([]); const loading = ref(false)
const statusType = (s: string) => ({ DRAFT:'info', RUNNING:'primary', PAUSED:'warning', DONE:'success', ROLLED_BACK:'danger' } as any)[s] || 'info'

async function load() {
  loading.value = true
  try { rows.value = await rolloutApi.list() } catch {} finally { loading.value = false }
}
async function op(row: any, action: string) {
  if (action === 'start') await rolloutApi.start(row.id)
  else if (action === 'advance') await rolloutApi.advance(row.id)
  else if (action === 'rollback') await rolloutApi.rollback(row.id, '人工回滚')
  ElMessage.success('操作成功'); load()
}
onMounted(load)
</script>
