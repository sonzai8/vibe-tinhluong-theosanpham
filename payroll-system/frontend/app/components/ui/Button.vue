<template>
  <button
    :type="type"
    :disabled="loading || disabled"
    class="btn-primary relative"
    :class="{ 'opacity-80 cursor-not-allowed': loading || disabled }"
  >
    <div v-if="loading" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></div>
    <slot />
  </button>
</template>

<script setup>
defineProps({
  type: { type: String, default: 'button' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false }
});
</script>
