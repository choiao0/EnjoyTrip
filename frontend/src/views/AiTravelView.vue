<template>
  <main class="py-5">
    <div class="container">
      <div class="d-flex flex-wrap align-items-end justify-content-between gap-3 mb-4">
        <div>
          <p class="text-indigo fw-semibold mb-2">Spring AI 구조 검증</p>
          <h1 class="h2 fw-bold mb-1">AI 여행도우미</h1>
          <p class="text-muted mb-0">Client, Tool, Resource, Prompt, Planner 흐름을 기존 관광 데이터와 연결합니다.</p>
        </div>
        <button class="btn btn-outline-secondary" type="button" @click="loadCapabilities">
          Tool 새로고침
        </button>
      </div>

      <div class="row g-4">
        <div class="col-lg-5">
          <section class="ai-panel bg-white border p-4">
            <h2 class="h5 fw-bold mb-3">요청 입력</h2>
            <label class="form-label fw-semibold" for="ai-message">질문</label>
            <textarea
              id="ai-message"
              v-model="form.message"
              class="form-control mb-3"
              rows="4"
              placeholder="예: 서울 여행지 추천해줘. 날씨와 일정 팁도 같이 알려줘."
            />

            <div class="row g-3">
              <div class="col-6">
                <label class="form-label fw-semibold" for="sido-code">시도 코드</label>
                <input id="sido-code" v-model.number="form.sidoCode" class="form-control" type="number">
              </div>
              <div class="col-6">
                <label class="form-label fw-semibold" for="gugun-code">구군 코드</label>
                <input id="gugun-code" v-model.number="form.gugunCode" class="form-control" type="number">
              </div>
              <div class="col-6">
                <label class="form-label fw-semibold" for="content-type">콘텐츠 타입</label>
                <input id="content-type" v-model.number="form.contentTypeId" class="form-control" type="number">
              </div>
              <div class="col-6">
                <label class="form-label fw-semibold" for="keyword">키워드</label>
                <input id="keyword" v-model="form.keyword" class="form-control" type="text">
              </div>
            </div>

            <div class="d-grid gap-2 mt-4">
              <button class="btn btn-indigo" type="button" :disabled="loading" @click="runPlanner">
                Planner 다중 Tool 실행
              </button>
              <button class="btn btn-outline-secondary" type="button" :disabled="loading" @click="runSingleTool">
                단일 관광지 Tool 실행
              </button>
            </div>
          </section>

          <section class="ai-panel bg-white border p-4 mt-4">
            <h2 class="h5 fw-bold mb-3">등록 Tool</h2>
            <div v-if="capabilities.length" class="d-flex flex-column gap-2">
              <div v-for="tool in capabilities" :key="tool.name" class="tool-row border">
                <strong>{{ tool.name }}</strong>
                <span class="text-muted">{{ tool.description }}</span>
              </div>
            </div>
            <p v-else class="text-muted mb-0">등록된 Tool 정보를 불러오세요.</p>
          </section>
        </div>

        <div class="col-lg-7">
          <section class="ai-panel bg-white border p-4">
            <div class="d-flex align-items-center justify-content-between gap-3 mb-3">
              <h2 class="h5 fw-bold mb-0">실행 결과</h2>
              <span v-if="result?.plan" class="badge text-bg-light border">{{ result.plan.intent }}</span>
            </div>

            <div v-if="loading" class="text-muted py-5 text-center">Tool 실행 중입니다.</div>
            <div v-else-if="error" class="alert alert-danger mb-0">{{ error }}</div>
            <div v-else-if="result">
              <pre class="ai-answer mb-4">{{ result.answer || result.summary }}</pre>

              <h3 class="h6 fw-bold">Resource</h3>
              <div class="resource-list mb-4">
                <article v-for="resource in resources" :key="resource.name" class="resource-item border">
                  <strong>{{ resource.name }}</strong>
                  <p class="mb-2 text-muted">{{ resource.summary }}</p>
                  <pre>{{ JSON.stringify(resource.data, null, 2) }}</pre>
                </article>
              </div>

              <h3 v-if="result.prompt" class="h6 fw-bold">Prompt</h3>
              <pre v-if="result.prompt" class="prompt-box mb-0">{{ result.prompt }}</pre>
            </div>
            <div v-else class="text-muted py-5 text-center">왼쪽에서 요청을 실행하면 결과가 표시됩니다.</div>
          </section>
        </div>
      </div>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { aiApi } from '../api/index.js'

const capabilities = ref([])
const result = ref(null)
const loading = ref(false)
const error = ref('')
const form = reactive({
  message: '서울 여행지 추천해줘. 날씨와 일정 팁도 같이 알려줘.',
  sidoCode: 1,
  gugunCode: null,
  contentTypeId: null,
  keyword: '서울'
})

const resources = computed(() => {
  if (!result.value) return []
  if (result.value.resources) return result.value.resources
  return [result.value]
})

function body() {
  return {
    message: form.message,
    sidoCode: form.sidoCode || null,
    gugunCode: form.gugunCode || null,
    contentTypeId: form.contentTypeId || null,
    keyword: form.keyword
  }
}

async function loadCapabilities() {
  const { data } = await aiApi.capabilities()
  capabilities.value = data
}

async function execute(action) {
  loading.value = true
  error.value = ''
  try {
    const { data } = await action()
    result.value = data
  } catch (e) {
    error.value = e.response?.data?.message || 'AI Tool 실행 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

function runPlanner() {
  execute(() => aiApi.chat(body()))
}

function runSingleTool() {
  execute(() => aiApi.executeTool('attractionSearchTool', body()))
}

onMounted(loadCapabilities)
</script>
