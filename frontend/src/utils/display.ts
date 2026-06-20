import { SEVERITY_TYPE, STATUS_TYPE } from '@/types'

/** 严重级别 → Element Plus tag type */
export function severityType(s: string): string {
  return SEVERITY_TYPE[s] || 'info'
}

/** 状态 → Element Plus tag type */
export function statusType(s: string): string {
  return STATUS_TYPE[s] || 'info'
}

/** 严重级别中文标签 */
export function severityLabel(s: string): string {
  const map: Record<string, string> = {
    INFO: '信息', LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '严重',
  }
  return map[s] || s
}

/** 数字百分比格式化（保留 n 位，附 %） */
export function formatPercent(v: number | string | null | undefined, digits = 2): string {
  if (v === null || v === undefined || v === '') return '-'
  const n = Number(v)
  if (Number.isNaN(n)) return '-'
  return `${n.toFixed(digits)}%`
}
