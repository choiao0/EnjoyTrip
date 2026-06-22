<template>
  <NavBar />
  <AppToast />
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import NavBar from './components/NavBar.vue'
import AppToast from './components/AppToast.vue'
import { useAuthStore } from './stores/auth.js'

const authStore = useAuthStore()
onMounted(() => authStore.fetchMe())
</script>
