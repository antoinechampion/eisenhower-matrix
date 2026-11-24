import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
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

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders a canvas element', () => {
    mockCanvasContext()
    const wrapper = mount(BackgroundCanvas, { props: { width: 320, height: 480 } })
    expect(wrapper.find('canvas').exists()).toBe(true)
  })

  it('calls getContext on the canvas on mount', () => {
    const spy = mockCanvasContext()
    mount(BackgroundCanvas, { props: { width: 320, height: 480 } })
    expect(HTMLCanvasElement.prototype.getContext).toHaveBeenCalledWith('2d')
  })

  it('resizes and redraws when the measured board dimensions change', async () => {
    const ctx = mockCanvasContext()
    const wrapper = mount(BackgroundCanvas, { props: { width: 320, height: 480 } })
    const canvas = wrapper.find('canvas').element

    expect(canvas.width).toBe(320)
    expect(canvas.height).toBe(480)
    const initialDrawCalls = ctx.fillRect.mock.calls.length

    await wrapper.setProps({ width: 600, height: 400 })

    expect(canvas.width).toBe(600)
    expect(canvas.height).toBe(400)
    expect(ctx.fillRect).toHaveBeenCalledTimes(initialDrawCalls * 2)
    wrapper.unmount()
  })
})
