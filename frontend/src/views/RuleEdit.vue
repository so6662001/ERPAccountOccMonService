<template>
  <h2 class="page-title">{{ isEdit ? '编辑规则' : '新建规则' }}</h2>
  <el-card>
    <el-form label-width="130px" style="max-width:880px">
      <el-form-item label="规则 Key" required>
        <el-input v-model="form.ruleKey" :disabled="isEdit" placeholder="如 reconciliation.ar_control" />
      </el-form-item>
      <el-form-item label="规则名称" required><el-input v-model="form.name" /></el-form-item>
      <el-row :gutter="16">
        <el-col :span="8"><el-form-item label="账务类别">
          <el-select v-model="form.category" style="width:100%"><el-option v-for="c in categories" :key="c" :label="c" :value="c" /></el-select>
        </el-form-item></el-col>
        <el-col :span="8"><el-form-item label="检查类型">
          <el-select v-model="form.type" style="width:100%"><el-option v-for="t in types" :key="t" :label="t" :value="t" /></el-select>
        </el-form-item></el-col>
        <el-col :span="8"><el-form-item label="严重级别">
          <el-select v-model="form.severity" style="width:100%"><el-option v-for="s in sevs" :key="s" :label="s" :value="s" /></el-select>
        </el-form-item></el-col>
      </el-row>
      <el-form-item label="约束定义(spec)">
        <el-input v-model="form.specJson" type="textarea" :rows="5"
          placeholder='例: {"datasourceId":9,"leftSql":"SELECT SUM(amount) ...","rightSql":"SELECT closing ...","tolerance":0.005}' />
      </el-form-item>
      <el-form-item label="适用范围(scope)">
        <el-input v-model="form.scopeJson" type="textarea" :rows="3"
          placeholder='例: {"products":["商贸ERP"],"versionExpr":">=v3.0","servers":[],"ruleSets":["商贸标准集"]}' />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="8"><el-form-item label="迭代回归不变量">
          <el-switch v-model="form.invariant" :active-value="1" :inactive-value="0" />
        </el-form-item></el-col>
        <el-col :span="8"><el-form-item label="不变量指标"><el-input v-model="form.invariantMetric" placeholder="value" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="不变量容差"><el-input v-model="form.invariantTolerance" placeholder="0.005" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="调度(schedule)"><el-input v-model="form.scheduleJson" type="textarea" :rows="2" /></el-form-item>
      <el-form-item label="告警(alert)"><el-input v-model="form.alertJson" type="textarea" :rows="2" placeholder='{"channel":"wecom"}' /></el-form-item>
      <el-form-item label="变更说明"><el-input v-model="form.changeSummary" /></el-form-item>
      <el-form-item label="启用"><el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      <el-form-item>
        <el-button @click="$router.back()">返回</el-button>
        <el-button type="primary" @click="save">保存规则</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ruleApi } from '@/api'

const route = useRoute()
const router = useRouter()
const key = route.query.key as string | undefined
const isEdit = computed(() => !!key)

const categories = ['balance', 'reconciliation', 'continuity', 'cost', 'integrity', 'cross_system', 'regression']
const types = ['scalar_zero', 'scalar_equality', 'scalar_range', 'rows_empty', 'cross_system_scalar_equality']
const sevs = ['INFO', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

const form = reactive<any>({
  ruleKey: '', name: '', category: 'balance', type: 'scalar_zero', severity: 'HIGH',
  specJson: '', scopeJson: '', invariant: 0, invariantMetric: '', invariantTolerance: '',
  scheduleJson: '', alertJson: '', changeSummary: '', enabled: 1,
})

onMounted(async () => {
  if (key) {
    const r: any = await ruleApi.detail(key)
    Object.assign(form, r)
  }
})

async function save() {
  if (!form.ruleKey || !form.name) { ElMessage.warning('请填写 Key 与名称'); return }
  await ruleApi.save({ ...form, invariantTolerance: form.invariantTolerance || null })
  ElMessage.success('已保存（自动版本化）')
  router.push('/rules')
}
</script>
