<template>
  <section class="py-4 container px-4 px-lg-5">
    <div class="d-flex align-items-center gap-2 mb-4">
      <h2 class="fw-bold mb-0">관광정보 조회</h2>
    </div>
    <div class="row g-4 align-items-stretch">
      <!-- 지도 -->
      <div class="col-lg-7">
        <div class="map-wrap position-relative">
          <div ref="mapEl" class="rounded-6 border shadow-sm w-100" style="min-height:500px;"></div>
        </div>
      </div>
      <!-- 검색 패널 -->
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

        <!-- 결과 목록 -->
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
          <button type="button" class="btn btn-indigo btn-sm px-4" @click="addToDraft">
            <i class="bi bi-plus-lg me-1"></i>계획에 추가
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { attractionApi, planApi } from '../api/index.js'
import { useToastStore } from '../stores/toast.js'

const route = useRoute()
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
let mapInstance = null
let modalInstance = null

const form = ref({
  sidoCode: '',
  gugunCode: '',
  contentTypeId: '',
  keyword: route.query.keyword || ''
})

onMounted(async () => {
  sidosLoading.value = true
  sidosError.value = ''
  try {
    const res = await attractionApi.sidos()
    sidos.value = res.data
  } catch (e) {
    sidosError.value = '시도 목록을 불러오지 못했습니다. 서버 연결을 확인해 주세요.'
    toastStore.show(sidosError.value, 'danger')
  } finally {
    sidosLoading.value = false
  }

  await nextTick()
  initMap()

  if (form.value.keyword) {
    doSearch()
  }
})

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
  if (hasPoint) {
    mapInstance.relayout()
    mapInstance.setBounds(bounds)
  }
}

function openModal(item) {
  selected.value = item
  if (!modalInstance) {
    const el = document.getElementById('attractionModal')
    modalInstance = new window.bootstrap.Modal(el)
  }
  modalInstance.show()
}

async function addToDraft() {
  if (!selected.value) return
  try {
    const res = await planApi.addDraftItem({
      contentId: String(selected.value.contentId),
      title: selected.value.title,
      lat: selected.value.lat,
      lng: selected.value.lng
    })
    modalInstance.hide()
    toastStore.show(res.data.message, res.data.ok ? 'success' : 'warning')
  } catch {
    toastStore.show('오류가 발생했습니다.', 'danger')
  }
}
</script>
