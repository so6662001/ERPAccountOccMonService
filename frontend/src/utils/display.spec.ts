import { describe, it, expect } from 'vitest'
import { severityType, statusType, severityLabel, formatPercent } from './display'

describe('display utils', () => {
  it('severityType maps correctly', () => {
    expect(severityType('CRITICAL')).toBe('danger')
    expect(severityType('HIGH')).toBe('warning')
    expect(severityType('INFO')).toBe('info')
    expect(severityType('UNKNOWN')).toBe('info')
  })

  it('statusType maps correctly', () => {
    expect(statusType('PASSED')).toBe('success')
    expect(statusType('FAILED')).toBe('danger')
    expect(statusType('PROCESSING')).toBe('warning')
  })

  it('severityLabel localizes', () => {
    expect(severityLabel('CRITICAL')).toBe('严重')
    expect(severityLabel('HIGH')).toBe('高')
  })

  it('formatPercent formats and guards', () => {
    expect(formatPercent(99.2)).toBe('99.20%')
    expect(formatPercent(99.236)).toBe('99.24%')
    expect(formatPercent(100, 0)).toBe('100%')
    expect(formatPercent(null)).toBe('-')
    expect(formatPercent('abc')).toBe('-')
  })
})
