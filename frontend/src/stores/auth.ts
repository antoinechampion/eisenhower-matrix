import { defineStore } from 'pinia'
import { Auth, Boards } from '@/services/api'
import router from '@/router'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    error: null as string | null,
    resetError: null as string | null,
    resetSuccess: null as string | null,
  }),
  actions: {
    async login(email: string, password: string) {
      this.error = null
      const res = await Auth.login(email, password).catch(() => null)
      if (!res) {
        this.error = "We can't log you in — an internal error happened"
        return
      }
      if (res.status === 401) {
        this.error = 'Wrong email or password'
        return
      }
      router.push('/matrix')
    },
    async register(email: string, password: string) {
      this.error = null
      const res = await Auth.register(email, password).catch(() => null)
      if (!res) {
        this.error = "We can't create your account — an internal error happened"
        return
      }
      if (res.status === 409) {
        this.error = 'An account already exists with this email — please log in'
        return
      }
      await Boards.create('My personal board').catch(() => null)
      router.push('/matrix')
    },
    async logout() {
      await Auth.logout().catch(() => null)
      router.push('/')
    },
    async requestPasswordReset(email: string) {
      this.resetError = null
      this.resetSuccess = null
      // Always report success — the backend never reveals whether the email is registered.
      await Auth.forgotPassword(email).catch(() => null)
      this.resetSuccess = 'If that email is registered, a reset link has been sent.'
    },
    async resetPassword(token: string, newPassword: string) {
      this.resetError = null
      this.resetSuccess = null
      const res = await Auth.resetPassword(token, newPassword).catch(() => null)
      if (!res) {
        this.resetError = "We can't reset your password — an internal error happened"
        return false
      }
      if (res.status === 400) {
        this.resetError = 'This reset link is invalid or has expired'
        return false
      }
      this.resetSuccess = 'Your password has been reset. You can now log in.'
      return true
    },
  },
})
