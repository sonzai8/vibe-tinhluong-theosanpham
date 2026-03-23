<template>
  <div v-if="show" class="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-md">
    <div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-300 border border-white/20">
      <!-- Icon & Header -->
      <div class="p-8 text-center space-y-4">
        <div class="w-20 h-20 bg-red-50 rounded-full flex items-center justify-center mx-auto ring-8 ring-red-50/50">
          <AlertCircle class="w-10 h-10 text-red-500" />
        </div>
        <div class="space-y-2">
          <h3 class="text-2xl font-black text-slate-900 tracking-tight">{{ title || 'Đã xảy ra lỗi' }}</h3>
          <p class="text-slate-500 font-medium leading-relaxed">
            {{ message || 'Hệ thống gặp sự cố không mong muốn. Vui lòng kiểm tra lại.' }}
          </p>
        </div>
      </div>

      <!-- Detail Section (Optional) -->
      <div v-if="detail" class="px-8 pb-4">
        <div class="bg-slate-50 rounded-2xl p-4 text-left border border-slate-100 max-h-32 overflow-auto">
          <p class="text-xs font-mono text-slate-600 whitespace-pre-wrap break-words leading-relaxed">
            {{ detail }}
          </p>
        </div>
      </div>

      <!-- Footer -->
      <div class="p-8 bg-slate-50/50 border-t border-slate-100 mt-2">
        <UiButton 
          variant="outline" 
          class="w-full !rounded-2xl !py-4 font-black shadow-sm"
          @click="$emit('close')"
        >
          {{ confirmText || 'Đã hiểu' }}
        </UiButton>
      </div>
    </div>
  </div>
</template>

<script setup>
import { AlertCircle } from 'lucide-vue-next';

defineProps({
  show: Boolean,
  title: String,
  message: String,
  detail: String,
  confirmText: String
});

defineEmits(['close']);
</script>
