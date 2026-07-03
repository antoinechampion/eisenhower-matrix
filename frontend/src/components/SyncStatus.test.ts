import { mount } from '@vue/test-utils'
import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useMatrixStore } from '@/stores/matrix'
import SyncStatus from './SyncStatus.vue'

describe('SyncStatus', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('hides the icon when there are no dirty notes', () => {
    const wrapper = mount(SyncStatus)
    expect(wrapper.find('svg').exists()).toBe(false)
  })

  it('shows the icon when there are dirty notes', async () => {
    const matrix = useMatrixStore()
    matrix.dirtyNoteIds.add('note-1')
    const wrapper = mount(SyncStatus)
    expect(wrapper.find('svg').exists()).toBe(true)
  })
})
