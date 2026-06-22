<template>
  <nav class="navbar navbar-expand-lg sticky-top border-bottom bg-white py-3">
    <div class="container">
      <router-link class="navbar-brand fw-bold text-indigo" to="/">enjoy<span>TRIP</span></router-link>
      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarNav">
        <ul class="navbar-nav ms-auto gap-2 align-items-lg-center">
          <li class="nav-item">
            <router-link class="nav-link" active-class="active fw-bold" to="/attractions">지역별 관광정보</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" active-class="active fw-bold" to="/plans">나의 여행계획</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" active-class="active fw-bold" to="/ai">AI 여행도우미</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" active-class="active fw-bold" to="/hotplaces">핫플레이스</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" active-class="active fw-bold" to="/notices">공지사항</router-link>
          </li>
          <li class="nav-item">
            <router-link class="nav-link" active-class="active fw-bold" to="/boards">여행정보공유</router-link>
          </li>
          <template v-if="authStore.user">
            <li class="nav-item">
              <router-link class="btn btn-sm btn-outline-secondary" to="/auth/mypage">
                {{ authStore.user.name }}
              </router-link>
            </li>
            <li class="nav-item">
              <button class="btn btn-sm btn-indigo" @click="handleLogout">로그아웃</button>
            </li>
          </template>
          <template v-else>
            <li class="nav-item">
              <router-link class="btn btn-sm btn-outline-secondary" to="/auth/login">로그인</router-link>
            </li>
            <li class="nav-item">
              <router-link class="btn btn-sm btn-indigo" to="/auth/register">회원가입</router-link>
            </li>
          </template>
        </ul>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { useAuthStore } from '../stores/auth.js'
import { useToastStore } from '../stores/toast.js'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const toastStore = useToastStore()
const router = useRouter()

async function handleLogout() {
  try {
    await authStore.logout()
    toastStore.show('로그아웃되었습니다.')
    router.push('/')
  } catch {
    toastStore.show('오류가 발생했습니다.', 'danger')
  }
}
</script>
