<template>
  <section class="py-5 container">
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-indigo" role="status"></div>
    </div>

    <template v-else-if="store.currentGroup">
      <!-- 헤더 -->
      <div class="d-flex justify-content-between align-items-start mb-4">
        <div>
          <router-link to="/trips" class="btn btn-sm btn-outline-secondary mb-2">← 여행 목록</router-link>
          <h2 class="fw-bold mt-1 mb-0">{{ store.currentGroup.title }}</h2>
          <p class="text-muted mb-1">{{ store.currentGroup.description }}</p>
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
        <!-- 왼쪽: 장소 목록 + 지도 -->
        <div class="col-lg-8">
          <div class="card shadow-sm p-3 mb-3">
            <div class="d-flex justify-content-between align-items-center mb-2">
              <h6 class="fw-bold mb-0">장소 목록 ({{ store.places.length }}개)</h6>
              <router-link
                :to="{ path: '/attractions', query: { groupId: groupId, tripTitle: store.currentGroup.title } }"
                class="btn btn-sm btn-outline-secondary"
              >+ 관광지 추가</router-link>
            </div>
            <div v-if="store.places.length === 0" class="text-center text-muted py-3 small">
              아직 추가된 장소가 없습니다.
              <router-link :to="{ path: '/attractions', query: { groupId: groupId, tripTitle: store.currentGroup.title } }">관광정보 검색</router-link>에서 이 그룹을 선택해 추가하세요.
            </div>
            <div v-else class="d-flex flex-column gap-2">
              <div
                v-for="p in store.places"
                :key="p.id"
                class="d-flex justify-content-between align-items-center border rounded-3 px-3 py-2"
              >
                <div>
                  <span class="fw-semibold">{{ p.title }}</span>
                  <small class="text-muted ms-2">{{ p.addedByName }}</small>
                </div>
                <button
                  v-if="store.isMember"
                  class="btn btn-outline-danger btn-sm py-0"
                  @click="handleRemovePlace(p.id)"
                >삭제</button>
              </div>
            </div>
          </div>

          <div class="card shadow-sm p-3">
            <h6 class="fw-bold mb-2">지도</h6>
            <div ref="mapEl" class="rounded-3 border" style="height:360px;"></div>
          </div>
        </div>

        <!-- 오른쪽: 그룹 정보 -->
        <div class="col-lg-4">
          <!-- 초대 코드 -->
          <div class="card shadow-sm p-3 mb-3">
            <h6 class="fw-bold mb-2">그룹 초대</h6>
            <p class="small text-muted mb-1">초대 코드를 공유하면 누구나 참가할 수 있습니다.</p>
            <div class="text-center py-2 mb-2 rounded-3 bg-light border">
              <span class="fw-bold fs-4 letter-spacing-wide">
                {{ store.currentGroup.inviteCode || '코드 없음' }}
              </span>
            </div>
            <div class="d-flex gap-2">
              <button class="btn btn-outline-secondary btn-sm flex-grow-1" @click="copyCode">
                {{ codeCopied ? '복사됨!' : '코드 복사' }}
              </button>
              <button class="btn btn-outline-secondary btn-sm flex-grow-1" @click="copyLink">
                {{ copied ? '복사됨!' : '링크 복사' }}
              </button>
            </div>
          </div>

          <!-- 멤버 목록 -->
          <div class="card shadow-sm">
            <div class="card-header fw-bold py-2">멤버 ({{ store.members.length }})</div>
            <ul class="list-group list-group-flush">
              <li
                v-for="m in store.members"
                :key="m.userId"
                class="list-group-item d-flex justify-content-between align-items-center py-2"
              >
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
      </div>
    </template>

    <div v-else class="text-center text-muted py-5">그룹을 찾을 수 없습니다.</div>
  </section>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGroupTripStore } from '../../stores/groupTrip.js'
import { useAuthStore } from '../../stores/auth.js'
import { useToastStore } from '../../stores/toast.js'
import { useGroupWebSocket } from '../../composables/useGroupWebSocket.js'

const route = useRoute()
const router = useRouter()
const store = useGroupTripStore()
const authStore = useAuthStore()
const toastStore = useToastStore()

const loading = ref(true)
const copied = ref(false)
const codeCopied = ref(false)
const mapEl = ref(null)
let groupMap = null

const groupId = Number(route.params.id)

const { connected, connect } = useGroupWebSocket(groupId, (event) => {
  store.handleWsEvent(event)
  nextTick(() => updateMapMarkers())
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
    toastStore.show(e.response?.data?.error || '그룹 정보를 불러오지 못했습니다.', 'danger')
  } finally {
    loading.value = false
  }
  await nextTick()
  initMap()
})

function initMap() {
  if (!window.kakao || !mapEl.value) return
  groupMap = new window.kakao.maps.Map(mapEl.value, {
    center: new window.kakao.maps.LatLng(36.5, 127.8),
    level: 10
  })
  updateMapMarkers()
}

function updateMapMarkers() {
  if (!groupMap || !store.places.length) return
  const bounds = new window.kakao.maps.LatLngBounds()
  store.places.forEach((place, i) => {
    if (!place.lat || !place.lng) return
    const pos = new window.kakao.maps.LatLng(place.lat, place.lng)
    bounds.extend(pos)
    const marker = new window.kakao.maps.Marker({ map: groupMap, position: pos })
    const info = new window.kakao.maps.InfoWindow({
      content: `<div style="padding:6px 8px;font-size:12px;"><strong>${i + 1}. ${place.title}</strong></div>`
    })
    info.open(groupMap, marker)
  })
  groupMap.setBounds(bounds)
}

async function copyCode() {
  const code = store.currentGroup?.inviteCode
  if (!code) {
    toastStore.show('초대 코드를 불러오는 중입니다. 잠시 후 다시 시도하세요.', 'warning')
    return
  }
  await navigator.clipboard.writeText(code)
  codeCopied.value = true
  toastStore.show(`초대 코드 ${code} 복사됨!`)
  setTimeout(() => { codeCopied.value = false }, 2000)
}

async function copyLink() {
  await navigator.clipboard.writeText(window.location.href)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

async function handleRemovePlace(placeId) {
  try {
    await store.removePlace(groupId, placeId)
    toastStore.show('장소를 삭제했습니다.')
    updateMapMarkers()
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
    router.push('/trips')
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
    toastStore.show('그룹이 삭제됐습니다.')
    router.push('/trips')
  } catch (e) {
    toastStore.show(e.response?.data?.error || '삭제에 실패했습니다.', 'danger')
  }
}
</script>

<style scoped>
.letter-spacing-wide {
  letter-spacing: 0.35em;
}
</style>
