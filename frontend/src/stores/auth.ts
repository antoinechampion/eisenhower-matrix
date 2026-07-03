import { defineStore } from 'pinia'
import { Auth, Boards } from '@/services/api'
import router from '@/router'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    error: null as string | null,
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
  },
})
