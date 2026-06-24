<template>
  <section class="py-5 container">
    <div class="d-flex justify-content-between mb-4">
      <h2 class="fw-bold">그룹 여행</h2>
      <button class="btn btn-dark" @click="showForm = !showForm">새 그룹 만들기</button>
    </div>

    <!-- 그룹 생성 폼 -->
    <div v-if="showForm" class="card shadow-sm mb-4 p-4">
      <h5 class="fw-bold mb-3">그룹 만들기</h5>
      <div class="mb-3">
        <input v-model="newTitle" class="form-control" placeholder="그룹 이름" />
      </div>
      <div class="mb-3">
        <textarea v-model="newDesc" class="form-control" rows="2" placeholder="설명 (선택)" />
      </div>
      <div class="d-flex gap-2">
        <button class="btn btn-indigo" @click="handleCreate">만들기</button>
        <button class="btn btn-outline-secondary" @click="showForm = false">취소</button>
      </div>
    </div>

    <!-- 그룹 ID로 참여 -->
    <div v-if="authStore.user" class="card shadow-sm mb-4 p-4">
      <h5 class="fw-bold mb-2">그룹 ID로 참여하기</h5>
      <p class="text-muted small mb-3">그룹장이 공유한 그룹 ID를 입력하면 해당 그룹 페이지로 이동합니다.</p>
      <div class="input-group" style="max-width:320px;">
        <input
          v-model="joinId"
          class="form-control"
          type="number"
          placeholder="그룹 ID 입력"
          @keyup.enter="goToGroup"
        />
        <button class="btn btn-outline-secondary" @click="goToGroup">이동</button>
      </div>
    </div>

    <!-- 로그인 안내 -->
    <div v-if="!authStore.user" class="text-center text-muted py-5">
      <p>그룹 여행 기능을 사용하려면 <router-link to="/auth/login">로그인</router-link>이 필요합니다.</p>
    </div>

    <!-- 그룹 목록 -->
    <template v-else>
      <div v-if="store.groups.length === 0" class="text-center text-muted py-5">
        참여 중인 그룹이 없습니다. 새 그룹을 만들어보세요.
      </div>
      <div class="row g-3">
        <div v-for="group in store.groups" :key="group.id" class="col-md-6 col-lg-4">
          <div class="card h-100 shadow-sm" style="cursor:pointer;" @click="router.push(`/groups/${group.id}`)">
            <div class="card-body">
              <h5 class="card-title fw-bold">{{ group.title }}</h5>
              <p class="card-text text-muted small">{{ group.description || '설명 없음' }}</p>
              <p class="card-text small">
                <span class="badge bg-secondary me-1">그룹장: {{ group.hostUserName }}</span>
              </p>
              <p class="text-muted small mb-0">{{ group.createdAt }}</p>
            </div>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGroupTripStore } from '../../stores/groupTrip.js'
import { useAuthStore } from '../../stores/auth.js'
import { useToastStore } from '../../stores/toast.js'

const router = useRouter()
const store = useGroupTripStore()
const authStore = useAuthStore()
const toastStore = useToastStore()

const showForm = ref(false)
const newTitle = ref('')
const newDesc = ref('')
const joinId = ref('')

onMounted(async () => {
  if (authStore.user) {
    try {
      await store.fetchGroups()
    } catch {
      toastStore.show('그룹 목록을 불러오지 못했습니다.', 'danger')
    }
  }
})

function goToGroup() {
  const id = parseInt(joinId.value)
  if (!id || id <= 0) {
    toastStore.show('유효한 그룹 ID를 입력하세요.', 'warning')
    return
  }
  router.push(`/groups/${id}`)
}

async function handleCreate() {
  if (!newTitle.value.trim()) {
    toastStore.show('그룹 이름을 입력하세요.', 'warning')
    return
  }
  try {
    await store.createGroup(newTitle.value, newDesc.value)
    newTitle.value = ''
    newDesc.value = ''
    showForm.value = false
    toastStore.show('그룹이 생성되었습니다.')
  } catch (e) {
    toastStore.show(e.response?.data?.error || '그룹 생성에 실패했습니다.', 'danger')
  }
}
</script>
