<template>
  <span class="severity-tag" :data-type="type">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { severityType, severityLabel } from '@/utils/display'

const props = defineProps<{ severity: string }>()
const type = computed(() => severityType(props.severity))
const label = computed(() => severityLabel(props.severity))
</script>

<style scoped>
.severity-tag { display: inline-block; padding: 1px 9px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.severity-tag[data-type='danger'] { background: #fdeceb; color: #d92d20; }
.severity-tag[data-type='warning'] { background: #fdf3e2; color: #e08e0b; }
.severity-tag[data-type='info'] { background: #eef1f6; color: #5b6b88; }
.severity-tag[data-type='success'] { background: #e8f7ee; color: #18a058; }
</style>
