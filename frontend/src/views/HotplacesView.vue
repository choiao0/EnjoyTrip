<template>
  <section class="py-5 container">
    <div class="row g-4">
      <!-- 등록 폼 -->
      <div class="col-lg-5">
        <div class="p-4 border rounded-4 bg-white shadow-sm">
          <h4 class="fw-bold mb-4">나만의 핫플 자랑하기</h4>
          <form @submit.prevent="submitHotplace">
            <div
              id="uploadZone"
              class="upload-zone mb-3"
              :class="{ dragover: isDragging }"
              @click="fileInput.click()"
              @dragenter.prevent="isDragging = true"
              @dragover.prevent="isDragging = true"
              @dragleave.prevent="isDragging = false"
              @drop.prevent="onDrop"
            >
              <i class="bi bi-cloud-arrow-up fs-1"></i>
              <p class="mb-1 fw-bold">사진 등록</p>
              <small class="text-muted d-block">클릭하거나 파일을 드래그해 주세요</small>
            </div>
            <input ref="fileInput" type="file" accept="image/*" class="d-none" @change="onFileChange">
            <div v-if="previewUrl" class="mb-3">
              <img :src="previewUrl" class="img-fluid rounded-4" style="max-height:240px;object-fit:cover;width:100%;" alt="미리보기">
            </div>
            <input v-model="form.name" type="text" class="form-control mb-2" placeholder="장소명" required>
            <textarea v-model="form.description" class="form-control mb-3" placeholder="상세 설명" required></textarea>
            <button class="btn btn-indigo w-100" type="submit" :disabled="submitting">
              {{ submitting ? '등록 중...' : '등록' }}
            </button>
          </form>
        </div>
      </div>

      <!-- 핫플레이스 목록 -->
      <div class="col-lg-7">
        <div class="row row-cols-1 row-cols-md-2 g-3">
          <div v-if="hotplaces.length === 0" class="col-12 text-center text-muted p-5">
            등록된 핫플레이스가 없습니다.
          </div>
          <div v-for="hp in hotplaces" :key="hp.id" class="col">
            <div class="card border-0 shadow-sm rounded-4 h-100 overflow-hidden">
              <img :src="`/uploads/${hp.imagePath}`" :alt="hp.name" style="height:220px;object-fit:cover;">
              <div class="card-body">
                <h5 class="fw-bold">{{ hp.name }}</h5>
                <div class="small text-muted mb-2">{{ hp.userName }} · {{ hp.createdAt }}</div>
                <p class="small mb-3">{{ hp.description }}</p>
                <button
                  v-if="authStore.user?.id === hp.userId"
                  class="btn btn-sm btn-outline-danger"
                  @click="deleteHotplace(hp.id)"
                >삭제</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { hotplaceApi } from '../api/index.js'
import { useAuthStore } from '../stores/auth.js'
import { useToastStore } from '../stores/toast.js'

const authStore = useAuthStore()
const toastStore = useToastStore()

const hotplaces = ref([])
const fileInput = ref(null)
const previewUrl = ref(null)
const selectedFile = ref(null)
const isDragging = ref(false)
const submitting = ref(false)

const form = ref({ name: '', description: '' })

onMounted(loadHotplaces)

async function loadHotplaces() {
  const res = await hotplaceApi.list()
  hotplaces.value = res.data
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

async function submitHotplace() {
  if (!authStore.user) {
    toastStore.show('로그인이 필요합니다.', 'warning')
    return
  }
  if (!selectedFile.value) {
    toastStore.show('사진을 선택해 주세요.', 'warning')
    return
  }
  submitting.value = true
  try {
    const fd = new FormData()
    fd.append('name', form.value.name)
    fd.append('description', form.value.description)
    fd.append('image', selectedFile.value)
    await hotplaceApi.create(fd)
    form.value = { name: '', description: '' }
    previewUrl.value = null
    selectedFile.value = null
    await loadHotplaces()
    toastStore.show('핫플레이스가 등록되었습니다.')
  } catch {
    toastStore.show('등록 중 오류가 발생했습니다.', 'danger')
  } finally {
    submitting.value = false
  }
}

async function deleteHotplace(id) {
  if (!confirm('삭제하시겠습니까?')) return
  try {
    await hotplaceApi.delete(id)
    await loadHotplaces()
    toastStore.show('삭제되었습니다.')
  } catch {
    toastStore.show('삭제 중 오류가 발생했습니다.', 'danger')
  }
}
</script>
