<template>
  <section class="hero-minimal d-flex align-items-center text-center">
    <div class="container">
      <h1 class="display-3 fw-bold mb-3">대한민국 구석구석, EnjoyTrip</h1>
      <p class="lead text-muted mb-5">공공데이터와 경로 추천으로 완성하는 나만의 여행 일정</p>
      <form
        class="search-bar-modern mx-auto shadow border rounded-pill d-flex p-2 bg-white"
        style="max-width:640px;"
        @submit.prevent="handleSearch"
      >
        <input
          v-model="keyword"
          type="text"
          class="form-control border-0 bg-transparent ps-4"
          placeholder="어디로 떠나시나요?"
        >
        <button class="btn btn-indigo rounded-pill px-4" type="submit">검색</button>
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
