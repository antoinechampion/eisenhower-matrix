import { mount, flushPromises } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import NoteCard from './NoteCard.vue'

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

const defaultProps = {
  noteId: 'note-1',
  text: 'Buy milk',
  urgency: 0.2,
  importance: 0.5,
  background: 'hsla(0, 90%, 89%, 0.7)',
  border: 'hsl(0, 70%, 69%)',
  canvasWidth: 1000,
  canvasHeight: 800,
}

describe('NoteCard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the note text in a textarea', () => {
    const wrapper = mount(NoteCard, { props: defaultProps })
    expect(wrapper.find('textarea').element.value).toBe('Buy milk')
  })

  it('emits update with new text on input', async () => {
    const wrapper = mount(NoteCard, { props: defaultProps })
    const textarea = wrapper.find('textarea')
    await textarea.setValue('New text')
    expect(wrapper.emitted('update')).toBeTruthy()
    expect(wrapper.emitted('update')![0]![0]).toMatchObject({ text: 'New text' })
  })

  it('disables native touch panning on the editable drag surface', () => {
    const wrapper = mount(NoteCard, { props: defaultProps })
    const textarea = wrapper.find('.note-textarea')

    const textareaStyle = (wrapper.vm as any).$?.subTree.children?.[0]?.props.style
    expect(textareaStyle['touch-action']).toBe('none')
    expect((textarea.element as HTMLTextAreaElement).readOnly).toBe(false)
  })

  it('applies correct transform style from urgency/importance props', () => {
    const wrapper = mount(NoteCard, { props: defaultProps })
    const style = wrapper.find('.note-card').attributes('style')
    expect(style).toContain('translate(200px, 400px)')
  })

  it('repositions from saved normalized coordinates when the board is resized', async () => {
    const wrapper = mount(NoteCard, { props: defaultProps })

    await wrapper.setProps({ canvasWidth: 500, canvasHeight: 400 })

    expect(wrapper.find('.note-card').attributes('style')).toContain('translate(100px, 200px)')
    expect(wrapper.emitted('update')).toBeFalsy()
  })

  it('keeps the visible note within resized board bounds without changing saved coordinates', async () => {
    const wrapper = mount(NoteCard, {
      props: { ...defaultProps, urgency: 0.9, importance: 0.95 },
    })
    const card = wrapper.find('.note-card').element as HTMLElement
    Object.defineProperty(card, 'offsetWidth', { configurable: true, value: 150 })
    Object.defineProperty(card, 'offsetHeight', { configurable: true, value: 75 })

    await wrapper.setProps({ canvasWidth: 320, canvasHeight: 240 })

    expect(card.style.transform).toBe('translate(170px, 165px)')
    expect(wrapper.emitted('update')).toBeFalsy()
  })

  it('renders no emoji when flag is null or undefined', () => {
    const wrapper = mount(NoteCard, { props: { ...defaultProps, flag: null } })
    const badge = wrapper.find('.flag-badge')
    expect(badge.exists()).toBe(true)
    expect(badge.classes()).toContain('is-empty')
    expect(badge.text()).toBe('')
    expect(badge.attributes('aria-label')).toBe('Flag: None. Activate to set Quick Win')
  })

  it('renders ⚡ when flag is QUICK_WIN', () => {
    const wrapper = mount(NoteCard, { props: { ...defaultProps, flag: 'QUICK_WIN' } })
    const badge = wrapper.find('.flag-badge')
    expect(badge.classes()).toContain('has-flag')
    expect(badge.text()).toBe('⚡')
    expect(badge.attributes('aria-label')).toBe('Flag: Quick Win (⚡). Activate to change to Long Term')
  })

  it('renders 🐌 when flag is LONG_TERM', () => {
    const wrapper = mount(NoteCard, { props: { ...defaultProps, flag: 'LONG_TERM' } })
    const badge = wrapper.find('.flag-badge')
    expect(badge.classes()).toContain('has-flag')
    expect(badge.text()).toBe('🐌')
    expect(badge.attributes('aria-label')).toBe('Flag: Long Term (🐌). Activate to remove flag')
  })

  it('cycles flag from null -> QUICK_WIN on badge click', async () => {
    const wrapper = mount(NoteCard, { props: { ...defaultProps, flag: null } })
    await wrapper.find('.flag-badge').trigger('click')
    expect(wrapper.emitted('update')).toBeTruthy()
    expect(wrapper.emitted('update')![0]![0]).toEqual({ flag: 'QUICK_WIN' })
  })

  it('cycles flag from QUICK_WIN -> LONG_TERM on badge click', async () => {
    const wrapper = mount(NoteCard, { props: { ...defaultProps, flag: 'QUICK_WIN' } })
    await wrapper.find('.flag-badge').trigger('click')
    expect(wrapper.emitted('update')).toBeTruthy()
    expect(wrapper.emitted('update')![0]![0]).toEqual({ flag: 'LONG_TERM' })
  })

  it('cycles flag from LONG_TERM -> null on badge click', async () => {
    const wrapper = mount(NoteCard, { props: { ...defaultProps, flag: 'LONG_TERM' } })
    await wrapper.find('.flag-badge').trigger('click')
    expect(wrapper.emitted('update')).toBeTruthy()
    expect(wrapper.emitted('update')![0]![0]).toEqual({ flag: null })
  })

  it('configures interactjs draggable and resizable to ignore .flag-badge', async () => {
    mount(NoteCard, { props: defaultProps })
    await flushPromises()

    expect(interactFn).toHaveBeenCalled()
    expect(interactInstance.resizable).toHaveBeenCalledWith(
      expect.objectContaining({ ignoreFrom: '.flag-badge' }),
    )
    expect(interactInstance.draggable).toHaveBeenCalledWith(
      expect.objectContaining({ ignoreFrom: '.flag-badge' }),
    )
  })

  it('updates and saves every consecutive drag move in normalized board coordinates', async () => {
    let dragMoveListener: ((event: any) => void) | undefined
    interactInstance.draggable.mockImplementationOnce((options: any) => {
      dragMoveListener = options?.listeners?.move
      return interactInstance
    })

    const wrapper = mount(NoteCard, { props: defaultProps })
    await flushPromises()
    const card = wrapper.find('.note-card').element as HTMLElement

    dragMoveListener!({ target: card, dx: 10, dy: 20 })
    dragMoveListener!({ target: card, dx: 15, dy: -5 })

    expect(card.dataset.x).toBe('225')
    expect(card.dataset.y).toBe('415')
    expect(wrapper.emitted('update')).toEqual([
      [{ urgency: 0.21, importance: 0.525 }],
      [{ urgency: 0.225, importance: 0.51875 }],
    ])
  })

  it('unsets the interactable when the note is unmounted', async () => {
    const wrapper = mount(NoteCard, { props: defaultProps })
    await flushPromises()

    wrapper.unmount()

    expect(interactInstance.unset).toHaveBeenCalledOnce()
  })

  it('emits focus on card pointerdown but isolates badge pointerdown', async () => {
    const wrapper = mount(NoteCard, { props: defaultProps })

    // Clicking / pointerdown on badge should stop propagation and not emit focus
    await wrapper.find('.flag-badge').trigger('pointerdown')
    expect(wrapper.emitted('focus')).toBeFalsy()

    // Pointerdown on the card wrapper should emit focus
    await wrapper.find('.note-card').trigger('pointerdown')
    expect(wrapper.emitted('focus')).toBeTruthy()
    expect(wrapper.emitted('focus')![0]).toEqual(['note-1'])
  })

  it('maintains resized coordinates across badge click and re-render after top/left resize', async () => {
    let resizeMoveListener: ((event: any) => void) | undefined
    interactInstance.resizable.mockImplementationOnce((options: any) => {
      resizeMoveListener = options?.listeners?.move
      return interactInstance
    })

    const wrapper = mount(NoteCard, { props: defaultProps })
    await flushPromises()

    expect(resizeMoveListener).toBeDefined()
    const cardEl = wrapper.find('.note-card').element as HTMLElement

    // Initial transform: (200px, 400px)
    expect(cardEl.style.transform).toBe('translate(200px, 400px)')

    // Simulate top/left edge resize delta: -30px left, -20px top
    resizeMoveListener!({
      target: cardEl,
      rect: { width: 180, height: 100 },
      deltaRect: { left: -30, top: -20, right: 0, bottom: 0 },
    })

    expect(cardEl.dataset.x).toBe('170')
    expect(cardEl.dataset.y).toBe('380')

    // Moving top/left edges persists the new position via the same update event as drag
    expect(wrapper.emitted('update')).toHaveLength(1)
    expect(wrapper.emitted('update')![0]![0]).toEqual({ urgency: 0.17, importance: 0.475 })

    // Trigger flag badge click which emits update and re-renders with new flag
    await wrapper.find('.flag-badge').trigger('click')
    await wrapper.setProps({ flag: 'QUICK_WIN' })
    await flushPromises()

    // Assert that style transform maintains resized coordinates (170px, 380px) and does not jump back to (200px, 400px)
    const style = wrapper.find('.note-card').attributes('style')
    expect(style).toContain('translate(170px, 380px)')
    expect(wrapper.emitted('update')![1]![0]).toEqual({ flag: 'QUICK_WIN' })
  })

  it('does not emit position update for bottom/right-only resize', async () => {
    let resizeMoveListener: ((event: any) => void) | undefined
    interactInstance.resizable.mockImplementationOnce((options: any) => {
      resizeMoveListener = options?.listeners?.move
      return interactInstance
    })

    const wrapper = mount(NoteCard, { props: defaultProps })
    await flushPromises()
    const cardEl = wrapper.find('.note-card').element as HTMLElement

    resizeMoveListener!({
      target: cardEl,
      rect: { width: 220, height: 130 },
      deltaRect: { left: 0, top: 0, right: 20, bottom: 30 },
    })

    expect(cardEl.style.width).toBe('220px')
    expect(cardEl.style.height).toBe('130px')
    expect(cardEl.dataset.x).toBe('200')
    expect(cardEl.dataset.y).toBe('400')
    expect(wrapper.emitted('update')).toBeFalsy()
  })
})
