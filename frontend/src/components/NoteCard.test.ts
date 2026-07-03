import { mount } from '@vue/test-utils'
import { describe, it, expect, vi } from 'vitest'
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

  it('applies correct transform style from urgency/importance props', () => {
    const wrapper = mount(NoteCard, { props: defaultProps })
    const style = wrapper.find('textarea').attributes('style')
    expect(style).toContain('translate(200px, 400px)')
  })
})
