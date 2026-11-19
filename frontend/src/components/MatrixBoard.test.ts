import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useMatrixStore } from '@/stores/matrix'
import MatrixBoard from './MatrixBoard.vue'

vi.mock('@/router', () => ({ default: { push: vi.fn(), currentRoute: { value: {} } } }))

const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: { template: '<div/>' } }] })
const interactInstance = {
  resizable: vi.fn().mockReturnThis(),
  draggable: vi.fn().mockReturnThis(),
  unset: vi.fn(),
}
const interactFn = Object.assign(vi.fn().mockReturnValue(interactInstance), {
  modifiers: {
    restrictEdges: vi.fn().mockReturnValue({}),
    restrictSize: vi.fn().mockReturnValue({}),
  },
})
vi.mock('interactjs', () => ({ default: interactFn }))
// Stub canvas
vi.mock('./BackgroundCanvas.vue', () => ({ default: { template: '<canvas />' } }))

describe('MatrixBoard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('calls matrix.loadNotes on mount', async () => {
    const matrix = useMatrixStore()
    const spy = vi.spyOn(matrix, 'loadNotes').mockResolvedValue()
    mount(MatrixBoard, { global: { plugins: [router] } })
    expect(spy).toHaveBeenCalled()
  })

  it('renders a NoteCard for each note in the store', async () => {
    const matrix = useMatrixStore()
    vi.spyOn(matrix, 'loadNotes').mockResolvedValue()
    matrix.notes = [
      { id: '1', text: 'Note A', urgency: 0.1, importance: 0.1, boardId: 'b1', background: '#fff', border: '#000' },
      { id: '2', text: 'Note B', urgency: 0.5, importance: 0.5, boardId: 'b1', background: '#fff', border: '#000' },
    ]
    const wrapper = mount(MatrixBoard, { global: { plugins: [router] } })
    const textareas = wrapper.findAll('textarea')
    expect(textareas).toHaveLength(2)
  })

  it('calls matrix.updateNote when a NoteCard emits update', async () => {
    const matrix = useMatrixStore()
    vi.spyOn(matrix, 'loadNotes').mockResolvedValue()
    matrix.notes = [
      { id: '1', text: 'Test', urgency: 0.1, importance: 0.1, boardId: 'b1', background: '#fff', border: '#000' },
    ]
    const spy = vi.spyOn(matrix, 'updateNote')
    const wrapper = mount(MatrixBoard, { global: { plugins: [router] } })
    await wrapper.find('textarea').setValue('Updated')
    expect(spy).toHaveBeenCalledWith('1', expect.objectContaining({ text: 'Updated' }))
  })

  it('calls matrix.updateNote when a NoteCard flag badge is clicked', async () => {
    const matrix = useMatrixStore()
    vi.spyOn(matrix, 'loadNotes').mockResolvedValue()
    matrix.notes = [
      { id: '1', text: 'Task 1', urgency: 0.1, importance: 0.1, boardId: 'b1', flag: null, background: '#fff', border: '#000' },
    ]
    const spy = vi.spyOn(matrix, 'updateNote')
    const wrapper = mount(MatrixBoard, { global: { plugins: [router] } })
    await wrapper.find('.flag-badge').trigger('click')
    expect(spy).toHaveBeenCalledWith('1', expect.objectContaining({ flag: 'QUICK_WIN' }))
  })

  it('renders flag emojis on NoteCards', async () => {
    const matrix = useMatrixStore()
    vi.spyOn(matrix, 'loadNotes').mockResolvedValue()
    matrix.notes = [
      { id: '1', text: 'Quick', urgency: 0.1, importance: 0.1, boardId: 'b1', flag: 'QUICK_WIN', background: '#fff', border: '#000' },
      { id: '2', text: 'Long', urgency: 0.5, importance: 0.5, boardId: 'b1', flag: 'LONG_TERM', background: '#fff', border: '#000' },
    ]
    const wrapper = mount(MatrixBoard, { global: { plugins: [router] } })
    const badges = wrapper.findAll('.flag-badge')
    expect(badges[0]!.text()).toBe('⚡')
    expect(badges[1]!.text()).toBe('🐌')
  })
})
