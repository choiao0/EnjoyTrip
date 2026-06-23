<template>
  <section class="py-5 container">
    <router-link to="/trips" class="text-muted small">← 여행 목록</router-link>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border" role="status"></div>
    </div>

    <template v-else-if="plan">
      <div class="d-flex justify-content-between align-items-start mt-2 mb-4">
        <div>
          <h2 class="fw-bold mb-0">{{ plan.title }}</h2>
          <p class="text-muted small mb-0">{{ plan.createdAt }}</p>
        </div>
        <button class="btn btn-outline-danger btn-sm" @click="deletePlan">삭제</button>
      </div>

      <div class="row g-4">
        <div class="col-lg-7">
          <div class="card shadow-sm p-3 mb-3">
            <h6 class="fw-bold mb-2">장소 목록 ({{ plan.items?.length ?? 0 }}개{{ plan.lodging ? ' + 숙소' : '' }})</h6>
            <div v-if="!plan.items?.length && !plan.lodging" class="text-muted text-center py-3 small">
              장소가 없습니다. <router-link to="/attractions">관광정보 검색</router-link>에서 추가하세요.
            </div>
            <div v-else class="d-flex flex-column gap-2">
              <div
                v-for="(item, idx) in plan.items"
                :key="idx"
                class="d-flex align-items-center gap-2 border rounded-3 p-2"
              >
                <span class="badge text-bg-primary">{{ idx + 1 }}</span>
                <span class="fw-semibold">{{ item.title }}</span>
              </div>
              <div
                v-if="plan.lodging"
                class="d-flex align-items-center gap-2 border rounded-3 p-2 bg-light"
              >
                <span class="badge text-bg-secondary">숙소</span>
                <div>
                  <span class="fw-semibold">{{ plan.lodging.placeName || plan.lodging.address }}</span>
                  <span v-if="plan.lodging.placeName && plan.lodging.placeName !== plan.lodging.address" class="text-muted small ms-2">{{ plan.lodging.address }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="card shadow-sm p-3">
            <h6 class="fw-bold mb-2">지도</h6>
            <div ref="mapEl" class="rounded-3 border" style="height:360px;"></div>
          </div>
        </div>

        <div class="col-lg-5">
          <div class="card shadow-sm p-4">
            <p v-if="plan.memo" class="mb-3">{{ plan.memo }}</p>

            <!-- 숙소 없음: 안내 문구 + 등록 폼 -->
            <template v-if="!plan.lodging">
              <div class="alert alert-light border small mb-3">
                최단 경로 추천을 이용하려면 숙소가 등록된 계획이 필요합니다.
              </div>
              <div class="mb-3">
                <label class="form-label small fw-semibold">숙소 이름 <span class="text-muted fw-normal">(선택)</span></label>
                <input v-model="lodgingName" class="form-control mb-2" placeholder="예: 명동 호텔" />
                <label class="form-label small fw-semibold">숙소 주소</label>
                <input
                  v-model="lodgingAddress"
                  class="form-control mb-2"
                  placeholder="예: 서울 중구 명동길 74"
                  @keyup.enter="resolveLodging"
                />
                <div class="d-flex gap-2 mb-2">
                  <button class="btn btn-sm btn-outline-secondary flex-grow-1" @click="resolveLodging">좌표 확인</button>
                  <button
                    class="btn btn-sm btn-indigo flex-grow-1"
                    :disabled="!lodgingConfirmed || savingLodging"
                    @click="saveLodging"
                  >{{ savingLodging ? '저장 중...' : '숙소 등록' }}</button>
                </div>
                <p v-if="lodgingConfirmed" class="small text-success mb-0">
                  ✓ 좌표 확인 완료 ({{ lodgingLat.toFixed(4) }}, {{ lodgingLng.toFixed(4) }})
                </p>
              </div>
            </template>

            <!-- 숙소 있음: 표시 + 변경 + 경로 추천 -->
            <template v-else>
              <div class="d-flex justify-content-between align-items-center mb-2">
                <div>
                  <p class="small fw-semibold mb-0">
                    {{ plan.lodging.placeName || plan.lodging.address }}
                  </p>
                  <p v-if="plan.lodging.placeName" class="small text-muted mb-0">{{ plan.lodging.address }}</p>
                </div>
                <button class="btn btn-sm btn-link p-0 text-secondary flex-shrink-0 ms-2" @click="toggleLodgingEdit">
                  {{ showLodgingForm ? '취소' : '숙소 변경' }}
                </button>
              </div>

              <div v-if="showLodgingForm" class="border rounded-3 p-3 mb-3 bg-light">
                <label class="form-label small fw-semibold">숙소 이름 <span class="text-muted fw-normal">(선택)</span></label>
                <input v-model="lodgingName" class="form-control mb-2" placeholder="예: 명동 호텔" />
                <label class="form-label small fw-semibold">숙소 주소</label>
                <input
                  v-model="lodgingAddress"
                  class="form-control mb-2"
                  placeholder="새 숙소 주소"
                  @keyup.enter="resolveLodging"
                />
                <div class="d-flex gap-2 mb-2">
                  <button class="btn btn-sm btn-outline-secondary flex-grow-1" @click="resolveLodging">좌표 확인</button>
                  <button
                    class="btn btn-sm btn-indigo flex-grow-1"
                    :disabled="!lodgingConfirmed || savingLodging"
                    @click="saveLodging"
                  >{{ savingLodging ? '저장 중...' : '변경 저장' }}</button>
                </div>
                <p v-if="lodgingConfirmed" class="small text-success mb-0">
                  ✓ 좌표 확인 완료 ({{ lodgingLat.toFixed(4) }}, {{ lodgingLng.toFixed(4) }})
                </p>
              </div>

              <button
                class="btn btn-indigo w-100 mb-3"
                :disabled="recommending"
                @click="recommendRoute"
              >
                {{ recommending ? '계산 중...' : '최단 경로 추천' }}
              </button>
            </template>

            <div v-if="routeResult">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <h6 class="fw-bold mb-0">추천 경로</h6>
                <span class="badge text-bg-primary">{{ routeResult.totalMinutes }}분</span>
              </div>
              <div class="small mb-3">
                <span v-for="(stop, i) in routeResult.orderedStops" :key="i">
                  {{ stop.title }}<span v-if="i < routeResult.orderedStops.length - 1"> → </span>
                </span>
              </div>
              <div class="table-responsive">
                <table class="table table-sm">
                  <thead><tr><th>구간</th><th>경로</th><th>소요시간</th></tr></thead>
                  <tbody>
                    <tr v-for="(leg, i) in routeResult.legs" :key="i">
                      <td>{{ i + 1 }}</td>
                      <td>{{ leg.fromTitle }} → {{ leg.toTitle }}</td>
                      <td>{{ leg.minutes }}분</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="text-center text-muted py-5">계획을 찾을 수 없습니다.</div>
  </section>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { planApi } from '../api/index.js'
import { useAuthStore } from '../stores/auth.js'
import { useToastStore } from '../stores/toast.js'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

const loading = ref(true)
const plan = ref(null)
const mapEl = ref(null)
const routeResult = ref(null)
const recommending = ref(false)
let planMap = null

const lodgingName = ref('')
const lodgingAddress = ref('')
const lodgingLat = ref(0)
const lodgingLng = ref(0)
const lodgingConfirmed = ref(false)
const savingLodging = ref(false)
const showLodgingForm = ref(false)

onMounted(async () => {
  if (!authStore.user) {
    router.push('/auth/login')
    return
  }
  try {
    const res = await planApi.list()
    const id = route.params.id
    plan.value = res.data.find(p => String(p.id) === String(id)) || null
  } finally {
    loading.value = false
  }
  if (plan.value) {
    await nextTick()
    initMap()
  }
})

function initMap() {
  if (!window.kakao || !mapEl.value) return
  planMap = new window.kakao.maps.Map(mapEl.value, {
    center: new window.kakao.maps.LatLng(37.5665, 126.9780),
    level: 8
  })
  drawOnMap(plan.value)
}

function drawOnMap(p) {
  if (!planMap || !p?.items?.length) return
  const bounds = new window.kakao.maps.LatLngBounds()
  p.items.forEach((item, i) => {
    if (!item.lat || !item.lng) return
    const pos = new window.kakao.maps.LatLng(item.lat, item.lng)
    bounds.extend(pos)
    const marker = new window.kakao.maps.Marker({ map: planMap, position: pos })
    const info = new window.kakao.maps.InfoWindow({
      content: `<div style="padding:6px 8px;font-size:12px;"><strong>${i + 1}. ${item.title}</strong></div>`
    })
    info.open(planMap, marker)
  })
  if (p.lodging?.lat && p.lodging?.lng) {
    const pos = new window.kakao.maps.LatLng(p.lodging.lat, p.lodging.lng)
    bounds.extend(pos)
    new window.kakao.maps.Marker({ map: planMap, position: pos })
  }
  planMap.setBounds(bounds)
}

async function recommendRoute() {
  recommending.value = true
  try {
    const res = await planApi.recommend(plan.value.id)
    routeResult.value = res.data
    drawRouteOnMap(res.data)
    toastStore.show('경로 추천 완료!')
  } catch (e) {
    toastStore.show(e.response?.data?.error || '경로 추천 중 오류가 발생했습니다.', 'danger')
  } finally {
    recommending.value = false
  }
}

function drawRouteOnMap(data) {
  if (!planMap) return
  drawOnMap(plan.value)
  if (!data?.segments?.length) return
  const colors = ['#2563eb', '#f97316', '#16a34a', '#db2777', '#eab308']
  data.segments.forEach((seg, i) => {
    if (!seg?.length) return
    new window.kakao.maps.Polyline({
      map: planMap,
      path: seg.map(p => new window.kakao.maps.LatLng(p.lat, p.lng)),
      strokeWeight: 5, strokeOpacity: 0.9, strokeStyle: 'solid',
      strokeColor: colors[i % colors.length]
    })
  })
}

function toggleLodgingEdit() {
  showLodgingForm.value = !showLodgingForm.value
  lodgingName.value = ''
  lodgingAddress.value = ''
  lodgingConfirmed.value = false
}

function resolveLodging() {
  if (!lodgingAddress.value.trim()) {
    toastStore.show('숙소 주소를 입력하세요.', 'warning')
    return
  }
  if (!window.kakao?.maps?.services) {
    toastStore.show('카카오 지도 서비스를 불러오는 중입니다.', 'warning')
    return
  }
  const geocoder = new window.kakao.maps.services.Geocoder()
  geocoder.addressSearch(lodgingAddress.value, (result, status) => {
    if (status !== window.kakao.maps.services.Status.OK || !result.length) {
      toastStore.show('주소를 찾지 못했습니다. 정확한 주소를 입력해보세요.', 'danger')
      return
    }
    lodgingLat.value = parseFloat(result[0].y)
    lodgingLng.value = parseFloat(result[0].x)
    lodgingConfirmed.value = true
    toastStore.show('좌표가 확인됐습니다. 숙소 등록 버튼을 눌러 저장하세요.')
  })
}

async function saveLodging() {
  if (!lodgingConfirmed.value) return
  savingLodging.value = true
  try {
    const res = await planApi.updateLodging(plan.value.id, {
      lat: lodgingLat.value,
      lng: lodgingLng.value,
      address: lodgingAddress.value,
      placeName: lodgingName.value.trim() || lodgingAddress.value
    })
    plan.value = res.data
    showLodgingForm.value = false
    lodgingName.value = ''
    lodgingAddress.value = ''
    lodgingConfirmed.value = false
    toastStore.show('숙소가 등록됐습니다.')
    await nextTick()
    drawOnMap(plan.value)
  } catch (e) {
    toastStore.show(e.response?.data?.error || '숙소 등록 중 오류가 발생했습니다.', 'danger')
  } finally {
    savingLodging.value = false
  }
}

async function deletePlan() {
  if (!confirm('계획을 삭제하시겠습니까?')) return
  try {
    await planApi.delete(plan.value.id)
    toastStore.show('계획이 삭제됐습니다.')
    router.push('/trips')
  } catch {
    toastStore.show('삭제 중 오류가 발생했습니다.', 'danger')
  }
}
</script>
