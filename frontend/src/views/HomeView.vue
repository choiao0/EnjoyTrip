<template>
  <section class="hero-minimal d-flex align-items-center text-center">
    <div class="container">
      <h1 class="display-3 fw-bold mb-3">대한민국 구석구석, EnjoyTrip</h1>
      <p class="lead text-muted mb-5">전국 관광지 탐색부터 실시간 그룹 계획, 최적 경로 추천까지 한 번에</p>
      <form
        class="search-bar-modern mx-auto shadow border rounded-pill bg-white"
        style="max-width:640px; display:flex; flex-direction:row; align-items:center; padding:0.4rem 0.5rem;"
        @submit.prevent="handleSearch"
      >
        <input
          v-model="keyword"
          type="text"
          class="border-0 bg-transparent ps-4"
          style="flex:1; min-width:0; outline:none; box-shadow:none; font-size:1rem; height:44px;"
          placeholder="어디로 떠나시나요?"
          @keyup.enter="handleSearch"
        >
        <button class="btn btn-indigo rounded-pill" type="submit" style="white-space:nowrap; padding:0.6rem 1.8rem; font-size:1rem;">검색</button>
      </form>
    </div>
  </section>

  <section class="py-5">
    <div class="container">
      <div class="row g-4">
        <div class="col-md-4">
          <div class="feature-card p-4 h-100">
            <h4 class="fw-bold mb-3">관광 정보 조회</h4>
            <p class="text-muted mb-4">지역, 키워드, 콘텐츠 유형 기준으로 여행지를 조회하고 바로 일정에 담을 수 있습니다.</p>
            <router-link class="btn btn-outline-secondary" to="/attractions">바로가기</router-link>
          </div>
        </div>
        <div class="col-md-4">
          <div class="feature-card p-4 h-100">
            <h4 class="fw-bold mb-3">여행 계획 저장</h4>
            <p class="text-muted mb-4">개인 일정을 저장하거나 그룹 여행을 만들어 함께 장소를 추가하세요.</p>
            <router-link class="btn btn-outline-secondary" to="/trips">바로가기</router-link>
          </div>
        </div>
        <div class="col-md-4">
          <div class="feature-card p-4 h-100">
            <h4 class="fw-bold mb-3">커뮤니티 공유</h4>
            <p class="text-muted mb-4">핫플레이스, 여행정보공유, 공지사항을 한 곳에서 편리하게 이용하세요.</p>
            <router-link class="btn btn-outline-secondary" to="/boards">바로가기</router-link>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const keyword = ref('')
const router = useRouter()

function handleSearch() {
  router.push({ path: '/attractions', query: keyword.value ? { keyword: keyword.value } : {} })
}
</script>
