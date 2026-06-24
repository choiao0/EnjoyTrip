<template>
  <section class="py-4 container px-4 px-lg-5">
    <div class="d-flex align-items-center gap-2 mb-4">
      <h2 class="fw-bold mb-0">관광정보 조회</h2>
    </div>

    <!-- 수정 중인 여행 표시 바 -->
    <div v-if="currentContext" class="edit-context-bar mb-4 p-3 rounded-4 d-flex align-items-center gap-3 flex-wrap">
      <div class="edit-context-icon flex-shrink-0">
        <i class="bi bi-pencil-square"></i>
      </div>
      <div class="flex-grow-1">
        <div class="label-text text-uppercase fw-semibold mb-1">수정 중인 여행</div>
        <div class="fw-bold fs-6">{{ currentContext.title }}</div>
      </div>
      <div class="d-flex align-items-center gap-2 flex-shrink-0">
        <router-link v-if="returnPath" :to="returnPath" class="btn btn-sm btn-light border">
          ← 여행으로 돌아가기
        </router-link>
        <button class="btn btn-sm btn-outline-secondary" @click="showChangeTripModal = true">여행 변경</button>
      </div>
    </div>

    <div class="row g-4 align-items-start">
      <!-- 왼쪽: 지도 + 여행 장소 목록 -->
      <div class="col-lg-7">
        <div class="map-wrap position-relative mb-4">
          <div ref="mapEl" class="rounded-6 border shadow-sm w-100" style="min-height:500px;"></div>
        </div>

        <!-- 현재 여행 장소 목록 -->
        <div v-if="currentContext" class="border rounded-4 bg-white p-3 shadow-sm">
          <div class="d-flex align-items-center gap-2 mb-3">
            <h6 class="fw-bold mb-0">장소 목록</h6>
            <span class="text-muted small">({{ tripItems.length }}개)</span>

            <!-- 그룹: 실시간 연결 상태 -->
            <span
              v-if="currentContext._type === 'group'"
              :class="['badge ms-1', wsConnected ? 'bg-success' : 'bg-secondary']"
              style="font-size:0.65rem;"
            >{{ wsConnected ? '실시간 연결됨' : '연결 중...' }}</span>

            <!-- 개인: 저장 상태 -->
            <template v-if="currentContext._type === 'personal'">
              <span v-if="savingOrder" class="ms-auto small text-muted">저장 중...</span>
              <span v-else-if="lastSaved" class="ms-auto small text-success fw-semibold">✓ 저장됨</span>
            </template>
          </div>

          <div v-if="loadingItems" class="text-center py-3">
            <div class="spinner-border spinner-border-sm text-secondary"></div>
          </div>
          <div v-else-if="tripItems.length === 0" class="text-center text-muted py-3 small">
            추가된 장소가 없습니다. 오른쪽에서 관광지를 검색해 추가하세요.
          </div>

          <!-- 개인 여행: 드래그 가능 + 삭제 -->
          <div v-else-if="currentContext._type === 'personal'" class="d-flex flex-column gap-2">
            <div
              v-for="(item, idx) in tripItems"
              :key="item.contentId"
              :class="['d-flex align-items-center gap-2 border rounded-3 p-2 trip-item',
                       dragOverIdx === idx && dragStartIdx !== idx ? 'drag-over' : '']"
              draggable="true"
              @dragstart="onDragStart($event, idx)"
              @dragover="onDragOver($event, idx)"
              @drop="onDrop($event, idx)"
              @dragend="onDragEnd"
            >
              <span class="drag-handle text-muted flex-shrink-0">⠿</span>
              <span class="badge text-bg-primary flex-shrink-0">{{ idx + 1 }}</span>
              <span class="fw-semibold flex-grow-1">{{ item.title }}</span>
              <button
                class="btn btn-outline-danger btn-sm py-0 px-2 flex-shrink-0"
                @click.stop="deletePersonalItem(idx)"
              >✕</button>
            </div>
            <p class="text-muted small mt-1 mb-0">⠿ 드래그로 순서 변경 · ✕ 로 삭제</p>
          </div>

          <!-- 그룹 여행: addedByName + 삭제 -->
          <div v-else class="d-flex flex-column gap-2">
            <div
              v-for="(item, idx) in tripItems"
              :key="item.id"
              class="d-flex align-items-center gap-2 border rounded-3 p-2"
            >
              <span class="badge text-bg-success flex-shrink-0">{{ idx + 1 }}</span>
              <span class="fw-semibold flex-grow-1">{{ item.title }}</span>
              <span v-if="item.addedByName" class="text-muted small flex-shrink-0">{{ item.addedByName }}</span>
              <button
                class="btn btn-outline-danger btn-sm py-0 px-2 flex-shrink-0"
                @click="deleteGroupItem(item.id)"
              >✕</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 오른쪽: 검색 패널 + 결과 -->
      <div class="col-lg-5 d-flex flex-column" style="height:calc(100vh - 200px);overflow-y:auto;">
        <form class="p-4 border rounded-4 bg-white shadow-sm mb-4" @submit.prevent="doSearch">
          <label class="small fw-bold mb-2">시도 선택</label>
          <div v-if="sidosError" class="alert alert-danger py-2 small mb-3">{{ sidosError }}</div>
          <select v-model="form.sidoCode" class="form-select border-0 bg-light rounded-3 mb-3" @change="onSidoChange" :disabled="sidosLoading">
            <option value="">{{ sidosLoading ? '로딩 중...' : '전국' }}</option>
            <option v-for="s in sidos" :key="s.sidoCode" :value="s.sidoCode">{{ s.sidoName }}</option>
          </select>

          <label class="small fw-bold mb-2">구군 선택</label>
          <select v-model="form.gugunCode" class="form-select border-0 bg-light rounded-3 mb-3">
            <option value="">전체</option>
            <option v-for="g in guguns" :key="g.gugunCode" :value="g.gugunCode">{{ g.gugunName }}</option>
          </select>

          <label class="small fw-bold mb-2">콘텐츠 유형</label>
          <select v-model="form.contentTypeId" class="form-select border-0 bg-light rounded-3 mb-3">
            <option value="">전체</option>
            <option value="12">관광지</option>
            <option value="14">문화시설</option>
            <option value="15">행사/축제</option>
            <option value="25">여행코스</option>
            <option value="28">레포츠</option>
            <option value="32">숙박</option>
            <option value="38">쇼핑</option>
            <option value="39">음식점</option>
          </select>

          <label class="small fw-bold mb-2">키워드 검색</label>
          <input v-model="form.keyword" type="text" class="form-control mb-3" placeholder="경복궁, 카페, 축제 등">

          <button class="btn btn-indigo w-100 mt-2 py-2" type="submit" :disabled="loading">
            {{ loading ? '검색 중...' : '조회하기' }}
          </button>
        </form>

        <!-- 검색 결과 -->
        <div class="border rounded-4 bg-white p-2 shadow-sm">
          <div v-if="!searched" class="text-center text-muted p-4">조회할 관광정보를 검색해 주세요.</div>
          <div v-else-if="results.length === 0" class="text-center text-muted p-4">검색 결과가 없습니다.</div>
          <div v-else class="d-flex flex-column gap-3">
            <div
              v-for="item in results"
              :key="item.contentId"
              class="spot-card card border-0 shadow-sm rounded-4 overflow-hidden"
              role="button"
              style="cursor:pointer;"
              @click="openModal(item)"
            >
              <div class="row g-0">
                <div class="col-4">
                  <img
                    :src="item.imageUrl || '/assets/img/default-attraction.svg'"
                    :alt="item.title"
                    class="w-100 h-100"
                    style="object-fit:cover;min-height:120px;"
                    @error="e => e.target.src='/assets/img/default-attraction.svg'"
                  >
                </div>
                <div class="col-8">
                  <div class="card-body py-3">
                    <h6 class="fw-bold mb-1">{{ item.title }}</h6>
                    <p class="small text-muted mb-2">
                      <i class="bi bi-geo-alt me-1"></i>{{ item.address || '주소 정보 없음' }}
                    </p>
                    <span class="small text-primary">
                      <i class="bi bi-info-circle me-1"></i>클릭하여 상세보기
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- 여행 선택/추가 모달 -->
  <AddToTripModal
    :show="showAddModal"
    :attraction="selected"
    @close="showAddModal = false"
    @trip-selected="onTripSelected"
  />

  <!-- 여행 변경 모달 -->
  <AddToTripModal
    :show="showChangeTripModal"
    :attraction="null"
    @close="showChangeTripModal = false"
    @trip-selected="onTripSelected"
  />

  <!-- 관광지 상세 모달 -->
  <div class="modal fade" id="attractionModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
      <div class="modal-content rounded-4 border-0 shadow" v-if="selected">
        <div class="modal-header border-0 pb-0">
          <h5 class="modal-title fw-bold">{{ selected.title }}</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body pt-2">
          <img
            :src="selected.imageUrl || '/assets/img/default-attraction.svg'"
            :alt="selected.title"
            class="w-100 rounded-3 mb-3"
            style="max-height:280px;object-fit:cover;"
          >
          <p class="small text-muted mb-3">
            <i class="bi bi-geo-alt me-1"></i>{{ selected.address || '주소 정보 없음' }}
          </p>
          <hr class="my-2">
          <p class="small lh-lg mb-0">{{ selected.overview || '상세 설명이 없는 관광지입니다.' }}</p>
        </div>
        <div class="modal-footer border-0 pt-0">
          <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">닫기</button>
          <button type="button" class="btn btn-indigo btn-sm px-4" @click="openAddModal">
            <i class="bi bi-plus-lg me-1"></i>계획에 추가
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { attractionApi, planApi, groupTripApi } from '../api/index.js'
import { useAuthStore } from '../stores/auth.js'
import { useToastStore } from '../stores/toast.js'
import AddToTripModal from '../components/AddToTripModal.vue'

const route = useRoute()
const authStore = useAuthStore()
const toastStore = useToastStore()

const mapEl = ref(null)
const sidos = ref([])
const guguns = ref([])
const results = ref([])
const searched = ref(false)
const loading = ref(false)
const sidosLoading = ref(false)
const sidosError = ref('')
const selected = ref(null)
const showAddModal = ref(false)
const showChangeTripModal = ref(false)
let mapInstance = null
let modalInstance = null

const initPlanId = route.query.planId || null
const initGroupId = route.query.groupId ? Number(route.query.groupId) : null
const returnTitle = route.query.tripTitle || ''
const returnPath = initPlanId ? `/trips/${initPlanId}`
                 : initGroupId ? `/groups/${initGroupId}`
                 : null

const currentContext = ref(null)

// 장소 목록 상태
const tripItems = ref([])
const loadingItems = ref(false)
const savingOrder = ref(false)
const lastSaved = ref(false)
const dragStartIdx = ref(null)
const dragOverIdx = ref(null)

// 그룹 WebSocket
const wsConnected = ref(false)
let wsClient = null

const form = ref({
  sidoCode: '',
  gugunCode: '',
  contentTypeId: '',
  keyword: route.query.keyword || ''
})

// ── WebSocket ────────────────────────────────────────────────

function connectGroupWebSocket(groupId) {
  disconnectGroupWebSocket()
  wsClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    onConnect: () => {
      wsConnected.value = true
      wsClient.subscribe(`/topic/group/${groupId}`, (message) => {
        try { handleGroupWsEvent(JSON.parse(message.body)) } catch { /* ignore */ }
      })
    },
    onDisconnect: () => { wsConnected.value = false },
    onStompError: () => { wsConnected.value = false }
  })
  wsClient.activate()
}

function disconnectGroupWebSocket() {
  if (wsClient) {
    wsClient.deactivate()
    wsClient = null
    wsConnected.value = false
  }
}

function handleGroupWsEvent(event) {
  if (event.type === 'PLACE_ADDED') {
    const exists = tripItems.value.some(p => p.id === event.data.id)
    if (!exists) tripItems.value.push(event.data)
  } else if (event.type === 'PLACE_REMOVED') {
    tripItems.value = tripItems.value.filter(p => p.id !== event.data.placeId)
  }
}

// ── 생명주기 ────────────────────────────────────────────────

onMounted(async () => {
  if (initPlanId) {
    currentContext.value = { _type: 'personal', id: initPlanId, title: returnTitle }
  } else if (initGroupId) {
    currentContext.value = { _type: 'group', id: initGroupId, title: returnTitle }
    connectGroupWebSocket(initGroupId)
  }

  sidosLoading.value = true
  sidosError.value = ''
  try {
    const res = await attractionApi.sidos()
    sidos.value = res.data
  } catch {
    sidosError.value = '시도 목록을 불러오지 못했습니다. 서버 연결을 확인해 주세요.'
    toastStore.show(sidosError.value, 'danger')
  } finally {
    sidosLoading.value = false
  }

  await nextTick()
  initMap()
  if (form.value.keyword) doSearch()
  await loadTripItems()
})

onUnmounted(() => {
  disconnectGroupWebSocket()
})

// ── 장소 목록 로드 ────────────────────────────────────────

async function loadTripItems() {
  if (!currentContext.value) { tripItems.value = []; return }
  loadingItems.value = true
  try {
    if (currentContext.value._type === 'personal') {
      const res = await planApi.list()
      const plan = res.data.find(p => p.id === currentContext.value.id)
      tripItems.value = plan?.items || []
    } else {
      const res = await groupTripApi.detail(currentContext.value.id)
      tripItems.value = res.data.places || []
    }
  } catch {
    tripItems.value = []
  } finally {
    loadingItems.value = false
  }
}

// ── 지도 ────────────────────────────────────────────────────

function initMap() {
  if (!window.kakao || !mapEl.value) return
  mapInstance = new window.kakao.maps.Map(mapEl.value, {
    center: new window.kakao.maps.LatLng(36.5, 127.8),
    level: 13
  })
}

async function onSidoChange() {
  form.value.gugunCode = ''
  guguns.value = []
  if (!form.value.sidoCode) return
  try {
    const res = await attractionApi.guguns(form.value.sidoCode)
    guguns.value = res.data
  } catch {
    toastStore.show('구군 목록을 불러오지 못했습니다.', 'danger')
  }
}

async function doSearch() {
  loading.value = true
  searched.value = true
  try {
    const params = {}
    if (form.value.sidoCode) params.sidoCode = form.value.sidoCode
    if (form.value.gugunCode) params.gugunCode = form.value.gugunCode
    if (form.value.contentTypeId) params.contentTypeId = form.value.contentTypeId
    if (form.value.keyword) params.keyword = form.value.keyword
    const res = await attractionApi.search(params)
    results.value = res.data
    await nextTick()
    updateMap()
  } catch {
    toastStore.show('검색 중 오류가 발생했습니다.', 'danger')
  } finally {
    loading.value = false
  }
}

function updateMap() {
  if (!mapInstance || !results.value.length) return
  const bounds = new window.kakao.maps.LatLngBounds()
  let hasPoint = false
  results.value.forEach(item => {
    if (!item.lat || !item.lng) return
    const pos = new window.kakao.maps.LatLng(item.lat, item.lng)
    bounds.extend(pos)
    hasPoint = true
    const marker = new window.kakao.maps.Marker({ map: mapInstance, position: pos })
    const info = new window.kakao.maps.InfoWindow({
      content: `<div style="padding:8px 10px;font-size:12px;"><strong>${item.title}</strong><br>${item.address || ''}</div>`
    })
    window.kakao.maps.event.addListener(marker, 'click', () => info.open(mapInstance, marker))
  })
  if (hasPoint) { mapInstance.relayout(); mapInstance.setBounds(bounds) }
}

// ── 관광지 추가 ──────────────────────────────────────────────

function openModal(item) {
  selected.value = item
  if (!modalInstance) {
    const el = document.getElementById('attractionModal')
    modalInstance = new window.bootstrap.Modal(el)
  }
  modalInstance.show()
}

async function openAddModal() {
  modalInstance?.hide()
  if (currentContext.value) {
    await addToContext()
  } else {
    showAddModal.value = true
  }
}

async function addToContext() {
  if (!authStore.user) { toastStore.show('로그인이 필요합니다.', 'warning'); return }
  const a = selected.value
  const item = { contentId: String(a.contentId), title: a.title, lat: a.lat, lng: a.lng }
  try {
    if (currentContext.value._type === 'personal') {
      await planApi.addItem(currentContext.value.id, item)
      toastStore.show(`'${a.title}'을(를) '${currentContext.value.title}'에 추가했습니다.`)
      await loadTripItems()
    } else {
      await groupTripApi.addPlace(currentContext.value.id, item)
      toastStore.show(`'${a.title}'을(를) '${currentContext.value.title}'에 추가했습니다.`)
      // WebSocket이 연결됐으면 PLACE_ADDED 이벤트로 자동 갱신
      // 연결 안 됐을 때만 폴백으로 직접 로드
      if (!wsConnected.value) await loadTripItems()
    }
  } catch (e) {
    toastStore.show(e.response?.data?.error || '추가에 실패했습니다.', 'danger')
  }
}

function onTripSelected(trip) {
  const prevType = currentContext.value?._type
  currentContext.value = { _type: trip._type, id: trip.id, title: trip.title }
  showAddModal.value = false
  showChangeTripModal.value = false

  // 그룹 컨텍스트가 바뀌었으면 WebSocket 재연결
  if (trip._type === 'group') {
    connectGroupWebSocket(trip.id)
  } else if (prevType === 'group') {
    disconnectGroupWebSocket()
  }

  loadTripItems()
}

// ── 삭제 ────────────────────────────────────────────────────

async function deletePersonalItem(idx) {
  const items = tripItems.value.filter((_, i) => i !== idx)
  tripItems.value = items
  savingOrder.value = true
  lastSaved.value = false
  try {
    await planApi.reorderItems(currentContext.value.id, items)
    lastSaved.value = true
    setTimeout(() => { lastSaved.value = false }, 2000)
  } catch (e) {
    toastStore.show(e.response?.data?.error || '삭제에 실패했습니다.', 'danger')
    await loadTripItems()
  } finally {
    savingOrder.value = false
  }
}

async function deleteGroupItem(placeId) {
  try {
    await groupTripApi.removePlace(currentContext.value.id, placeId)
    // WebSocket PLACE_REMOVED 이벤트로 tripItems 자동 갱신
    // WS 미연결 시 폴백
    if (!wsConnected.value) {
      tripItems.value = tripItems.value.filter(p => p.id !== placeId)
    }
  } catch (e) {
    toastStore.show(e.response?.data?.error || '삭제에 실패했습니다.', 'danger')
  }
}

// ── 드래그 앤 드롭 (개인 여행 전용) ────────────────────────

function onDragStart(e, idx) {
  dragStartIdx.value = idx
  e.dataTransfer.effectAllowed = 'move'
}

function onDragOver(e, idx) {
  e.preventDefault()
  dragOverIdx.value = idx
}

function onDrop(e, idx) {
  e.preventDefault()
  if (dragStartIdx.value === null || dragStartIdx.value === idx) { onDragEnd(); return }
  const items = [...tripItems.value]
  const [moved] = items.splice(dragStartIdx.value, 1)
  items.splice(idx, 0, moved)
  tripItems.value = items
  onDragEnd()
  saveItemOrder()
}

function onDragEnd() {
  dragStartIdx.value = null
  dragOverIdx.value = null
}

async function saveItemOrder() {
  if (!currentContext.value || currentContext.value._type !== 'personal') return
  savingOrder.value = true
  lastSaved.value = false
  try {
    await planApi.reorderItems(currentContext.value.id, tripItems.value)
    lastSaved.value = true
    setTimeout(() => { lastSaved.value = false }, 2000)
  } catch (e) {
    toastStore.show(e.response?.data?.error || '순서 저장에 실패했습니다.', 'danger')
    await loadTripItems()
  } finally {
    savingOrder.value = false
  }
}
</script>

<style scoped>
.edit-context-bar {
  background: linear-gradient(135deg, #f0f5ff 0%, #e8f0fe 100%);
  border: 1px solid #c7d7fc;
  border-left: 4px solid #0d6efd;
}
.edit-context-icon {
  width: 38px;
  height: 38px;
  background: #0d6efd;
  color: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
}
.label-text {
  font-size: 0.65rem;
  color: #6c757d;
  letter-spacing: 0.05em;
}
.drag-handle {
  cursor: grab;
  font-size: 1.1rem;
  line-height: 1;
  user-select: none;
}
.trip-item {
  transition: background-color 0.1s;
}
.trip-item.drag-over {
  border-color: #0d6efd !important;
  border-width: 2px !important;
  background-color: #f0f5ff;
}
</style>
