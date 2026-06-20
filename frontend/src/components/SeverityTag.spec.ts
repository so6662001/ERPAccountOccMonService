import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SeverityTag from './SeverityTag.vue'

describe('SeverityTag.vue', () => {
  it('renders localized label and type for CRITICAL', () => {
    const w = mount(SeverityTag, { props: { severity: 'CRITICAL' } })
    expect(w.text()).toBe('严重')
    expect(w.attributes('data-type')).toBe('danger')
  })

  it('falls back to info for unknown severity', () => {
    const w = mount(SeverityTag, { props: { severity: 'XYZ' } })
    expect(w.attributes('data-type')).toBe('info')
    expect(w.text()).toBe('XYZ')
  })
})
