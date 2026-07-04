import { mount } from '@vue/test-utils'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import PasswordResetModal from './PasswordResetModal.vue'

vi.mock('@/router', () => ({ default: { push: vi.fn() } }))

describe('PasswordResetModal', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders nothing when closed', () => {
    const wrapper = mount(PasswordResetModal, {
      props: { open: false, mode: 'request' },
      attachTo: document.body,
    })
    expect(document.body.querySelector('.backdrop')).toBeNull()
    wrapper.unmount()
  })

  it('request mode: submits email and calls requestPasswordReset', async () => {
    const auth = useAuthStore()
    const spy = vi.spyOn(auth, 'requestPasswordReset').mockResolvedValue()
    const wrapper = mount(PasswordResetModal, {
      props: { open: true, mode: 'request' },
      attachTo: document.body,
    })
    const input = document.body.querySelector('input[type="email"]') as HTMLInputElement
    input.value = 'a@b.com'
    input.dispatchEvent(new Event('input'))
    await wrapper.vm.$nextTick()
    document.body.querySelector('form')!.dispatchEvent(new Event('submit'))
    await wrapper.vm.$nextTick()
    expect(spy).toHaveBeenCalledWith('a@b.com')
    wrapper.unmount()
  })

  it('reset mode: calls resetPassword with the token when passwords match', async () => {
    const auth = useAuthStore()
    const spy = vi.spyOn(auth, 'resetPassword').mockResolvedValue(true)
    const wrapper = mount(PasswordResetModal, {
      props: { open: true, mode: 'reset', token: 'the-token' },
      attachTo: document.body,
    })
    const inputs = document.body.querySelectorAll<HTMLInputElement>('input[type="password"]')
    inputs[0]!.value = 'newpass'
    inputs[0]!.dispatchEvent(new Event('input'))
    inputs[1]!.value = 'newpass'
    inputs[1]!.dispatchEvent(new Event('input'))
    await wrapper.vm.$nextTick()
    document.body.querySelector('form')!.dispatchEvent(new Event('submit'))
    await wrapper.vm.$nextTick()
    expect(spy).toHaveBeenCalledWith('the-token', 'newpass')
    wrapper.unmount()
  })

  it('reset mode: shows mismatch error and does not call resetPassword', async () => {
    const auth = useAuthStore()
    const spy = vi.spyOn(auth, 'resetPassword').mockResolvedValue(true)
    const wrapper = mount(PasswordResetModal, {
      props: { open: true, mode: 'reset', token: 'the-token' },
      attachTo: document.body,
    })
    const inputs = document.body.querySelectorAll<HTMLInputElement>('input[type="password"]')
    inputs[0]!.value = 'newpass'
    inputs[0]!.dispatchEvent(new Event('input'))
    inputs[1]!.value = 'different'
    inputs[1]!.dispatchEvent(new Event('input'))
    await wrapper.vm.$nextTick()
    document.body.querySelector('form')!.dispatchEvent(new Event('submit'))
    await wrapper.vm.$nextTick()
    expect(spy).not.toHaveBeenCalled()
    expect(document.body.querySelector('.error-alert')!.textContent).toContain('do not match')
    wrapper.unmount()
  })

  it('shows the success message from the auth store', async () => {
    const auth = useAuthStore()
    auth.resetSuccess = 'If that email is registered, a reset link has been sent.'
    const wrapper = mount(PasswordResetModal, {
      props: { open: true, mode: 'request' },
      attachTo: document.body,
    })
    await wrapper.vm.$nextTick()
    expect(document.body.querySelector('.success-alert')!.textContent).toContain('reset link')
    wrapper.unmount()
  })
})
