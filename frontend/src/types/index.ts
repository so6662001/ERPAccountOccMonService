export interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export const SEVERITY_TYPE: Record<string, string> = {
  INFO: 'info',
  LOW: 'info',
  MEDIUM: 'warning',
  HIGH: 'warning',
  CRITICAL: 'danger',
}

export const STATUS_TYPE: Record<string, string> = {
  PASSED: 'success',
  FAILED: 'danger',
  ERROR: 'danger',
  SKIPPED: 'info',
  PENDING: 'danger',
  PROCESSING: 'warning',
  CLOSED: 'success',
}
