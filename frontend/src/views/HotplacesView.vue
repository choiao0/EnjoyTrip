<template>
  <section class="py-5 container">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h2 class="fw-bold mb-0">나의 장소</h2>
        <p class="text-muted small mb-0">나만 볼 수 있는 개인 장소 저장소입니다.</p>
      </div>
    </div>

    <!-- 비로그인 -->
    <div v-if="!authStore.user" class="text-center py-5 text-muted">
      <p class="mb-3">장소를 저장하려면 로그인이 필요합니다.</p>
      <router-link class="btn btn-indigo" to="/auth/login">로그인</router-link>
    </div>

    <div v-else class="row g-4">
      <!-- 왼쪽: 지도 + 목록 -->
      <div class="col-lg-7">
        <!-- 카카오 지도 -->
        <div class="card shadow-sm border-0 mb-4">
          <div class="card-body p-3">
            <div ref="mapEl" class="rounded-3" style="height:380px;"></div>
          </div>
        </div>

        <!-- 저장된 장소 목록 -->
        <div v-if="loading" class="text-center py-4">
          <div class="spinner-border spinner-border-sm text-secondary"></div>
        </div>
        <div v-else-if="hotplaces.length === 0" class="text-center text-muted py-4 border rounded-3 bg-white">
          저장된 장소가 없습니다. 오른쪽에서 장소를 추가해보세요.
        </div>
        <div v-else class="d-flex flex-column gap-3">
          <div
            v-for="hp in hotplaces"
            :key="hp.id"
            :class="['card border-0 shadow-sm rounded-4 overflow-hidden', selectedId === hp.id ? 'border-primary border-2' : '']"
            style="cursor:pointer;"
            @click="focusPlace(hp)"
          >
            <div class="row g-0">
              <div v-if="hp.imagePath" class="col-3">
                <img
                  :src="`/uploads/${hp.imagePath}`"
                  :alt="hp.name"
                  class="w-100 h-100"
                  style="object-fit:cover;min-height:100px;"
                  @error="e => e.target.style.display='none'"
                />
              </div>
              <div :class="hp.imagePath ? 'col-9' : 'col-12'">
                <div class="card-body py-3">
                  <div class="d-flex justify-content-between align-items-start">
                    <div class="flex-grow-1 me-2">
                      <h6 class="fw-bold mb-1">{{ hp.name }}</h6>
                      <p v-if="hp.address" class="small text-muted mb-1">
                        <i class="bi bi-geo-alt me-1"></i>{{ hp.address }}
                      </p>
                      <p v-if="hp.description" class="small text-muted mb-0">{{ hp.description }}</p>
                    </div>
                    <button
                      class="btn btn-outline-danger btn-sm py-0 px-2 flex-shrink-0"
                      @click.stop="deleteHotplace(hp.id)"
                    >✕</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 오른쪽: 등록 폼 -->
      <div class="col-lg-5">
        <div class="card border-0 shadow-sm rounded-4 p-4 sticky-top-card">
          <h5 class="fw-bold mb-4">장소 추가</h5>
          <form @submit.prevent="submitHotplace">
            <div class="mb-3">
              <label class="form-label small fw-semibold">장소명 <span class="text-danger">*</span></label>
              <input v-model="form.name" class="form-control" placeholder="예: 단골 카페, 비밀 공원" required />
            </div>

            <div class="mb-3">
              <label class="form-label small fw-semibold">주소</label>
              <div class="d-flex gap-2">
                <input
                  v-model="form.address"
                  class="form-control"
                  placeholder="예: 서울 마포구 연남동"
                  @keyup.enter.prevent="resolveAddress"
                />
                <button type="button" class="btn btn-outline-secondary flex-shrink-0" @click="resolveAddress">
                  확인
                </button>
              </div>
              <p v-if="coordConfirmed" class="small text-success mt-1 mb-0">
                ✓ 위치 확인 완료
              </p>
            </div>

            <div class="mb-3">
              <label class="form-label small fw-semibold">메모</label>
              <textarea v-model="form.description" class="form-control" rows="3" placeholder="이 장소에 대한 메모를 남겨보세요." />
            </div>

            <div class="mb-4">
              <label class="form-label small fw-semibold">사진 <span class="text-muted fw-normal">(선택)</span></label>
              <div
                class="upload-zone rounded-3"
                :class="{ dragover: isDragging }"
                @click="fileInput.click()"
                @dragenter.prevent="isDragging = true"
                @dragover.prevent="isDragging = true"
                @dragleave.prevent="isDragging = false"
                @drop.prevent="onDrop"
              >
                <i class="bi bi-cloud-arrow-up fs-2 text-muted"></i>
                <p class="small text-muted mb-0 mt-1">클릭하거나 드래그해서 사진 추가</p>
              </div>
              <input ref="fileInput" type="file" accept="image/*" class="d-none" @change="onFileChange" />
              <div v-if="previewUrl" class="mt-2 position-relative d-inline-block">
                <img :src="previewUrl" class="rounded-3" style="max-height:120px;object-fit:cover;" />
                <button
                  type="button"
                  class="btn btn-sm btn-danger position-absolute top-0 end-0"
                  style="margin:4px;"
                  @click="clearImage"
                >✕</button>
              </div>
            </div>

            <button class="btn btn-indigo w-100" type="submit" :disabled="submitting">
              {{ submitting ? '저장 중...' : '장소 저장' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { hotplaceApi } from '../api/index.js'
import { useAuthStore } from '../stores/auth.js'
import { useToastStore } from '../stores/toast.js'

const authStore = useAuthStore()
const toastStore = useToastStore()

const mapEl = ref(null)
const fileInput = ref(null)
const hotplaces = ref([])
const loading = ref(false)
const submitting = ref(false)
const isDragging = ref(false)
const previewUrl = ref(null)
const selectedFile = ref(null)
const coordConfirmed = ref(false)
const selectedId = ref(null)

const form = ref({ name: '', description: '', address: '', lat: null, lng: null })

let map = null
const markers = []

onMounted(async () => {
  await nextTick()
  initMap()
  if (authStore.user) await loadHotplaces()
})

function initMap() {
  if (!window.kakao || !mapEl.value) return
  map = new window.kakao.maps.Map(mapEl.value, {
    center: new window.kakao.maps.LatLng(36.5, 127.8),
    level: 13
  })
}

function clearMarkers() {
  markers.forEach(m => m.setMap(null))
  markers.length = 0
}

function drawMarkers(list) {
  clearMarkers()
  if (!map || !list.length) return
  const bounds = new window.kakao.maps.LatLngBounds()
  let hasPoint = false
  list.forEach(hp => {
    if (!hp.lat || !hp.lng) return
    const pos = new window.kakao.maps.LatLng(hp.lat, hp.lng)
    bounds.extend(pos)
    hasPoint = true
    const marker = new window.kakao.maps.Marker({ map, position: pos })
    const info = new window.kakao.maps.InfoWindow({
      content: `<div style="padding:6px 10px;font-size:12px;"><strong>${hp.name}</strong></div>`
    })
    window.kakao.maps.event.addListener(marker, 'click', () => {
      info.open(map, marker)
      selectedId.value = hp.id
    })
    markers.push(marker)
  })
  if (hasPoint) map.setBounds(bounds)
}

function focusPlace(hp) {
  selectedId.value = hp.id
  if (!map || !hp.lat || !hp.lng) return
  map.setCenter(new window.kakao.maps.LatLng(hp.lat, hp.lng))
  map.setLevel(4)
}

async function loadHotplaces() {
  loading.value = true
  try {
    const res = await hotplaceApi.list()
    hotplaces.value = res.data
    await nextTick()
    drawMarkers(hotplaces.value)
  } catch {
    toastStore.show('장소 목록을 불러오지 못했습니다.', 'danger')
  } finally {
    loading.value = false
  }
}

function resolveAddress() {
  if (!form.value.address.trim()) {
    toastStore.show('주소를 입력하세요.', 'warning')
    return
  }
  if (!window.kakao?.maps?.services) {
    toastStore.show('카카오 지도 서비스를 불러오는 중입니다.', 'warning')
    return
  }
  const geocoder = new window.kakao.maps.services.Geocoder()
  geocoder.addressSearch(form.value.address, (result, status) => {
    if (status !== window.kakao.maps.services.Status.OK || !result.length) {
      toastStore.show('주소를 찾지 못했습니다. 더 정확한 주소를 입력해보세요.', 'danger')
      return
    }
    form.value.lat = parseFloat(result[0].y)
    form.value.lng = parseFloat(result[0].x)
    coordConfirmed.value = true
    if (map) {
      const pos = new window.kakao.maps.LatLng(form.value.lat, form.value.lng)
      map.setCenter(pos)
      map.setLevel(4)
    }
    toastStore.show('위치가 확인됐습니다.')
  })
}

function onFileChange(e) {
  const file = e.target.files[0]
  if (file) setFile(file)
}

function onDrop(e) {
  isDragging.value = false
  const file = e.dataTransfer.files[0]
  if (file) setFile(file)
}

function setFile(file) {
  selectedFile.value = file
  const reader = new FileReader()
  reader.onload = e => { previewUrl.value = e.target.result }
  reader.readAsDataURL(file)
}

function clearImage() {
  selectedFile.value = null
  previewUrl.value = null
  if (fileInput.value) fileInput.value.value = ''
}

async function submitHotplace() {
  if (!authStore.user) {
    toastStore.show('로그인이 필요합니다.', 'warning')
    return
  }
  submitting.value = true
  try {
    const fd = new FormData()
    fd.append('name', form.value.name)
    if (form.value.description) fd.append('description', form.value.description)
    if (form.value.address) fd.append('address', form.value.address)
    if (form.value.lat != null) fd.append('lat', form.value.lat)
    if (form.value.lng != null) fd.append('lng', form.value.lng)
    if (selectedFile.value) fd.append('image', selectedFile.value)

    await hotplaceApi.create(fd)
    form.value = { name: '', description: '', address: '', lat: null, lng: null }
    coordConfirmed.value = false
    clearImage()
    await loadHotplaces()
    toastStore.show('장소가 저장됐습니다.')
  } catch (e) {
    toastStore.show(e.response?.data?.error || '저장 중 오류가 발생했습니다.', 'danger')
  } finally {
    submitting.value = false
  }
}

async function deleteHotplace(id) {
  if (!confirm('이 장소를 삭제하시겠습니까?')) return
  try {
    await hotplaceApi.delete(id)
    if (selectedId.value === id) selectedId.value = null
    await loadHotplaces()
    toastStore.show('장소가 삭제됐습니다.')
  } catch {
    toastStore.show('삭제 중 오류가 발생했습니다.', 'danger')
  }
}
</script>
