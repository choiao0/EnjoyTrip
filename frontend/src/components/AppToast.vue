<template>
  <div class="toast-container position-fixed bottom-0 end-0 p-3" style="z-index:1100">
    <div
      v-for="toast in toastStore.toasts"
      :key="toast.id"
      :class="`toast show align-items-center border-0 text-bg-${toast.type}`"
      role="alert"
    >
      <div class="d-flex">
        <div class="toast-body fw-semibold">{{ toast.message }}</div>
        <button
          type="button"
          class="btn-close btn-close-white me-2 m-auto"
          @click="toastStore.dismiss(toast.id)"
        ></button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useToastStore } from '../stores/toast.js'
const toastStore = useToastStore()
</script>
