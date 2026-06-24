<template>
  <section class="py-5 container">
    <div v-if="!authStore.user" class="text-center p-5 text-muted">로그인이 필요합니다.</div>
    <div v-else class="row g-4">
      <div class="col-lg-7">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-4">
            <h2 class="fw-bold mb-4">회원 정보 수정</h2>
            <form @submit.prevent="updateProfile">
              <div class="mb-3">
                <label class="form-label">아이디</label>
                <input type="text" class="form-control" :value="authStore.user.id" readonly>
              </div>
              <div class="mb-3">
                <label class="form-label">이름</label>
                <input v-model="profileForm.name" type="text" class="form-control">
              </div>
              <div class="mb-3">
                <label class="form-label">현재 비밀번호</label>
                <input v-model="profileForm.currentPassword" type="password" class="form-control">
              </div>
              <div class="mb-3">
                <label class="form-label">새 비밀번호</label>
                <input v-model="profileForm.newPassword" type="password" class="form-control">
              </div>
              <button class="btn btn-indigo" type="submit" :disabled="saving">
                {{ saving ? '저장 중...' : '수정 저장' }}
              </button>
            </form>
          </div>
        </div>
      </div>
      <!-- 관리자 패널 -->
      <div v-if="authStore.user?.role === 'ADMIN'" class="col-12">
        <div class="card border-0 shadow-sm rounded-4 border-start border-4 border-warning">
          <div class="card-body p-4">
            <h4 class="fw-bold mb-1">관리자 패널</h4>
            <p class="text-muted small mb-3">유저 ID를 입력해 관리자 권한을 부여하거나 회수합니다.</p>
            <div class="d-flex gap-2 align-items-end flex-wrap">
              <div class="flex-grow-1">
                <label class="form-label small fw-semibold">유저 ID</label>
                <input v-model="adminTargetId" class="form-control" placeholder="예: user123">
              </div>
              <button class="btn btn-warning" :disabled="!adminTargetId.trim() || changingRole" @click="promoteToAdmin">
                {{ changingRole ? '처리 중...' : '관리자로 승격' }}
              </button>
              <button class="btn btn-outline-secondary" :disabled="!adminTargetId.trim() || changingRole" @click="demoteToUser">
                {{ changingRole ? '처리 중...' : '일반 유저로 변경' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="col-lg-5">
        <div class="card border-0 shadow-sm rounded-4">
          <div class="card-body p-4">
            <h4 class="fw-bold mb-3">회원 탈퇴</h4>
            <p class="text-muted small mb-3">현재 비밀번호를 입력하면 계정이 삭제되고 로그인 상태가 해제됩니다.</p>
            <form @submit.prevent="deleteAccount">
              <input v-model="deletePassword" type="password" class="form-control mb-3" placeholder="현재 비밀번호">
              <button class="btn btn-outline-danger w-100" type="submit" :disabled="deleting">
                {{ deleting ? '처리 중...' : '회원 탈퇴' }}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../../api/index.js'
import { useAuthStore } from '../../stores/auth.js'
import { useToastStore } from '../../stores/toast.js'

const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

const saving = ref(false)
const deleting = ref(false)
const deletePassword = ref('')
const adminTargetId = ref('')
const changingRole = ref(false)

const profileForm = ref({ name: '', currentPassword: '', newPassword: '' })

onMounted(() => {
  if (authStore.user) {
    profileForm.value.name = authStore.user.name
  }
})

async function updateProfile() {
  saving.value = true
  try {
    const res = await authApi.updateProfile(
      profileForm.value.name,
      profileForm.value.currentPassword,
      profileForm.value.newPassword
    )
    authStore.user = res.data
    profileForm.value.currentPassword = ''
    profileForm.value.newPassword = ''
    toastStore.show('프로필이 수정되었습니다.')
  } catch (e) {
    const msg = e.response?.data?.message || '수정 중 오류가 발생했습니다.'
    toastStore.show(msg, 'danger')
  } finally {
    saving.value = false
  }
}

async function changeRole(role) {
  const id = adminTargetId.value.trim()
  if (!id) return
  changingRole.value = true
  try {
    const res = await authApi.changeRole(id, role)
    toastStore.show(res.data.message)
    adminTargetId.value = ''
  } catch (e) {
    toastStore.show(e.response?.data?.error || '처리 중 오류가 발생했습니다.', 'danger')
  } finally {
    changingRole.value = false
  }
}

function promoteToAdmin() { changeRole('ADMIN') }
function demoteToUser() { changeRole('USER') }

async function deleteAccount() {
  if (!confirm('정말로 탈퇴하시겠습니까?')) return
  deleting.value = true
  try {
    await authApi.deleteAccount(deletePassword.value)
    authStore.user = null
    toastStore.show('회원 탈퇴가 완료되었습니다.')
    router.push('/')
  } catch (e) {
    const msg = e.response?.data?.message || '탈퇴 중 오류가 발생했습니다.'
    toastStore.show(msg, 'danger')
  } finally {
    deleting.value = false
  }
}
</script>
