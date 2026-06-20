<template>
  <h2 class="page-title">模板差异对比与同步</h2>
  <el-card>
    <div style="display:flex;gap:10px;margin-bottom:12px">
      <el-input v-model.number="customerId" placeholder="客户ID" style="width:160px" />
      <el-input v-model.number="templateId" placeholder="模板/规则集ID" style="width:180px" />
      <el-button type="primary" @click="loadDiff">对比</el-button>
      <div class="spacer"></div>
      <el-checkbox v-model="includeAdded">同步新增项</el-checkbox>
      <el-button type="success" :disabled="!items.length" @click="apply">应用同步</el-button>
    </div>

    <el-alert v-if="conflicts" type="warning" show-icon :closable="false" style="margin-bottom:12px"
      :title="`存在 ${conflicts} 个冲突项（本地已微调），必须逐条决定后才能同步`" />

    <el-table :data="items" v-loading="loading">
      <el-table-column label="变更" width="120">
        <template #default="{ row }"><el-tag size="small" :type="kindType(row.kind)">{{ kindLabel(row.kind) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="ruleKey" label="规则 Key" />
      <el-table-column prop="summary" label="说明" />
      <el-table-column label="处理" width="220">
        <template #default="{ row }">
          <el-radio-group v-if="row.conflict" v-model="decisions[row.ruleKey]" size="small">
            <el-radio-button label="KEEP_LOCAL">保留本地</el-radio-button>
            <el-radio-button label="ADOPT_TEMPLATE">采用模板</el-radio-button>
          </el-radio-group>
          <span v-else class="muted">{{ autoLabel(row.kind) }}</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { templateSyncApi } from '@/api'

const route = useRoute()
const customerId = ref<number | undefined>(route.query.customerId ? Number(route.query.customerId) : undefined)
const templateId = ref<number | undefined>(route.query.templateId ? Number(route.query.templateId) : undefined)
const items = ref<any[]>([])
const loading = ref(false)
const includeAdded = ref(true)
const decisions = reactive<Record<string, string>>({})

const conflicts = computed(() => items.value.filter((i) => i.conflict).length)

const kindType = (k: string) => ({ ADDED: 'success', MODIFIED: 'warning', DEPRECATED: 'danger', LOCAL_CUSTOM: 'info' } as any)[k] || 'info'
const kindLabel = (k: string) => ({ ADDED: '新增', MODIFIED: '修改', DEPRECATED: '弃用', LOCAL_CUSTOM: '本地自定义' } as any)[k] || k
const autoLabel = (k: string) => ({ ADDED: '随模板加入', MODIFIED: '默认不动', LOCAL_CUSTOM: '保留', DEPRECATED: '默认保留' } as any)[k] || '-'

async function loadDiff() {
  if (!customerId.value || !templateId.value) { ElMessage.warning('请填写客户ID与模板ID'); return }
  loading.value = true
  try {
    const d: any = await templateSyncApi.diff(customerId.value, templateId.value)
    items.value = d.items || []
  } catch {} finally { loading.value = false }
}

async function apply() {
  const unresolved = items.value.filter((i) => i.conflict && !decisions[i.ruleKey])
  if (unresolved.length) { ElMessage.warning('请先决定全部冲突项'); return }
  await templateSyncApi.apply({ customerId: customerId.value, templateId: templateId.value, decisions, includeAdded: includeAdded.value })
  ElMessage.success('同步完成')
  loadDiff()
}
</script>
