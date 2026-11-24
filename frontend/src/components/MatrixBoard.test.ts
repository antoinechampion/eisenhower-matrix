import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useMatrixStore } from '@/stores/matrix'
import { useSettingsStore } from '@/stores/settings'
import MatrixBoard from './MatrixBoard.vue'
import NoteCard from './NoteCard.vue'
import BackgroundCanvas from './BackgroundCanvas.vue'

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
vi.mock('./BackgroundCanvas.vue', () => ({ default: { props: ['width', 'height'], template: '<canvas />' } }))

describe('MatrixBoard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(async () => {
    await flushPromises()
    vi.unstubAllGlobals()
  })

  it('calls matrix.loadNotes on mount', async () => {
    const matrix = useMatrixStore()
    const spy = vi.spyOn(matrix, 'loadNotes').mockResolvedValue()
    mount(MatrixBoard, { global: { plugins: [router] } })
    expect(spy).toHaveBeenCalled()
  })

  it('shares measured board dimensions and responds to resizing', async () => {
    const matrix = useMatrixStore()
    vi.spyOn(matrix, 'loadNotes').mockResolvedValue()
    matrix.notes = [
      { id: '1', text: 'Note A', urgency: 0.1, importance: 0.1, boardId: 'b1', background: '#fff', border: '#000' },
    ]
    const bounds = { width: 320, height: 480 }
    const getBoundingClientRect = HTMLElement.prototype.getBoundingClientRect
    const getBoundingClientRectSpy = vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(function (this: HTMLElement) {
      if (this.classList.contains('matrix-container')) {
        return bounds as DOMRect
      }
      return getBoundingClientRect.call(this)
    })
    let resizeCallback: ResizeObserverCallback | undefined
    const observe = vi.fn()
    const disconnect = vi.fn()
    vi.stubGlobal('ResizeObserver', class {
      constructor(callback: ResizeObserverCallback) {
        resizeCallback = callback
      }
      observe = observe
      disconnect = disconnect
    })

    const wrapper = mount(MatrixBoard, { global: { plugins: [router] } })
    await nextTick()

    expect(wrapper.findComponent(NoteCard).props('canvasWidth')).toBe(320)
    expect(wrapper.findComponent(NoteCard).props('canvasHeight')).toBe(480)
    expect(wrapper.findComponent(BackgroundCanvas).props()).toEqual({ width: 320, height: 480 })
    expect(observe).toHaveBeenCalled()

    bounds.width = 600
    bounds.height = 400
    resizeCallback?.([] as ResizeObserverEntry[], {} as ResizeObserver)
    await nextTick()

    expect(wrapper.findComponent(NoteCard).props('canvasWidth')).toBe(600)
    expect(wrapper.findComponent(NoteCard).props('canvasHeight')).toBe(400)
    expect(wrapper.findComponent(BackgroundCanvas).props()).toEqual({ width: 600, height: 400 })
    wrapper.unmount()
    expect(disconnect).toHaveBeenCalled()
    getBoundingClientRectSpy.mockRestore()
  })

  it('keeps floating controls and the settings menu interactive', async () => {
    vi.spyOn(useMatrixStore(), 'loadNotes').mockResolvedValue()
    const settings = useSettingsStore()
    const wrapper = mount(MatrixBoard, { global: { plugins: [router] } })

    const icons = wrapper.findAll('.buttons-bar .icon')
    expect(icons).toHaveLength(4)
    await icons[3]!.trigger('click')
    expect(wrapper.find('.settings-menu').exists()).toBe(true)

    const flipMatrixItem = wrapper.findAll('.settings-menu .menu-item').at(-1)!
    const flipMatrixBefore = settings.flipMatrix
    await flipMatrixItem.trigger('click')
    expect(settings.flipMatrix).toBe(!flipMatrixBefore)
    settings.toggleFlip()
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
