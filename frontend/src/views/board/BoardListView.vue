<template>
  <section class="py-5 container">
    <div class="d-flex justify-content-between mb-4">
      <h2 class="fw-bold">커뮤니티</h2>
      <router-link v-if="authStore.user" class="btn btn-dark" to="/boards/new">글쓰기</router-link>
    </div>
    <table class="table table-hover border rounded-4 overflow-hidden shadow-sm bg-white">
      <thead class="table-light">
        <tr><th>번호</th><th>제목</th><th>작성자</th><th>날짜</th></tr>
      </thead>
      <tbody>
        <tr v-if="posts.length === 0">
          <td colspan="4" class="text-center text-muted py-4">등록된 게시물이 없습니다.</td>
        </tr>
        <tr
          v-for="(post, i) in posts"
          :key="post.id"
          style="cursor:pointer;"
          @click="$router.push(`/boards/${post.id}`)"
        >
          <td>{{ i + 1 }}</td>
          <td>{{ post.title }}</td>
          <td>{{ post.authorName }}</td>
          <td>{{ post.createdAt }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { boardApi } from '../../api/index.js'
import { useAuthStore } from '../../stores/auth.js'

const authStore = useAuthStore()
const posts = ref([])
onMounted(async () => {
  const res = await boardApi.list()
  posts.value = res.data
})
</script>
