<template>
  <section class="py-5 container">
    <div v-if="post" class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <h2 class="fw-bold mb-2">{{ post.title }}</h2>
        <div class="small text-muted mb-4">{{ post.authorName }} · {{ post.createdAt }}</div>
        <div class="content-pre">{{ post.content }}</div>
        <div class="d-flex gap-2 mt-4">
          <router-link class="btn btn-outline-secondary" to="/boards">목록</router-link>
          <template v-if="authStore.user?.id === post.authorId">
            <router-link :to="`/boards/${post.id}/edit`" class="btn btn-outline-dark">수정</router-link>
            <button class="btn btn-outline-danger" @click="deletePost">삭제</button>
          </template>
        </div>
      </div>
    </div>
    <div v-else class="text-center p-5 text-muted">게시물을 불러오는 중...</div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { boardApi } from '../../api/index.js'
import { useAuthStore } from '../../stores/auth.js'
import { useToastStore } from '../../stores/toast.js'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()
const post = ref(null)

onMounted(async () => {
  const res = await boardApi.detail(route.params.id)
  post.value = res.data
})

async function deletePost() {
  if (!confirm('삭제하시겠습니까?')) return
  try {
    await boardApi.delete(route.params.id)
    toastStore.show('삭제되었습니다.')
    router.push('/boards')
  } catch {
    toastStore.show('삭제 중 오류가 발생했습니다.', 'danger')
  }
}
</script>
