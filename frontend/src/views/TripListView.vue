<template>
  <section class="py-5 container">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h2 class="fw-bold">여행 목록</h2>
      <div class="d-flex gap-2">
        <button class="btn btn-outline-secondary" @click="openJoinModal">그룹 참가하기</button>
        <button class="btn btn-dark" @click="showModal = true">+ 새 여행 만들기</button>
      </div>
    </div>

    <!-- 탭 -->
    <ul class="nav nav-tabs mb-4">
      <li class="nav-item">
        <button :class="['nav-link', tab === 'all' ? 'active' : '']" @click="tab = 'all'">
          전체 ({{ allTrips.length }})
        </button>
      </li>
      <li class="nav-item">
        <button :class="['nav-link', tab === 'personal' ? 'active' : '']" @click="tab = 'personal'">
          개인 ({{ personalPlans.length }})
        </button>
      </li>
      <li class="nav-item">
        <button :class="['nav-link', tab === 'group' ? 'active' : '']" @click="tab = 'group'">
          그룹 ({{ groups.length }})
        </button>
      </li>
    </ul>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border" role="status"></div>
    </div>
    <div v-else-if="!authStore.user" class="text-center text-muted py-5">
      <p>여행 목록을 보려면 <router-link to="/auth/login">로그인</router-link>이 필요합니다.</p>
    </div>
    <div v-else-if="filteredTrips.length === 0" class="text-center text-muted py-5">
      여행이 없습니다. 새 여행을 만들어보세요.
    </div>
    <div v-else class="row g-3">
      <div v-for="item in filteredTrips" :key="item._key" class="col-md-6 col-lg-4">
        <!-- 개인 여행 카드 -->
        <div
          v-if="item._type === 'personal'"
          class="card h-100 shadow-sm"
          style="cursor:pointer;"
          @click="router.push(`/trips/${item.id}`)"
        >
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <h5 class="card-title fw-bold mb-0">{{ item.title }}</h5>
              <span class="badge bg-primary">개인</span>
            </div>
            <p class="card-text text-muted small mb-2">{{ item.memo || '메모 없음' }}</p>
            <p class="card-text small text-muted mb-1">{{ item.createdAt }}</p>
            <span class="badge bg-light text-dark border">장소 {{ item.items?.length ?? 0 }}개</span>
          </div>
        </div>
        <!-- 그룹 여행 카드 -->
        <div
          v-else
          class="card h-100 shadow-sm"
          style="cursor:pointer;"
          @click="router.push(`/groups/${item.id}`)"
        >
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <h5 class="card-title fw-bold mb-0">{{ item.title }}</h5>
              <span class="badge bg-success">그룹</span>
            </div>
            <p class="card-text text-muted small mb-2">{{ item.description || '설명 없음' }}</p>
            <p class="card-text small text-muted mb-1">{{ item.createdAt }}</p>
            <span class="badge bg-light text-dark border">그룹장: {{ item.hostUserName }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 새 여행 만들기 모달 -->
    <teleport to="body">
      <div v-if="showModal" class="modal-backdrop-custom" @click.self="closeModal">
        <div class="card shadow p-4" style="width:420px;max-width:92vw;">
          <h5 class="fw-bold mb-3">새 여행 만들기</h5>

          <div v-if="!modalMode" class="d-grid gap-2">
            <button class="btn btn-outline-primary py-3 text-start" @click="modalMode = 'personal'">
              <div class="fw-semibold">개인 여행</div>
              <div class="small text-muted">나만의 장소를 골라 일정을 저장합니다</div>
            </button>
            <button class="btn btn-outline-success py-3 text-start" @click="modalMode = 'group'">
              <div class="fw-semibold">그룹 여행</div>
              <div class="small text-muted">여러 명이 함께 장소를 추가합니다</div>
            </button>
          </div>

          <div v-else-if="modalMode === 'personal'">
            <div class="mb-3">
              <input
                v-model="newPersonalTitle"
                class="form-control"
                placeholder="여행 이름 *"
                @keyup.enter="handleCreatePersonal"
              />
            </div>
            <div class="d-flex gap-2">
              <button
                class="btn btn-indigo flex-grow-1"
                :disabled="!newPersonalTitle.trim() || creatingPersonal"
                @click="handleCreatePersonal"
              >
                {{ creatingPersonal ? '생성 중...' : '여행 만들기' }}
              </button>
              <button class="btn btn-outline-secondary" @click="modalMode = null">뒤로</button>
            </div>
          </div>

          <div v-else-if="modalMode === 'group'">
            <div class="mb-3">
              <input v-model="newGroupTitle" class="form-control mb-2" placeholder="그룹 이름 *" />
              <textarea v-model="newGroupDesc" class="form-control" rows="2" placeholder="설명 (선택)" />
            </div>
            <div class="d-flex gap-2">
              <button class="btn btn-indigo flex-grow-1" :disabled="creatingGroup" @click="handleCreateGroup">
                {{ creatingGroup ? '생성 중...' : '그룹 만들기' }}
              </button>
              <button class="btn btn-outline-secondary" @click="modalMode = null">뒤로</button>
            </div>
          </div>

          <button class="btn btn-sm btn-outline-secondary mt-3 w-100" @click="closeModal">취소</button>
        </div>
      </div>
    </teleport>

    <!-- 그룹 참가하기 모달 -->
    <teleport to="body">
      <div v-if="showJoinModal" class="modal-backdrop-custom" @click.self="showJoinModal = false">
        <div class="card shadow p-4" style="width:380px;max-width:92vw;">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <h5 class="fw-bold mb-0">그룹 참가하기</h5>
            <button class="btn-close" @click="showJoinModal = false"></button>
          </div>
          <p class="text-muted small mb-3">그룹장에게 받은 6자리 초대 코드를 입력하세요.</p>
          <input
            v-model="joinCode"
            class="form-control mb-3 text-center fw-bold fs-5 text-uppercase"
            placeholder="예: AB12CD"
            maxlength="6"
            @keyup.enter="handleJoinByCode"
          />
          <button class="btn btn-indigo w-100" :disabled="joinCode.length < 6 || joiningByCode" @click="handleJoinByCode">
            {{ joiningByCode ? '참가 중...' : '참가하기' }}
          </button>
        </div>
      </div>
    </teleport>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { planApi, groupTripApi } from '../api/index.js'
import { useAuthStore } from '../stores/auth.js'
import { useGroupTripStore } from '../stores/groupTrip.js'
import { useToastStore } from '../stores/toast.js'

const router = useRouter()
const authStore = useAuthStore()
const groupStore = useGroupTripStore()
const toastStore = useToastStore()

const loading = ref(true)
const tab = ref('all')
const personalPlans = ref([])
const groups = ref([])

const showModal = ref(false)
const modalMode = ref(null)
const newPersonalTitle = ref('')
const creatingPersonal = ref(false)
const newGroupTitle = ref('')
const newGroupDesc = ref('')
const creatingGroup = ref(false)

const showJoinModal = ref(false)
const joinCode = ref('')
const joiningByCode = ref(false)

const allTrips = computed(() => [
  ...personalPlans.value.map(p => ({ ...p, _type: 'personal', _key: `p-${p.id}` })),
  ...groups.value.map(g => ({ ...g, _type: 'group', _key: `g-${g.id}` }))
])

const filteredTrips = computed(() => {
  if (tab.value === 'personal') return allTrips.value.filter(t => t._type === 'personal')
  if (tab.value === 'group') return allTrips.value.filter(t => t._type === 'group')
  return allTrips.value
})

onMounted(async () => {
  if (!authStore.user) { loading.value = false; return }
  const [plansRes, groupsRes] = await Promise.allSettled([
    planApi.list(),
    groupTripApi.list()
  ])
  if (plansRes.status === 'fulfilled') personalPlans.value = plansRes.value.data
  if (groupsRes.status === 'fulfilled') groups.value = groupsRes.value.data
  loading.value = false
})

function closeModal() {
  showModal.value = false
  modalMode.value = null
  newPersonalTitle.value = ''
  newGroupTitle.value = ''
  newGroupDesc.value = ''
}

async function handleCreatePersonal() {
  if (!newPersonalTitle.value.trim()) {
    toastStore.show('여행 이름을 입력하세요.', 'warning')
    return
  }
  creatingPersonal.value = true
  try {
    const res = await planApi.save({ title: newPersonalTitle.value.trim(), items: [] })
    closeModal()
    router.push(`/trips/${res.data.id}`)
  } catch (e) {
    toastStore.show(e.response?.data?.error || '여행 생성에 실패했습니다.', 'danger')
  } finally {
    creatingPersonal.value = false
  }
}

async function handleCreateGroup() {
  if (!newGroupTitle.value.trim()) {
    toastStore.show('그룹 이름을 입력하세요.', 'warning')
    return
  }
  creatingGroup.value = true
  try {
    const group = await groupStore.createGroup(newGroupTitle.value, newGroupDesc.value)
    closeModal()
    router.push(`/groups/${group.id}`)
  } catch (e) {
    toastStore.show(e.response?.data?.error || '그룹 생성에 실패했습니다.', 'danger')
  } finally {
    creatingGroup.value = false
  }
}

function openJoinModal() {
  if (!authStore.user) {
    toastStore.show('로그인이 필요합니다.', 'warning')
    router.push('/auth/login')
    return
  }
  showJoinModal.value = true
}

async function handleJoinByCode() {
  const code = joinCode.value.trim().toUpperCase()
  if (code.length < 6) return
  joiningByCode.value = true
  try {
    const res = await groupTripApi.joinByCode(code)
    const group = res.data.group
    showJoinModal.value = false
    joinCode.value = ''
    toastStore.show(`'${group.title}' 그룹에 참가했습니다.`)
    router.push(`/groups/${group.id}`)
  } catch (e) {
    toastStore.show(e.response?.data?.error || '참가에 실패했습니다.', 'danger')
  } finally {
    joiningByCode.value = false
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
  z-index: 1050;
}
</style>
