<template>
  <section class="py-5 container">
    <div class="row g-4">
      <!-- 왼쪽: 초안 계획 + 지도 -->
      <div class="col-lg-7">
        <div class="card border-0 shadow-sm rounded-4 p-4 mb-4">
          <h2 class="fw-bold mb-3">여행 계획 경로 설정</h2>
          <div v-if="draft.length === 0" class="text-center p-5 border border-dashed rounded-4 text-muted">
            관광지 조회에서 장소를 추가하세요.
          </div>
          <div v-else class="d-flex flex-column gap-3">
            <div v-for="(item, idx) in draft" :key="idx" class="card border rounded-4 p-3">
              <div class="d-flex justify-content-between align-items-center">
                <div>
                  <span class="badge text-bg-primary me-2">{{ idx + 1 }}</span>
                  <span class="fw-semibold">{{ item.title }}</span>
                </div>
                <div class="d-flex gap-2">
                  <button class="btn btn-sm btn-outline-secondary" :disabled="idx === 0" @click="moveItem(idx, 'up')">위로</button>
                  <button class="btn btn-sm btn-outline-secondary" :disabled="idx === draft.length - 1" @click="moveItem(idx, 'down')">아래로</button>
                  <button class="btn btn-sm btn-outline-danger" @click="removeItem(idx)">삭제</button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="card border-0 shadow-sm rounded-4 p-4">
          <h4 class="fw-bold mb-3">지도 / 추천 경로</h4>
          <div ref="planMapEl" class="rounded-4 border shadow-sm" style="min-height:420px;"></div>
        </div>
      </div>

      <!-- 오른쪽: 숙소 + 저장 폼 -->
      <div class="col-lg-5">
        <div class="card border-0 shadow-sm rounded-4 p-4 sticky-top-card">
          <h6 class="fw-bold mb-2">숙소 정보</h6>
          <input v-model="lodgingAddress" type="text" class="form-control mb-2" placeholder="숙소 주소를 입력하세요">
          <button class="btn btn-outline-secondary w-100 mb-3" type="button" @click="resolveLodging">숙소 좌표 확인</button>
          <div :class="['small lodging-status-card mb-3', lodgingConfirmed ? 'is-confirmed' : '']">
            <span v-if="lodgingConfirmed">
              <strong>{{ lodgingAddress }}</strong><br>
              좌표 확인 완료: {{ lodgingLat.toFixed(5) }}, {{ lodgingLng.toFixed(5) }}
            </span>
            <span v-else>숙소를 입력하면 저장 계획에서 최단 소요시간 경로를 추천합니다.</span>
          </div>
          <hr>
          <input v-model="planTitle" type="text" class="form-control mb-3" placeholder="일정 제목">
          <textarea v-model="planMemo" class="form-control mb-3" rows="3" placeholder="상세 계획 및 경비 입력"></textarea>
          <button class="btn btn-indigo w-100" @click="savePlan" :disabled="saving">
            {{ saving ? '저장 중...' : '계획 저장' }}
          </button>
          <button class="btn btn-outline-secondary w-100 mt-2" @click="clearDraft">작성 중인 계획 비우기</button>
        </div>
      </div>
    </div>

    <!-- 저장된 여행 계획 -->
    <div class="mt-5">
      <h4 class="fw-bold mb-3">저장된 여행 계획</h4>
      <div v-if="!authStore.user" class="alert alert-light border rounded-4">
        저장된 여행 계획은 로그인 후 확인할 수 있습니다.
      </div>
      <div v-else-if="savedPlans.length === 0" class="alert alert-light border rounded-4">
        저장된 여행 계획이 없습니다.
      </div>
      <div v-else class="row g-3">
        <div v-for="plan in savedPlans" :key="plan.id" class="col-lg-6">
          <div class="saved-plan-card card border-0 shadow-sm rounded-4 h-100">
            <div class="card-body p-4">
              <div class="d-flex justify-content-between align-items-start mb-2">
                <div>
                  <h5 class="fw-bold mb-1">{{ plan.title }}</h5>
                  <div class="small text-muted">{{ plan.createdAt }}</div>
                </div>
                <span class="badge rounded-pill text-bg-primary">{{ plan.items?.length ?? 0 }}개</span>
              </div>
              <p class="small mb-2">{{ plan.memo || '메모 없음' }}</p>
              <p class="small text-muted mb-3">
                {{ plan.lodging ? '숙소: ' + plan.lodging.address : '숙소 미입력' }}
              </p>
              <div class="d-flex flex-wrap gap-2">
                <button class="btn btn-sm btn-outline-secondary" @click="viewPlanOnMap(plan)">지도 보기</button>
                <button class="btn btn-sm btn-indigo" @click="recommendRoute(plan.id)">최단 경로 추천</button>
                <button class="btn btn-sm btn-outline-danger" @click="deletePlan(plan.id)">삭제</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 경로 추천 결과 -->
      <div v-if="routeResult" class="route-result-card card border-0 shadow-sm rounded-4 p-4 mt-4">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <h5 class="fw-bold mb-0">최단 경로 추천 결과</h5>
          <span class="badge text-bg-primary">{{ routeResult.totalMinutes }}분</span>
        </div>
        <div class="small mb-3">
          <span v-for="(stop, i) in routeResult.orderedStops" :key="i">
            {{ stop.title }}<span v-if="i < routeResult.orderedStops.length - 1"> → </span>
          </span>
        </div>
        <div class="table-responsive">
          <table class="table align-middle">
            <thead>
              <tr><th>구간</th><th>경로</th><th>소요시간</th></tr>
            </thead>
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
  </section>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { planApi } from '../api/index.js'
import { useAuthStore } from '../stores/auth.js'
import { useToastStore } from '../stores/toast.js'

const authStore = useAuthStore()
const toastStore = useToastStore()

const planMapEl = ref(null)
const draft = ref([])
const savedPlans = ref([])
const routeResult = ref(null)
const saving = ref(false)

const lodgingAddress = ref('')
const lodgingLat = ref(0)
const lodgingLng = ref(0)
const lodgingConfirmed = ref(false)

const planTitle = ref('')
const planMemo = ref('')

let planMap = null

onMounted(async () => {
  await loadDraft()
  if (authStore.user) await loadSavedPlans()
  await nextTick()
  initPlanMap()
})

function initPlanMap() {
  if (!window.kakao || !planMapEl.value) return
  planMap = new window.kakao.maps.Map(planMapEl.value, {
    center: new window.kakao.maps.LatLng(37.5665, 126.9780),
    level: 8
  })
}

async function loadDraft() {
  try {
    const res = await planApi.getDraft()
    draft.value = res.data.items || []
  } catch {
    draft.value = []
  }
}

async function loadSavedPlans() {
  try {
    const res = await planApi.list()
    savedPlans.value = res.data
  } catch {
    savedPlans.value = []
  }
}

async function moveItem(index, direction) {
  await planApi.moveDraftItem(index, direction)
  await loadDraft()
}

async function removeItem(index) {
  await planApi.removeDraftItem(index)
  await loadDraft()
}

async function clearDraft() {
  await planApi.clearDraft()
  await loadDraft()
  toastStore.show('계획이 비워졌습니다.')
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
      toastStore.show('숙소 좌표를 찾지 못했습니다.', 'danger')
      return
    }
    lodgingLat.value = parseFloat(result[0].y)
    lodgingLng.value = parseFloat(result[0].x)
    lodgingConfirmed.value = true
    toastStore.show('숙소 좌표가 확인되었습니다.')
  })
}

async function savePlan() {
  if (!authStore.user) {
    toastStore.show('로그인이 필요합니다.', 'warning')
    return
  }
  saving.value = true
  try {
    const body = { title: planTitle.value, memo: planMemo.value }
    if (lodgingConfirmed.value) {
      body.lodging = {
        address: lodgingAddress.value,
        placeName: lodgingAddress.value,
        lat: lodgingLat.value,
        lng: lodgingLng.value
      }
    }
    await planApi.save(body)
    await loadDraft()
    await loadSavedPlans()
    planTitle.value = ''
    planMemo.value = ''
    lodgingAddress.value = ''
    lodgingConfirmed.value = false
    toastStore.show('계획이 저장되었습니다.')
  } catch {
    toastStore.show('저장 중 오류가 발생했습니다.', 'danger')
  } finally {
    saving.value = false
  }
}

async function deletePlan(id) {
  if (!confirm('계획을 삭제하시겠습니까?')) return
  try {
    await planApi.delete(id)
    await loadSavedPlans()
    toastStore.show('계획이 삭제되었습니다.')
  } catch {
    toastStore.show('삭제 중 오류가 발생했습니다.', 'danger')
  }
}

async function recommendRoute(id) {
  try {
    const res = await planApi.recommend(id)
    routeResult.value = res.data
    drawRouteOnMap(res.data)
    toastStore.show('경로 추천 완료!')
  } catch {
    toastStore.show('경로 추천 중 오류가 발생했습니다.', 'danger')
  }
}

function viewPlanOnMap(plan) {
  if (!planMap) return
  const payload = buildMapPayload(plan)
  drawRouteOnMap(payload)
}

function buildMapPayload(plan) {
  const stops = (plan.items || []).map(item => ({
    title: item.title, lat: item.lat, lng: item.lng, lodging: false
  }))
  if (plan.lodging) {
    stops.push({ title: plan.lodging.placeName || plan.lodging.address, lat: plan.lodging.lat, lng: plan.lodging.lng, lodging: true })
  }
  return { stops, segments: [] }
}

function drawRouteOnMap(payload) {
  if (!planMap || !payload?.stops?.length) return
  const bounds = new window.kakao.maps.LatLngBounds()
  payload.stops.forEach((stop, i) => {
    const pos = new window.kakao.maps.LatLng(stop.lat, stop.lng)
    bounds.extend(pos)
    const marker = new window.kakao.maps.Marker({ map: planMap, position: pos })
    const label = stop.lodging ? '숙소' : String(i + 1)
    const info = new window.kakao.maps.InfoWindow({
      content: `<div style="padding:8px 10px;font-size:12px;"><strong>${label}. ${stop.title}</strong></div>`
    })
    info.open(planMap, marker)
  })
  if (payload.segments?.length) {
    const colors = ['#2563eb', '#f97316', '#16a34a', '#db2777', '#eab308', '#0ea5e9']
    payload.segments.forEach((seg, i) => {
      if (!seg?.length) return
      new window.kakao.maps.Polyline({
        map: planMap,
        path: seg.map(p => new window.kakao.maps.LatLng(p.lat, p.lng)),
        strokeWeight: 5,
        strokeOpacity: 0.9,
        strokeStyle: 'solid',
        strokeColor: colors[i % colors.length]
      })
    })
  }
  planMap.setBounds(bounds)
}
</script>
