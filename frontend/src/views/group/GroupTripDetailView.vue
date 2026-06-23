<template>
  <section class="py-5 container">
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-indigo" role="status"></div>
    </div>

    <template v-else-if="store.currentGroup">
      <!-- 헤더 -->
      <div class="d-flex justify-content-between align-items-start mb-4">
        <div>
          <router-link to="/groups" class="text-muted small">← 그룹 목록</router-link>
          <h2 class="fw-bold mt-1 mb-0">{{ store.currentGroup.title }}</h2>
          <p class="text-muted mb-1">{{ store.currentGroup.description }}</p>
          <div class="d-flex align-items-center gap-2">
            <span class="text-muted small">그룹 ID: <strong>{{ groupId }}</strong></span>
            <button class="btn btn-outline-secondary btn-sm py-0" @click="copyLink">
              {{ copied ? '복사됨!' : '링크 복사' }}
            </button>
          </div>
        </div>
        <div class="d-flex gap-2 align-items-center flex-wrap justify-content-end">
          <span :class="['badge', connected ? 'bg-success' : 'bg-secondary']">
            {{ connected ? '실시간 연결됨' : '연결 중...' }}
          </span>
          <button v-if="!store.isMember" class="btn btn-indigo btn-sm" @click="handleJoin">참가하기</button>
          <button v-else-if="!store.isHost" class="btn btn-outline-danger btn-sm" @click="handleLeave">나가기</button>
          <button v-if="store.isHost" class="btn btn-danger btn-sm" @click="handleDelete">그룹 삭제</button>
        </div>
      </div>

      <div class="row g-4">
        <!-- 멤버 목록 -->
        <div class="col-lg-4">
          <div class="card shadow-sm h-100">
            <div class="card-header fw-bold">멤버 ({{ store.members.length }})</div>
            <ul class="list-group list-group-flush">
              <li v-for="m in store.members" :key="m.userId" class="list-group-item d-flex justify-content-between align-items-center">
                <span>
                  {{ m.userName }}
                  <span v-if="m.userId === store.currentGroup.hostUserId" class="badge bg-warning text-dark ms-1">그룹장</span>
                </span>
                <button
                  v-if="store.isHost && m.userId !== store.currentGroup.hostUserId"
                  class="btn btn-outline-danger btn-sm py-0"
                  @click="handleKick(m.userId)"
                >추방</button>
              </li>
            </ul>
          </div>
        </div>

        <!-- 장소 목록 + 추가 -->
        <div class="col-lg-8">
          <!-- 장소 추가 (멤버만) -->
          <div v-if="store.isMember" class="card shadow-sm mb-4 p-3">
            <h6 class="fw-bold mb-2">장소 검색 후 추가</h6>
            <div class="input-group mb-2">
              <input v-model="searchKeyword" class="form-control" placeholder="관광지 이름 검색" @keyup.enter="searchAttractions" />
              <button class="btn btn-outline-secondary" @click="searchAttractions">검색</button>
            </div>
            <ul v-if="searchResults.length" class="list-group">
              <li
                v-for="a in searchResults"
                :key="a.contentId"
                class="list-group-item d-flex justify-content-between align-items-center"
              >
                <div>
                  <div class="fw-semibold">{{ a.title }}</div>
                  <small class="text-muted">{{ a.address }}</small>
                </div>
                <button class="btn btn-sm btn-indigo" @click="handleAddPlace(a)">추가</button>
              </li>
            </ul>
          </div>

          <!-- 장소 카드 -->
          <div class="card shadow-sm">
            <div class="card-header fw-bold">장소 목록 ({{ store.places.length }})</div>
            <ul class="list-group list-group-flush">
              <li v-if="store.places.length === 0" class="list-group-item text-center text-muted py-3">
                아직 추가된 장소가 없습니다.
              </li>
              <li
                v-for="p in store.places"
                :key="p.id"
                class="list-group-item d-flex justify-content-between align-items-center"
              >
                <div>
                  <div class="fw-semibold">{{ p.title }}</div>
                  <small class="text-muted">추가: {{ p.addedByName }} · {{ p.addedAt }}</small>
                </div>
                <button
                  v-if="store.isMember"
                  class="btn btn-outline-danger btn-sm py-0"
                  @click="handleRemovePlace(p.id)"
                >삭제</button>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="text-center text-muted py-5">그룹을 찾을 수 없습니다.</div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGroupTripStore } from '../../stores/groupTrip.js'
import { useAuthStore } from '../../stores/auth.js'
import { useToastStore } from '../../stores/toast.js'
import { useGroupWebSocket } from '../../composables/useGroupWebSocket.js'
import { attractionApi } from '../../api/index.js'

const route = useRoute()
const router = useRouter()
const store = useGroupTripStore()
const authStore = useAuthStore()
const toastStore = useToastStore()

const loading = ref(true)
const copied = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])

const groupId = Number(route.params.id)

const { connected, connect } = useGroupWebSocket(groupId, (event) => {
  store.handleWsEvent(event)
})

onMounted(async () => {
  if (!authStore.user) {
    router.push('/auth/login')
    return
  }
  try {
    await store.fetchDetail(groupId)
    connect()
  } catch (e) {
    console.error('[GroupTripDetail] fetchDetail 실패:', e)
    toastStore.show(
      e.response?.data?.error || e.message || '그룹 정보를 불러오지 못했습니다.',
      'danger'
    )
  } finally {
    loading.value = false
  }
})

async function copyLink() {
  await navigator.clipboard.writeText(window.location.href)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

async function searchAttractions() {
  if (!searchKeyword.value.trim()) return
  try {
    const res = await attractionApi.search({ keyword: searchKeyword.value })
    searchResults.value = res.data
  } catch {
    toastStore.show('검색에 실패했습니다.', 'danger')
  }
}

async function handleAddPlace(attraction) {
  try {
    await store.addPlace(groupId, {
      contentId: attraction.contentId,
      title: attraction.title,
      lat: attraction.lat,
      lng: attraction.lng
    })
    toastStore.show(`"${attraction.title}"을(를) 추가했습니다.`)
  } catch (e) {
    toastStore.show(e.response?.data?.error || '장소 추가에 실패했습니다.', 'danger')
  }
}

async function handleRemovePlace(placeId) {
  try {
    await store.removePlace(groupId, placeId)
    toastStore.show('장소를 삭제했습니다.')
  } catch (e) {
    toastStore.show(e.response?.data?.error || '장소 삭제에 실패했습니다.', 'danger')
  }
}

async function handleJoin() {
  try {
    await store.joinGroup(groupId)
    toastStore.show('그룹에 참가했습니다.')
  } catch (e) {
    toastStore.show(e.response?.data?.error || '참가에 실패했습니다.', 'danger')
  }
}

async function handleLeave() {
  try {
    await store.leaveGroup(groupId)
    toastStore.show('그룹에서 나왔습니다.')
    router.push('/groups')
  } catch (e) {
    toastStore.show(e.response?.data?.error || '탈퇴에 실패했습니다.', 'danger')
  }
}

async function handleKick(userId) {
  try {
    await store.kickMember(groupId, userId)
    toastStore.show('멤버를 추방했습니다.')
  } catch (e) {
    toastStore.show(e.response?.data?.error || '추방에 실패했습니다.', 'danger')
  }
}

async function handleDelete() {
  if (!confirm('그룹을 삭제하시겠습니까? 장소와 멤버 정보가 모두 삭제됩니다.')) return
  try {
    await store.deleteGroup(groupId)
    toastStore.show('그룹이 삭제되었습니다.')
    router.push('/groups')
  } catch (e) {
    toastStore.show(e.response?.data?.error || '삭제에 실패했습니다.', 'danger')
  }
}
</script>
