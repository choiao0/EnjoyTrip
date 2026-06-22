<template>
  <section class="py-5 container">
    <div class="auth-card card border-0 shadow-sm rounded-4">
      <div class="card-body p-4 p-md-5">
        <h2 class="fw-bold mb-4">비밀번호 찾기</h2>
        <form v-if="!result" @submit.prevent="submit">
          <input v-model="form.id" type="text" class="form-control mb-3" placeholder="아이디" required>
          <input v-model="form.name" type="text" class="form-control mb-3" placeholder="이름" required>
          <button class="btn btn-indigo w-100" type="submit" :disabled="loading">
            {{ loading ? '확인 중...' : '비밀번호 확인' }}
          </button>
        </form>
        <div v-else>
          <div class="alert alert-info rounded-4">{{ result }}</div>
          <router-link class="btn btn-outline-secondary w-100" to="/auth/login">로그인으로 돌아가기</router-link>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { authApi } from '../../api/index.js'
import { useToastStore } from '../../stores/toast.js'

const toastStore = useToastStore()
const loading = ref(false)
const result = ref(null)
const form = ref({ id: '', name: '' })

async function submit() {
  loading.value = true
  try {
    const res = await authApi.findPassword(form.value.id, form.value.name)
    result.value = res.data.message
  } catch {
    toastStore.show('확인 중 오류가 발생했습니다.', 'danger')
  } finally {
    loading.value = false
  }
}
</script>
