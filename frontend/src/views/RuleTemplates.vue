<template>
  <h2 class="page-title">规则模板库</h2>
  <el-row :gutter="16">
    <el-col :span="8" v-for="t in templates" :key="t.id" style="margin-bottom:16px">
      <el-card>
        <template #header>
          <b>{{ t.name }}</b>
          <el-tag size="small" style="float:right" :type="t.status==='DRAFT'?'warning':'success'">{{ t.status }}</el-tag>
        </template>
        <div class="muted">行业：{{ t.industry || '-' }} · 产品线：{{ t.productLine || '-' }}</div>
        <div class="muted">模板版本：v{{ t.version }}</div>
        <el-button style="margin-top:10px" type="primary" plain size="small" @click="viewItems(t)">查看明细</el-button>
      </el-card>
    </el-col>
  </el-row>

  <el-dialog v-model="dialog" :title="`${cur.name} · 规则明细`" width="640">
    <el-table :data="items" size="small">
      <el-table-column prop="ruleKey" label="规则 Key" />
      <el-table-column prop="effectiveVersionExpr" label="生效版本" width="160" />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ruleTemplateApi } from '@/api'

const templates = ref<any[]>([])
const items = ref<any[]>([])
const dialog = ref(false)
const cur = ref<any>({})

async function viewItems(t: any) {
  cur.value = t; dialog.value = true
  items.value = await ruleTemplateApi.items(t.id)
}
onMounted(async () => { templates.value = await ruleTemplateApi.list() })
</script>
