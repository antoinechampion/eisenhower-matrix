import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import * as api from '@/services/api'
import Editor from './Editor.vue'

vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
    currentRoute: { value: { params: { noteId: 'note-abc' } } },
  },
}))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>()
  return {
    ...actual,
    useRoute: () => ({ params: { noteId: 'note-abc' } }),
  }
})

// Stub TinyMCE global
;(globalThis as any).tinymce = {
  init: vi.fn(),
}

describe('Editor', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('fetches note and document on mount using noteId from route', async () => {
    const notesSpy = vi.spyOn(api.Notes, 'get').mockResolvedValue({ id: 'note-abc', text: 'Test note' })
    const docSpy = vi.spyOn(api.AttachedDocuments, 'get').mockResolvedValue({
      status: 200,
      headers: { has: () => false },
      text: async () => '<p>content</p>',
    })
    mount(Editor)
    await vi.waitFor(() => expect(notesSpy).toHaveBeenCalledWith('note-abc'))
    await vi.waitFor(() => expect(docSpy).toHaveBeenCalledWith('note-abc'))
  })

  it('shows loading text initially', () => {
    vi.spyOn(api.Notes, 'get').mockResolvedValue({ id: 'note-abc', text: '' })
    vi.spyOn(api.AttachedDocuments, 'get').mockResolvedValue({
      status: 200,
      headers: { has: () => false },
      text: async () => '',
    })
    const wrapper = mount(Editor)
    expect(wrapper.find('.loading').exists()).toBe(true)
  })
})
