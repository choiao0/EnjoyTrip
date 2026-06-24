<template>
  <section class="py-5 container" style="max-width:860px;">
    <!-- 헤더 -->
    <div class="d-flex justify-content-between align-items-end mb-4">
      <div>
        <h2 class="fw-bold mb-1">공지사항</h2>
        <p class="text-muted small mb-0">EnjoyTrip의 주요 공지를 확인하세요.</p>
      </div>
      <router-link v-if="authStore.user?.role === 'ADMIN'" class="btn btn-indigo btn-sm" to="/notices/new">
        + 공지 작성
      </router-link>
    </div>

    <!-- 공지 목록 -->
    <div class="bg-white border rounded-4 shadow-sm overflow-hidden">
      <div v-if="loading" class="text-center text-muted py-5">
        <div class="spinner-border spinner-border-sm"></div>
      </div>
      <div v-else-if="notices.length === 0" class="text-center text-muted py-5">
        등록된 공지사항이 없습니다.
      </div>
      <div v-else>
        <div
          v-for="(notice, i) in notices"
          :key="notice.id"
          class="notice-row d-flex align-items-center gap-3 px-4 py-3"
          :class="{ 'border-bottom': i < notices.length - 1 }"
          style="cursor:pointer;"
          @click="$router.push(`/notices/${notice.id}`)"
        >
          <span class="badge text-bg-danger flex-shrink-0" style="font-size:0.7rem;">공지</span>
          <span class="fw-semibold flex-grow-1 notice-title">{{ notice.title }}</span>
          <span class="text-muted small flex-shrink-0">{{ notice.createdAt?.slice(0, 10) }}</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { noticeApi } from '../../api/index.js'
import { useAuthStore } from '../../stores/auth.js'

const authStore = useAuthStore()
const notices = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await noticeApi.list()
    notices.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.notice-row:hover { background: #fafafa; }
.notice-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
