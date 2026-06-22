<template>
  <section class="py-5 container">
    <div class="auth-card card border-0 shadow-sm rounded-4">
      <div class="card-body p-4 p-md-5">
        <h2 class="fw-bold mb-4">로그인</h2>
        <form @submit.prevent="submit">
          <input v-model="form.id" type="text" class="form-control mb-3" placeholder="아이디" required>
          <input v-model="form.password" type="password" class="form-control mb-3" placeholder="비밀번호" required>
          <button class="btn btn-indigo w-100 mb-3" type="submit" :disabled="loading">
            {{ loading ? '로그인 중...' : '로그인' }}
          </button>
        </form>
        <div class="d-flex justify-content-between">
          <router-link to="/auth/register">회원가입</router-link>
          <router-link to="/auth/find-password">비밀번호 찾기</router-link>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth.js'
import { useToastStore } from '../../stores/toast.js'

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()
const loading = ref(false)
const form = ref({ id: '', password: '' })

async function submit() {
  loading.value = true
  try {
    await authStore.login(form.value.id, form.value.password)
    toastStore.show('로그인되었습니다.')
    router.push('/')
  } catch {
    toastStore.show('아이디 또는 비밀번호가 올바르지 않습니다.', 'danger')
  } finally {
    loading.value = false
  }
}
</script>
