import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import SyncStatus from './SyncStatus.vue'

describe('SyncStatus', () => {
  it('hides the icon when not active', () => {
    const wrapper = mount(SyncStatus, { props: { active: false } })
    expect(wrapper.find('svg').exists()).toBe(false)
  })

  it('shows the icon when active', () => {
    const wrapper = mount(SyncStatus, { props: { active: true } })
    expect(wrapper.find('svg').exists()).toBe(true)
  })
})
