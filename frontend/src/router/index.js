import { createRouter, createWebHashHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const routes = [
  { path: '/', component: HomeView },
  { path: '/attractions', component: () => import('../views/AttractionsView.vue') },
  { path: '/trips', component: () => import('../views/TripListView.vue') },
  { path: '/trips/:id', component: () => import('../views/TripDetailView.vue') },
  { path: '/ai', component: () => import('../views/AiTravelView.vue') },
  { path: '/hotplaces', component: () => import('../views/HotplacesView.vue') },
  { path: '/boards', component: () => import('../views/board/BoardListView.vue') },
  { path: '/boards/new', component: () => import('../views/board/BoardFormView.vue') },
  { path: '/boards/:id', component: () => import('../views/board/BoardDetailView.vue') },
  { path: '/boards/:id/edit', component: () => import('../views/board/BoardFormView.vue') },
  { path: '/notices', component: () => import('../views/notice/NoticeListView.vue') },
  { path: '/notices/new', component: () => import('../views/notice/NoticeFormView.vue') },
  { path: '/notices/:id', component: () => import('../views/notice/NoticeDetailView.vue') },
  { path: '/notices/:id/edit', component: () => import('../views/notice/NoticeFormView.vue') },
  { path: '/groups', component: () => import('../views/group/GroupTripListView.vue') },
  { path: '/groups/:id', component: () => import('../views/group/GroupTripDetailView.vue') },
  { path: '/auth/login', component: () => import('../views/auth/LoginView.vue') },
  { path: '/auth/register', component: () => import('../views/auth/RegisterView.vue') },
  { path: '/auth/mypage', component: () => import('../views/auth/MypageView.vue') },
  { path: '/auth/find-password', component: () => import('../views/auth/FindPasswordView.vue') }
]

export default createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})
