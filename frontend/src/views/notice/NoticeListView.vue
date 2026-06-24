<template>
  <section class="py-5 container">
    <div class="d-flex justify-content-between mb-4">
      <h2 class="fw-bold">공지사항</h2>
      <router-link v-if="authStore.user?.role === 'ADMIN'" class="btn btn-dark" to="/notices/new">글쓰기</router-link>
    </div>
    <table class="table table-hover border rounded-4 overflow-hidden shadow-sm bg-white">
      <thead class="table-light">
        <tr><th>번호</th><th>제목</th><th>작성자</th><th>날짜</th></tr>
      </thead>
      <tbody>
        <tr v-if="notices.length === 0">
          <td colspan="4" class="text-center text-muted py-4">등록된 공지사항이 없습니다.</td>
        </tr>
        <tr
          v-for="(notice, i) in notices"
          :key="notice.id"
          style="cursor:pointer;"
          @click="$router.push(`/notices/${notice.id}`)"
        >
          <td>{{ i + 1 }}</td>
          <td>{{ notice.title }}</td>
          <td>{{ notice.authorName }}</td>
          <td>{{ notice.createdAt }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { noticeApi } from '../../api/index.js'
import { useAuthStore } from '../../stores/auth.js'

const authStore = useAuthStore()
const notices = ref([])
onMounted(async () => {
  const res = await noticeApi.list()
  notices.value = res.data
})
</script>
