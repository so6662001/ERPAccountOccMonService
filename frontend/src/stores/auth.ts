import { defineStore } from 'pinia'
import { authApi } from '@/api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('accessToken') || '',
    user: null as any,
    permissions: [] as string[],
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    roles: (s) => (s.user?.roles || []) as string[],
  },
  actions: {
    async login(username: string, password: string) {
      const data = await authApi.login(username, password)
      this.token = data.accessToken
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      await this.loadMe()
    },
    async loadMe() {
      this.user = await authApi.me()
      this.permissions = this.user?.permissions || []
    },
    logout() {
      this.token = ''
      this.user = null
      this.permissions = []
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    },
  },
})
