<template>
  <teleport to="body">
    <div v-if="show" class="modal-backdrop-custom" @click.self="$emit('close')">
      <div class="card shadow p-4" style="width:480px;max-width:92vw;max-height:80vh;display:flex;flex-direction:column;">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <h5 class="fw-bold mb-0">어느 여행에 추가할까요?</h5>
          <button class="btn-close" @click="$emit('close')"></button>
        </div>

        <p class="text-muted small mb-3">
          <strong>{{ attraction?.title }}</strong>을(를) 추가할 여행을 선택하세요.
        </p>

        <div v-if="loading" class="text-center py-3">
          <div class="spinner-border spinner-border-sm"></div>
        </div>

        <div v-else style="overflow-y:auto;flex:1;">
          <div v-if="allTrips.length" class="d-flex flex-column gap-2 mb-3">
            <div
              v-for="trip in allTrips"
              :key="trip._key"
              :class="['border rounded-3 p-3', selected?._key === trip._key ? 'border-primary bg-light' : '']"
              style="cursor:pointer;"
              @click="selected = trip; showNewForm = false"
            >
              <div class="d-flex justify-content-between align-items-center">
                <div>
                  <span class="fw-semibold">{{ trip.title }}</span>
                  <span :class="['badge ms-2', trip._type === 'group' ? 'bg-success' : 'bg-primary']">
                    {{ trip._type === 'group' ? '그룹' : '개인' }}
                  </span>
                </div>
                <span v-if="selected?._key === trip._key" class="text-primary fw-bold">✓</span>
              </div>
            </div>
          </div>
          <p v-else class="text-muted small mb-3">저장된 여행이 없습니다.</p>

          <!-- 새 여행 만들기 -->
          <div :class="['border rounded-3 p-3 mb-3', showNewForm ? 'border-primary bg-light' : '']">
            <button
              v-if="!showNewForm"
              class="btn btn-outline-secondary w-100"
              @click="showNewForm = true; selected = { _type: 'new' }"
            >+ 새 여행 만들기</button>
            <div v-else>
              <p class="small fw-semibold mb-2 text-primary">새 여행 만들기</p>
              <input
                v-model="newTitle"
                class="form-control mb-2"
                placeholder="여행 이름 *"
                @keyup.enter="doConfirm"
              />
              <button
                class="btn btn-sm btn-outline-secondary"
                @click="showNewForm = false; selected = null; newTitle = ''"
              >취소</button>
            </div>
          </div>
        </div>

        <button
          class="btn btn-indigo w-100 mt-2"
          :disabled="!selected || adding || (selected._type === 'new' && !newTitle.trim())"
          @click="doConfirm"
        >
          {{ adding ? '추가 중...' : '추가하기' }}
        </button>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { planApi, groupTripApi } from '../api/index.js'
import { useAuthStore } from '../stores/auth.js'
import { useToastStore } from '../stores/toast.js'

const props = defineProps({
  show: Boolean,
  attraction: Object  // { contentId, title, lat, lng }
})
const emit = defineEmits(['close', 'added'])

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

const loading = ref(false)
const adding = ref(false)
const personalPlans = ref([])
const groups = ref([])
const selected = ref(null)
const showNewForm = ref(false)
const newTitle = ref('')

const allTrips = computed(() => [
  ...personalPlans.value.map(p => ({ ...p, _type: 'personal', _key: `p-${p.id}` })),
  ...groups.value.map(g => ({ ...g, _type: 'group', _key: `g-${g.id}` }))
])

watch(() => props.show, async (val) => {
  if (!val) {
    selected.value = null
    showNewForm.value = false
    newTitle.value = ''
    return
  }
  if (!authStore.user) {
    toastStore.show('로그인이 필요합니다.', 'warning')
    emit('close')
    router.push('/auth/login')
    return
  }
  loading.value = true
  const [plansRes, groupsRes] = await Promise.allSettled([planApi.list(), groupTripApi.list()])
  if (plansRes.status === 'fulfilled') personalPlans.value = plansRes.value.data
  if (groupsRes.status === 'fulfilled') groups.value = groupsRes.value.data
  loading.value = false
})

async function doConfirm() {
  if (!selected.value || adding.value) return
  if (!authStore.user) {
    toastStore.show('로그인이 필요합니다.', 'warning')
    emit('close')
    router.push('/auth/login')
    return
  }
  if (!props.attraction) {
    toastStore.show('관광지 정보를 찾을 수 없습니다.', 'warning')
    return
  }
  if (selected.value._type === 'new' && !newTitle.value.trim()) {
    toastStore.show('여행 이름을 입력하세요.', 'warning')
    return
  }
  adding.value = true
  try {
    const a = props.attraction
    const item = { contentId: String(a.contentId), title: a.title, lat: a.lat, lng: a.lng }

    if (selected.value._type === 'new') {
      await planApi.save({ title: newTitle.value.trim(), items: [item] })
      toastStore.show(`'${newTitle.value.trim()}' 여행이 만들어지고 '${a.title}'이(가) 추가됐습니다.`)
    } else if (selected.value._type === 'personal') {
      await planApi.addItem(selected.value.id, item)
      toastStore.show(`'${a.title}'을(를) '${selected.value.title}'에 추가했습니다.`)
    } else {
      await groupTripApi.addPlace(selected.value.id, item)
      toastStore.show(`'${a.title}'을(를) '${selected.value.title}' 그룹에 추가했습니다.`)
    }

    emit('added')
    emit('close')
  } catch (e) {
    console.error('[AddToTripModal] doConfirm error:', e)
    toastStore.show(e.response?.data?.error || '추가에 실패했습니다.', 'danger')
  } finally {
    adding.value = false
  }
}
</script>

<style scoped>
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1060;
}
</style>
