import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '../api/index.js'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const initialized = ref(false)

  async function fetchMe() {
    try {
      const res = await authApi.me()
      user.value = res.data
    } catch {
      user.value = null
    } finally {
      initialized.value = true
    }
  }

  async function login(id, password) {
    const res = await authApi.login(id, password)
    user.value = res.data
    return res.data
  }

  async function logout() {
    await authApi.logout()
    user.value = null
  }

  async function register(id, name, password) {
    const res = await authApi.register(id, name, password)
    user.value = res.data
    return res.data
  }

  return { user, initialized, fetchMe, login, logout, register }
})
