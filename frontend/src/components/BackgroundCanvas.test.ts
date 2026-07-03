import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import BackgroundCanvas from './BackgroundCanvas.vue'

function mockCanvasContext() {
  const ctx = {
    fillStyle: '',
    strokeStyle: '',
    lineWidth: 0,
    font: '',
    fillRect: vi.fn(),
    beginPath: vi.fn(),
    moveTo: vi.fn(),
    lineTo: vi.fn(),
    stroke: vi.fn(),
    restore: vi.fn(),
    fillText: vi.fn(),
  }
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(ctx as any)
  return ctx
}

describe('BackgroundCanvas', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders a canvas element', () => {
    mockCanvasContext()
    const wrapper = mount(BackgroundCanvas)
    expect(wrapper.find('canvas').exists()).toBe(true)
  })

  it('calls getContext on the canvas on mount', () => {
    const spy = mockCanvasContext()
    mount(BackgroundCanvas)
    expect(HTMLCanvasElement.prototype.getContext).toHaveBeenCalledWith('2d')
  })
})
