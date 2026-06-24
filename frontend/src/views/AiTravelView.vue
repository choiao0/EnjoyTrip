<template>
  <main class="py-5">
    <div class="container">
      <h1 class="h2 fw-bold mb-1">AI 여행도우미</h1>
      <p class="text-muted mb-4">AI가 관광지 정보를 바탕으로 여행 계획을 도와드립니다.</p>

      <div class="row g-4">
        <div class="col-lg-5">
          <section class="bg-white border rounded-3 p-4">
            <label class="form-label fw-semibold mb-2">질문</label>
            <div class="d-flex flex-wrap gap-2 mb-2">
              <button
                v-for="chip in chips"
                :key="chip"
                class="btn btn-sm btn-outline-secondary rounded-pill"
                @click="form.message = chip"
              >{{ chip }}</button>
            </div>
            <textarea
              v-model="form.message"
              class="form-control mb-3"
              rows="4"
              placeholder="예: 서울 여행지 추천해줘. 날씨와 일정 팁도 같이 알려줘."
            />

            <div class="row g-2 mb-3">
              <div class="col-6">
                <label class="form-label small fw-semibold">지역</label>
                <select v-model="selectedSido" class="form-select" @change="onSidoChange">
                  <option value="">전체</option>
                  <option v-for="s in sidos" :key="s.sidoCode" :value="s.sidoCode">{{ s.sidoName }}</option>
                </select>
              </div>
              <div class="col-6">
                <label class="form-label small fw-semibold">시군구</label>
                <select v-model="selectedGugun" class="form-select" :disabled="!selectedSido || !guguns.length">
                  <option value="">전체</option>
                  <option v-for="g in guguns" :key="g.gugunCode" :value="g.gugunCode">{{ g.gugunName }}</option>
                </select>
              </div>
            </div>

            <button class="btn btn-indigo w-100" :disabled="loading" @click="ask">
              {{ loading ? 'AI 응답 중...' : '질문하기' }}
            </button>
          </section>
        </div>

        <div class="col-lg-7">
          <!-- AI 텍스트 답변 -->
          <section class="bg-white border rounded-3 p-4 mb-4">
            <h2 class="h5 fw-bold mb-3">답변</h2>
            <div v-if="loading" class="text-muted py-5 text-center">
              <div class="spinner-border spinner-border-sm me-2"></div>AI가 답변을 생성하고 있습니다...
            </div>
            <div v-else-if="error" class="alert alert-danger mb-0">{{ error }}</div>
            <div v-else-if="answer" class="ai-answer-body" v-html="renderedAnswer"></div>
            <div v-else class="text-muted py-5 text-center">질문을 입력하고 버튼을 눌러보세요.</div>
          </section>

          <!-- AI 추천 관광지 카드 -->
          <div v-if="recommendedCards.length">
            <div class="d-flex align-items-center gap-2 mb-3">
              <span class="fw-bold">AI 추천 관광지</span>
              <span class="badge bg-indigo">{{ recommendedCards.length }}곳</span>
              <span v-if="showingAll" class="text-muted small">· 전체 검색 결과</span>
            </div>
            <div class="d-flex flex-column gap-3">
              <div
                v-for="a in recommendedCards"
                :key="a.contentId"
                class="card shadow-sm border-0"
              >
                <div class="row g-0">
                  <div v-if="a.imageUrl" class="col-3">
                    <img
                      :src="a.imageUrl"
                      :alt="a.title"
                      class="w-100 h-100 rounded-start"
                      style="object-fit:cover;max-height:120px;"
                      @error="e => e.target.style.display='none'"
                    />
                  </div>
                  <div :class="a.imageUrl ? 'col-9' : 'col-12'">
                    <div class="card-body py-3">
                      <div class="d-flex justify-content-between align-items-start">
                        <div>
                          <h6 class="fw-bold mb-1">{{ a.title }}</h6>
                          <p v-if="a.address" class="small text-muted mb-1">{{ a.address }}</p>
                        </div>
                        <button
                          class="btn btn-sm btn-indigo ms-2 flex-shrink-0"
                          @click="openAddModal(a)"
                        >여행에 추가</button>
                      </div>
                      <p v-if="a.overview" class="small text-muted mb-0 mt-1" style="display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
                        {{ a.overview }}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <AddToTripModal
      :show="showAddModal"
      :attraction="selectedAttraction"
      @close="showAddModal = false"
    />
  </main>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { marked } from 'marked'
import { aiApi, attractionApi } from '../api/index.js'
import { useToastStore } from '../stores/toast.js'
import AddToTripModal from '../components/AddToTripModal.vue'

marked.setOptions({ breaks: true, gfm: true })

const toastStore = useToastStore()

const sidos = ref([])
const guguns = ref([])
const selectedSido = ref('')
const selectedGugun = ref('')
const loading = ref(false)
const error = ref('')
const answer = ref('')
const resources = ref([])

const showAddModal = ref(false)
const selectedAttraction = ref(null)

const chips = ['서울 여행지 추천해줘', '부산 바다 근처 관광지 알려줘', '제주도 2박 3일 일정 짜줘', '경주 역사 여행 추천해줘']

const form = reactive({ message: '' })

const renderedAnswer = computed(() => answer.value ? marked(answer.value) : '')

// resources에서 관광지 전체 목록 추출
const attractionCards = computed(() => {
  const items = []
  for (const r of resources.value) {
    const data = r.data
    if (!data) continue
    if (Array.isArray(data.items)) items.push(...data.items)
  }
  return items.filter(a => a.contentId && a.title)
})

// AI 응답 텍스트에서 언급된 관광지만 필터링 → 없으면 전체 표시
const showingAll = ref(false)
const recommendedCards = computed(() => {
  if (!attractionCards.value.length) return []
  if (!answer.value) return []

  const text = answer.value
  const mentioned = attractionCards.value.filter(a =>
    text.includes(a.title) ||
    // 2글자 이상 앞부분 매칭 (예: "한라산 국립공원" → "한라산"으로도 검색)
    (a.title.length > 2 && text.includes(a.title.substring(0, Math.ceil(a.title.length * 0.6))))
  )

  if (mentioned.length > 0) {
    showingAll.value = false
    return mentioned
  }
  // AI가 아무 관광지도 언급 안 했으면 상위 6개 표시
  showingAll.value = true
  return attractionCards.value.slice(0, 6)
})

onMounted(async () => {
  try {
    const res = await attractionApi.sidos()
    sidos.value = res.data
  } catch {}
})

async function onSidoChange() {
  selectedGugun.value = ''
  guguns.value = []
  if (!selectedSido.value) return
  try {
    const res = await attractionApi.guguns(selectedSido.value)
    guguns.value = res.data
  } catch {}
}

async function ask() {
  if (!form.message.trim()) {
    toastStore.show('질문을 입력하세요.', 'warning')
    return
  }
  loading.value = true
  error.value = ''
  answer.value = ''
  resources.value = []
  try {
    const { data } = await aiApi.chat({
      message: form.message,
      sidoCode: selectedSido.value || null,
      gugunCode: selectedGugun.value || null,
      contentTypeId: null,
      keyword: null
    })
    answer.value = data.answer || data.summary || ''
    resources.value = data.resources || []
  } catch (e) {
    error.value = e.response?.data?.message || 'AI 응답 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

function openAddModal(attraction) {
  selectedAttraction.value = {
    contentId: attraction.contentId,
    title: attraction.title,
    lat: attraction.lat,
    lng: attraction.lng
  }
  showAddModal.value = true
}
</script>
