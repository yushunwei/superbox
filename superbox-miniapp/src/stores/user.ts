import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi, type UserInfo } from '@/api/modules/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(uni.getStorageSync('token') || '')
  const user = ref<UserInfo | null>(null)

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    token.value = res.data.token
    uni.setStorageSync('token', res.data.token)
    await fetchUser()
    uni.switchTab({ url: '/pages/index/index' })
  }

  async function wxLogin() {
    const loginRes = await uni.login()
    const res = await authApi.wxLogin(loginRes.code)
    token.value = res.data.token
    uni.setStorageSync('token', res.data.token)
    await fetchUser()
    uni.switchTab({ url: '/pages/index/index' })
  }

  async function fetchUser() {
    const res = await authApi.getMe()
    user.value = res.data
  }

  function logout() {
    token.value = ''
    user.value = null
    uni.removeStorageSync('token')
    uni.reLaunch({ url: '/pages/login/login' })
  }

  function checkAuth(): boolean {
    return !!token.value
  }

  return { token, user, login, wxLogin, fetchUser, logout, checkAuth }
})
