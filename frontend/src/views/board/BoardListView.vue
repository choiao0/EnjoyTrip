<template>
  <section class="py-5 container">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h2 class="fw-bold mb-1">여행정보공유</h2>
        <p class="text-muted mb-0">총 {{ posts.length }}개의 게시글</p>
      </div>
      <router-link v-if="authStore.user" class="btn btn-indigo" to="/boards/new">글쓰기</router-link>
    </div>

    <div class="bg-white border rounded-4 shadow-sm overflow-hidden">
      <!-- 헤더 -->
      <div class="board-header d-none d-md-flex px-4 py-2 border-bottom bg-light text-muted small fw-semibold">
        <span class="board-col-num">번호</span>
        <span class="board-col-title">제목</span>
        <span class="board-col-author">작성자</span>
        <span class="board-col-date">날짜</span>
      </div>

      <!-- 게시글 없음 -->
      <div v-if="loading" class="text-center text-muted py-5">
        <div class="spinner-border spinner-border-sm"></div>
      </div>
      <div v-else-if="posts.length === 0" class="text-center text-muted py-5">
        등록된 게시글이 없습니다.
      </div>

      <!-- 게시글 목록 -->
      <div v-else>
        <div
          v-for="(post, i) in posts"
          :key="post.id"
          class="board-row d-flex align-items-center px-4 py-3 border-bottom"
          style="cursor:pointer;"
          @click="$router.push(`/boards/${post.id}`)"
        >
          <span class="board-col-num text-muted small">{{ posts.length - i }}</span>
          <span class="board-col-title">
            <span class="fw-semibold">{{ post.title }}</span>
            <span v-if="post.commentCount > 0" class="badge bg-indigo ms-2" style="font-size:0.7rem;">
              {{ post.commentCount }}
            </span>
          </span>
          <span class="board-col-author text-muted small">{{ post.authorName }}</span>
          <span class="board-col-date text-muted small">{{ post.createdAt?.slice(0, 10) }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { boardApi } from '../../api/index.js'
import { useAuthStore } from '../../stores/auth.js'

const authStore = useAuthStore()
const posts = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await boardApi.list()
    posts.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.board-header, .board-row {
  display: flex;
  align-items: center;
  gap: 0;
}
.board-col-num   { width: 60px;  flex-shrink: 0; text-align: center; }
.board-col-title { flex: 1;      min-width: 0; }
.board-col-author{ width: 90px;  flex-shrink: 0; text-align: center; }
.board-col-date  { width: 100px; flex-shrink: 0; text-align: center; }
.board-row:hover { background: #f8f9ff; }
.board-row:last-child { border-bottom: none !important; }
.badge.bg-indigo { background-color: #4f46e5 !important; }
</style>
