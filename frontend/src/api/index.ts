import http from './http'
import type { PageResult } from '@/types'

export const authApi = {
  login: (username: string, password: string) =>
    http.post('/auth/login', { username, password }) as Promise<{ accessToken: string; refreshToken: string }>,
  me: () => http.get('/auth/me') as Promise<any>,
  permissions: () => http.get('/auth/me/permissions') as Promise<string[]>,
  logout: () => http.post('/auth/logout'),
}

export const customerApi = {
  page: (params: any) => http.get('/customers', { params }) as Promise<PageResult<any>>,
  detail: (id: number) => http.get(`/customers/${id}/detail`) as Promise<any>,
  save: (cmd: any) => http.post('/customers', cmd),
}

export const ruleApi = {
  page: (params: any) => http.get('/rules', { params }) as Promise<PageResult<any>>,
  detail: (key: string) => http.get(`/rules/${key}`),
  save: (cmd: any) => http.post('/rules', cmd),
  versions: (key: string) => http.get(`/rules/${key}/versions`) as Promise<any[]>,
  rollback: (key: string, version: number) => http.post(`/rules/${key}/rollback/${version}`),
}

export const ruleTemplateApi = {
  list: () => http.get('/rule-templates') as Promise<any[]>,
  items: (id: number) => http.get(`/rule-templates/${id}/items`) as Promise<any[]>,
  apply: (cmd: any) => http.post('/rule-templates/apply', cmd),
}

export const datasourceApi = {
  list: (customerId?: number) => http.get('/datasources', { params: { customerId } }) as Promise<any[]>,
  save: (cmd: any) => http.post('/datasources', cmd),
  test: (cmd: any) => http.post('/datasources/test', cmd),
  probe: (id: number) => http.post(`/datasources/${id}/probe`),
}

export const taskApi = {
  page: (params: any) => http.get('/tasks', { params }) as Promise<PageResult<any>>,
  save: (cmd: any) => http.post('/tasks', cmd),
  run: (id: number) => http.post(`/tasks/${id}/run`),
}

export const resultApi = {
  pageRuns: (params: any) => http.get('/runs', { params }) as Promise<PageResult<any>>,
  detail: (id: number, params?: any) => http.get(`/runs/${id}`, { params }) as Promise<any>,
}

export const baselineApi = {
  list: (params: any) => http.get('/baselines', { params }) as Promise<any[]>,
  snapshot: (runId: number) => http.post('/baselines/snapshot', null, { params: { runId } }),
  audit: (id: number) => http.post(`/baselines/${id}/audit`),
}

export const rolloutApi = {
  list: () => http.get('/rollouts') as Promise<any[]>,
  detail: (id: number) => http.get(`/rollouts/${id}`) as Promise<any>,
  create: (cmd: any) => http.post('/rollouts', cmd),
  start: (id: number) => http.post(`/rollouts/${id}/start`),
  advance: (id: number) => http.post(`/rollouts/${id}/advance`),
  rollback: (id: number, reason?: string) => http.post(`/rollouts/${id}/rollback`, null, { params: { reason } }),
}

export const alertApi = {
  page: (params: any) => http.get('/alerts', { params }) as Promise<PageResult<any>>,
  claim: (id: number) => http.post(`/alerts/${id}/claim`),
  close: (id: number) => http.post(`/alerts/${id}/close`),
  falsePositive: (id: number) => http.post(`/alerts/${id}/false-positive`),
  channels: () => http.get('/alert-channels') as Promise<any[]>,
  saveChannel: (cmd: any) => http.post('/alert-channels', cmd),
  testChannel: (id: number) => http.post(`/alert-channels/${id}/test`),
}

export const templateSyncApi = {
  diff: (customerId: number, templateId: number) =>
    http.get('/template-sync/diff', { params: { customerId, templateId } }) as Promise<any>,
  apply: (cmd: any) => http.post('/template-sync/apply', cmd),
}

export const analyticsApi = {
  overview: () => http.get('/analytics/overview') as Promise<any>,
  passRateTrend: (days = 14) => http.get('/analytics/pass-rate-trend', { params: { days } }) as Promise<any[]>,
  failuresByCategory: (days = 7) => http.get('/analytics/failures-by-category', { params: { days } }) as Promise<any[]>,
  topFailingRules: (days = 14, limit = 10) => http.get('/analytics/top-failing-rules', { params: { days, limit } }) as Promise<any[]>,
  industryHealth: (days = 7) => http.get('/analytics/industry-health', { params: { days } }) as Promise<any[]>,
  mttr: (days = 7) => http.get('/analytics/mttr', { params: { days } }) as Promise<number>,
}
