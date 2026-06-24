<template>
  <section class="py-5 container" style="max-width:860px;">
    <div v-if="notice">
      <div class="bg-white border rounded-4 shadow-sm overflow-hidden">
        <!-- 공지 헤더 -->
        <div class="notice-header px-5 py-4 border-bottom">
          <span class="badge text-bg-danger mb-3" style="font-size:0.75rem;">공지</span>
          <h2 class="fw-bold mb-2">{{ notice.title }}</h2>
          <div class="d-flex align-items-center gap-3 text-muted small">
            <span><i class="bi bi-person me-1"></i>{{ notice.authorName }}</span>
            <span><i class="bi bi-calendar3 me-1"></i>{{ notice.createdAt }}</span>
          </div>
        </div>

        <!-- 본문 -->
        <div class="px-5 py-4 content-pre notice-body">{{ notice.content }}</div>

        <!-- 하단 버튼 -->
        <div class="px-5 py-3 border-top bg-light d-flex justify-content-between align-items-center">
          <router-link class="btn btn-outline-secondary btn-sm" to="/notices">← 목록</router-link>
          <div v-if="authStore.user?.role === 'ADMIN'" class="d-flex gap-2">
            <router-link :to="`/notices/${notice.id}/edit`" class="btn btn-outline-secondary btn-sm">수정</router-link>
            <button class="btn btn-outline-danger btn-sm" @click="deleteNotice">삭제</button>
          </div>
        </div>
      </div>

      <!-- 이전/다음 공지 -->
      <div v-if="prevNotice || nextNotice" class="bg-white border rounded-4 shadow-sm mt-3 overflow-hidden">
        <div v-if="prevNotice"
          class="d-flex align-items-center gap-3 px-4 py-3 nav-notice"
          :class="{ 'border-bottom': nextNotice }"
          style="cursor:pointer;"
          @click="$router.push(`/notices/${prevNotice.id}`)"
        >
          <span class="text-muted small flex-shrink-0" style="width:40px;">이전</span>
          <span class="flex-grow-1 small fw-semibold">{{ prevNotice.title }}</span>
          <span class="text-muted small">{{ prevNotice.createdAt?.slice(0, 10) }}</span>
        </div>
        <div v-if="nextNotice"
          class="d-flex align-items-center gap-3 px-4 py-3 nav-notice"
          style="cursor:pointer;"
          @click="$router.push(`/notices/${nextNotice.id}`)"
        >
          <span class="text-muted small flex-shrink-0" style="width:40px;">다음</span>
          <span class="flex-grow-1 small fw-semibold">{{ nextNotice.title }}</span>
          <span class="text-muted small">{{ nextNotice.createdAt?.slice(0, 10) }}</span>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-5 text-muted">
      <div class="spinner-border spinner-border-sm"></div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { noticeApi } from '../../api/index.js'
import { useAuthStore } from '../../stores/auth.js'
import { useToastStore } from '../../stores/toast.js'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

const notice = ref(null)
const allNotices = ref([])

const currentIndex = computed(() =>
  allNotices.value.findIndex(n => n.id === route.params.id)
)
const prevNotice = computed(() =>
  currentIndex.value > 0 ? allNotices.value[currentIndex.value - 1] : null
)
const nextNotice = computed(() =>
  currentIndex.value < allNotices.value.length - 1 ? allNotices.value[currentIndex.value + 1] : null
)

async function loadNotice(id) {
  notice.value = null
  const res = await noticeApi.detail(id)
  notice.value = res.data
}

onMounted(async () => {
  const [detailRes, listRes] = await Promise.allSettled([
    noticeApi.detail(route.params.id),
    noticeApi.list()
  ])
  if (detailRes.status === 'fulfilled') notice.value = detailRes.value.data
  if (listRes.status === 'fulfilled') allNotices.value = listRes.value.data
})

watch(() => route.params.id, (newId) => {
  if (newId) loadNotice(newId)
})

async function deleteNotice() {
  if (!confirm('공지사항을 삭제하시겠습니까?')) return
  try {
    await noticeApi.delete(route.params.id)
    toastStore.show('삭제됐습니다.')
    router.push('/notices')
  } catch {
    toastStore.show('삭제 중 오류가 발생했습니다.', 'danger')
  }
}
</script>

<style scoped>
.notice-header { background: #fafbff; }
.notice-body { line-height: 1.9; min-height: 200px; }
.nav-notice:hover { background: #f8f9ff; }
</style>
