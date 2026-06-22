<template>
  <section class="py-5 container">
    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <h2 class="fw-bold mb-4">{{ isEdit ? '공지사항 수정' : '공지사항 작성' }}</h2>
        <form @submit.prevent="submit">
          <input v-model="form.title" type="text" class="form-control mb-3" placeholder="제목" required>
          <textarea v-model="form.content" class="form-control mb-3" rows="10" placeholder="내용" required></textarea>
          <div class="d-flex gap-2">
            <button class="btn btn-indigo" type="submit" :disabled="submitting">
              {{ submitting ? '처리 중...' : (isEdit ? '수정' : '등록') }}
            </button>
            <router-link class="btn btn-outline-secondary" to="/notices">취소</router-link>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { noticeApi } from '../../api/index.js'
import { useAuthStore } from '../../stores/auth.js'
import { useToastStore } from '../../stores/toast.js'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

const isEdit = computed(() => !!route.params.id && route.path.includes('/edit'))
const submitting = ref(false)
const form = ref({ title: '', content: '' })

onMounted(async () => {
  if (!authStore.user) {
    router.push('/auth/login')
    return
  }
  if (isEdit.value) {
    const res = await noticeApi.detail(route.params.id)
    form.value = { title: res.data.title, content: res.data.content }
  }
})

async function submit() {
  submitting.value = true
  try {
    if (isEdit.value) {
      await noticeApi.update(route.params.id, form.value.title, form.value.content)
      toastStore.show('수정되었습니다.')
      router.push(`/notices/${route.params.id}`)
    } else {
      const res = await noticeApi.create(form.value.title, form.value.content)
      toastStore.show('등록되었습니다.')
      router.push(`/notices/${res.data.id}`)
    }
  } catch {
    toastStore.show('처리 중 오류가 발생했습니다.', 'danger')
  } finally {
    submitting.value = false
  }
}
</script>
