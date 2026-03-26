<template>
  <div v-if="show" class="fixed inset-0 z-[200] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-md">
    <div class="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-300 border border-white/20">
      <!-- Icon & Header -->
      <div class="p-8 text-center space-y-4">
        <div :class="[
          'w-20 h-20 rounded-full flex items-center justify-center mx-auto ring-8',
          variant === 'danger' ? 'bg-red-50 ring-red-50/50' : 'bg-primary-50 ring-primary-50/50'
        ]">
          <component 
            :is="icon || (variant === 'danger' ? Trash2 : AlertCircle)" 
            :class="[
              'w-10 h-10',
              variant === 'danger' ? 'text-red-500' : 'text-primary-600'
            ]" 
          />
        </div>
        <div class="space-y-2">
          <h3 class="text-2xl font-black text-slate-900 tracking-tight">{{ title || $t('common.confirm_title') }}</h3>
          <p class="text-slate-500 font-medium leading-relaxed">
            {{ message || $t('common.confirm_message') }}
          </p>
        </div>
      </div>

      <!-- Footer Actions -->
      <div class="p-6 bg-slate-50/50 border-t border-slate-100 flex gap-3">
        <UiButton 
          variant="outline" 
          class="flex-1 !rounded-2xl !py-3.5 font-black shadow-sm"
          @click="$emit('cancel')"
        >
          {{ cancelText || $t('common.cancel') }}
        </UiButton>
        <UiButton 
          :variant="variant === 'danger' ? 'danger' : 'primary'" 
          class="flex-[1.5] !rounded-2xl !py-3.5 font-black shadow-lg shadow-primary-100"
          :class="variant === 'danger' ? 'bg-red-600 hover:bg-red-700 text-white border-none' : ''"
          @click="$emit('confirm')"
          :loading="loading"
        >
          {{ confirmText || $t('common.confirm') }}
        </UiButton>
      </div>
    </div>
  </div>
</template>

<script setup>
import { AlertCircle, Trash2, HelpCircle } from 'lucide-vue-next';

defineProps({
  show: Boolean,
  title: String,
  message: String,
  variant: {
    type: String,
    default: 'primary' // 'primary' | 'danger'
  },
  icon: Object, // Lucide icon component
  confirmText: String,
  cancelText: String,
  loading: Boolean
});

defineEmits(['confirm', 'cancel']);
</script>
