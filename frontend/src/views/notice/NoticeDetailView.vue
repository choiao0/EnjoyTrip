<template>
  <section class="py-5 container">
    <div v-if="notice" class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <h2 class="fw-bold mb-2">{{ notice.title }}</h2>
        <div class="small text-muted mb-4">{{ notice.authorName }} · {{ notice.createdAt }}</div>
        <div class="content-pre">{{ notice.content }}</div>
        <div class="d-flex gap-2 mt-4">
          <router-link class="btn btn-outline-secondary" to="/notices">목록</router-link>
          <template v-if="authStore.user?.role === 'ADMIN'">
            <router-link :to="`/notices/${notice.id}/edit`" class="btn btn-outline-dark">수정</router-link>
            <button class="btn btn-outline-danger" @click="deleteNotice">삭제</button>
          </template>
        </div>
      </div>
    </div>
    <div v-else class="text-center p-5 text-muted">공지사항을 불러오는 중...</div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { noticeApi } from '../../api/index.js'
import { useAuthStore } from '../../stores/auth.js'
import { useToastStore } from '../../stores/toast.js'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()
const notice = ref(null)

onMounted(async () => {
  const res = await noticeApi.detail(route.params.id)
  notice.value = res.data
})

async function deleteNotice() {
  if (!confirm('삭제하시겠습니까?')) return
  try {
    await noticeApi.delete(route.params.id)
    toastStore.show('삭제되었습니다.')
    router.push('/notices')
  } catch {
    toastStore.show('삭제 중 오류가 발생했습니다.', 'danger')
  }
}
</script>
