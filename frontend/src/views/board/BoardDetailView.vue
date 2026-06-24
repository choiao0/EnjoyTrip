<template>
  <section class="py-5 container" style="max-width:860px;">
    <!-- 게시글 -->
    <div v-if="post" class="card border-0 shadow-sm rounded-4 mb-4">
      <div class="card-body p-4 p-md-5">
        <div class="d-flex justify-content-between align-items-start mb-3">
          <div>
            <h2 class="fw-bold mb-1">{{ post.title }}</h2>
            <span class="text-muted small">{{ post.authorName }} · {{ post.createdAt }}</span>
          </div>
          <div v-if="authStore.user?.id === post.authorId" class="d-flex gap-2 flex-shrink-0 ms-3">
            <router-link :to="`/boards/${post.id}/edit`" class="btn btn-sm btn-outline-secondary">수정</router-link>
            <button class="btn btn-sm btn-outline-danger" @click="deletePost">삭제</button>
          </div>
        </div>
        <hr>
        <div class="content-pre mt-3">{{ post.content }}</div>
        <div class="mt-4">
          <router-link class="btn btn-outline-secondary btn-sm" to="/boards">← 목록</router-link>
        </div>
      </div>
    </div>
    <div v-else class="text-center p-5 text-muted">
      <div class="spinner-border spinner-border-sm"></div>
    </div>

    <!-- 댓글 -->
    <div v-if="post" class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        <h6 class="fw-bold mb-3">댓글 {{ comments.length }}개</h6>

        <!-- 댓글 목록 -->
        <div v-if="comments.length === 0" class="text-muted small py-2 mb-3">
          아직 댓글이 없습니다. 첫 댓글을 남겨보세요.
        </div>
        <div v-else class="d-flex flex-column gap-3 mb-4">
          <div
            v-for="c in comments"
            :key="c.id"
            class="d-flex gap-3"
          >
            <!-- 아바타 -->
            <img
              :src="`https://api.dicebear.com/9.x/thumbs/svg?seed=${c.authorId}`"
              class="flex-shrink-0 rounded-circle"
              style="width:36px;height:36px;background:#f0f0f0;"
              :alt="c.authorName"
            />

            <!-- 내용 -->
            <div class="flex-grow-1">
              <div class="d-flex justify-content-between align-items-center mb-1">
                <div class="d-flex align-items-center gap-2">
                  <span class="fw-semibold small">{{ c.authorName }}</span>
                  <span
                    v-if="post && c.authorId === post.authorId"
                    class="badge text-bg-primary"
                    style="font-size:0.65rem;"
                  >작성자</span>
                </div>
                <div class="d-flex align-items-center gap-2">
                  <span class="text-muted" style="font-size:0.75rem;">{{ c.createdAt }}</span>
                  <button
                    v-if="authStore.user?.id === c.authorId"
                    class="btn btn-link btn-sm p-0 text-muted"
                    style="font-size:0.75rem;"
                    @click="deleteComment(c.id)"
                  >삭제</button>
                </div>
              </div>
              <p class="mb-0 small" style="white-space:pre-wrap;">{{ c.content }}</p>
            </div>
          </div>
        </div>

        <hr>

        <!-- 댓글 작성 -->
        <div v-if="authStore.user" class="mt-3">
          <label class="form-label small fw-semibold">댓글 작성</label>
          <textarea
            v-model="newComment"
            class="form-control mb-2"
            rows="3"
            placeholder="댓글을 입력하세요."
          />
          <div class="d-flex justify-content-end">
            <button
              class="btn btn-indigo btn-sm px-4"
              :disabled="!newComment.trim() || submittingComment"
              @click="submitComment"
            >{{ submittingComment ? '등록 중...' : '등록' }}</button>
          </div>
        </div>
        <div v-else class="mt-3 text-muted small text-center py-2 border rounded-3 bg-light">
          댓글을 작성하려면 <router-link to="/auth/login">로그인</router-link>이 필요합니다.
        </div>
      </div>
    </div>
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
const comments = ref([])
const newComment = ref('')
const submittingComment = ref(false)

onMounted(async () => {
  const [postRes, commentRes] = await Promise.allSettled([
    boardApi.detail(route.params.id),
    boardApi.comments(route.params.id)
  ])
  if (postRes.status === 'fulfilled') post.value = postRes.value.data
  if (commentRes.status === 'fulfilled') comments.value = commentRes.value.data
})

async function deletePost() {
  if (!confirm('게시글을 삭제하시겠습니까?')) return
  try {
    await boardApi.delete(route.params.id)
    toastStore.show('삭제됐습니다.')
    router.push('/boards')
  } catch {
    toastStore.show('삭제 중 오류가 발생했습니다.', 'danger')
  }
}

async function submitComment() {
  if (!newComment.value.trim()) return
  submittingComment.value = true
  try {
    const res = await boardApi.addComment(route.params.id, newComment.value)
    comments.value.push(res.data)
    newComment.value = ''
  } catch (e) {
    toastStore.show(e.response?.data?.error || '댓글 등록 중 오류가 발생했습니다.', 'danger')
  } finally {
    submittingComment.value = false
  }
}

async function deleteComment(commentId) {
  if (!confirm('댓글을 삭제하시겠습니까?')) return
  try {
    await boardApi.deleteComment(route.params.id, commentId)
    comments.value = comments.value.filter(c => c.id !== commentId)
    toastStore.show('댓글이 삭제됐습니다.')
  } catch {
    toastStore.show('댓글 삭제 중 오류가 발생했습니다.', 'danger')
  }
}
</script>
