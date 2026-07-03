import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSettingsStore } from '@/stores/settings'
import { useAuthStore } from '@/stores/auth'
import MatrixSettings from './MatrixSettings.vue'

vi.mock('@/router', () => ({ default: { push: vi.fn() } }))

describe('MatrixSettings', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  function openMenu(wrapper: ReturnType<typeof mount>) {
    ;(wrapper.vm as any).toggleMenu()
    return wrapper.vm.$nextTick()
  }

  it('calls auth.logout when logout button clicked', async () => {
    const auth = useAuthStore()
    const spy = vi.spyOn(auth, 'logout').mockResolvedValue()
    const wrapper = mount(MatrixSettings)
    await openMenu(wrapper)
    await wrapper.find('.logout').trigger('click')
    expect(spy).toHaveBeenCalled()
  })

  it('updates settings.palette when a palette is selected', async () => {
    const settings = useSettingsStore()
    const wrapper = mount(MatrixSettings)
    await openMenu(wrapper)
    const buttons = wrapper.findAll('.menu-item')
    const shadeBtn = buttons.find((b) => b.text() === 'Shade')!
    await shadeBtn.trigger('click')
    expect(settings.palette).toBe('Shade')
  })

  it('toggles flipMatrix when Flip Matrix clicked', async () => {
    const settings = useSettingsStore()
    const wrapper = mount(MatrixSettings)
    await openMenu(wrapper)
    const flipBtn = wrapper.findAll('.menu-item').find((b) => b.text() === 'Flip Matrix')!
    await flipBtn.trigger('click')
    expect(settings.flipMatrix).toBe(true)
  })
})
