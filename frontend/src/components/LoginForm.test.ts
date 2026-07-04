import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import LoginForm from './LoginForm.vue'

vi.mock('@/router', () => ({ default: { push: vi.fn() } }))

describe('LoginForm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders Login and Register tabs', () => {
    const wrapper = mount(LoginForm)
    const buttons = wrapper.findAll('.tabs button')
    expect(buttons.map((b) => b.text())).toContain('Log In')
    expect(buttons.map((b) => b.text())).toContain('Register')
  })

  it('calls auth.login on login submit', async () => {
    const auth = useAuthStore()
    const spy = vi.spyOn(auth, 'login').mockResolvedValue()
    const wrapper = mount(LoginForm)
    await wrapper.find('input[type="email"]').setValue('a@b.com')
    await wrapper.find('input[type="password"]').setValue('secret')
    await wrapper.find('form').trigger('submit')
    expect(spy).toHaveBeenCalledWith('a@b.com', 'secret')
  })

  it('calls auth.register on register submit', async () => {
    const auth = useAuthStore()
    const spy = vi.spyOn(auth, 'register').mockResolvedValue()
    const wrapper = mount(LoginForm)
    // Switch to register tab
    await wrapper.findAll('.tabs button').find((b) => b.text() === 'Register')!.trigger('click')
    await wrapper.find('input[type="email"]').setValue('new@user.com')
    await wrapper.find('input[type="password"]').setValue('pass123')
    await wrapper.find('form').trigger('submit')
    expect(spy).toHaveBeenCalledWith('new@user.com', 'pass123')
  })

  it('shows error message from auth store', async () => {
    const auth = useAuthStore()
    auth.error = 'Wrong email or password'
    const wrapper = mount(LoginForm)
    expect(wrapper.find('.error-alert').text()).toBe('Wrong email or password')
  })

  it('emits forgot-password when the link is clicked', async () => {
    const wrapper = mount(LoginForm)
    await wrapper.find('.forgot-link').trigger('click')
    expect(wrapper.emitted('forgot-password')).toBeTruthy()
  })
})
