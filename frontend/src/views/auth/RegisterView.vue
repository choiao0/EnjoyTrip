<template>
  <section class="py-5 container">
    <div class="auth-card card border-0 shadow-sm rounded-4">
      <div class="card-body p-4 p-md-5">
        <h2 class="fw-bold mb-4">회원가입</h2>
        <form @submit.prevent="submit">
          <input v-model="form.id" type="text" class="form-control mb-3" placeholder="아이디" required>
          <input v-model="form.name" type="text" class="form-control mb-3" placeholder="이름" required>
          <input v-model="form.password" type="password" class="form-control mb-3" placeholder="비밀번호" required>
          <button class="btn btn-indigo w-100" type="submit" :disabled="loading">
            {{ loading ? '처리 중...' : '가입하기' }}
          </button>
        </form>
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
const form = ref({ id: '', name: '', password: '' })

async function submit() {
  loading.value = true
  try {
    await authStore.register(form.value.id, form.value.name, form.value.password)
    toastStore.show('회원가입되었습니다.')
    router.push('/')
  } catch (e) {
    const msg = e.response?.data?.message || '회원가입 중 오류가 발생했습니다.'
    toastStore.show(msg, 'danger')
  } finally {
    loading.value = false
  }
}
</script>
