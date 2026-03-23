<template>
  <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in duration-300">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-white sticky top-0 z-10">
        <div class="flex items-center gap-3">
          <div class="p-2 bg-red-50 rounded-lg text-red-600">
            <AlertCircle class="w-6 h-6" />
          </div>
          <div>
            <h3 class="text-xl font-bold text-gray-900">{{ $t('import.error_title') }}</h3>
            <p class="text-sm text-gray-500">{{ $t('import.error_subtitle', { count: errors.length }) }}</p>
          </div>
        </div>
        <button 
          @click="$emit('close')" 
          class="p-2 hover:bg-gray-100 rounded-full transition-colors"
        >
          <X class="w-5 h-5 text-gray-400" />
        </button>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-auto p-6">
        <div class="overflow-hidden rounded-xl border border-gray-200">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-gray-50 border-b border-gray-200 text-sm font-semibold text-gray-600">
                <th class="px-4 py-3 w-20">{{ $t('import.row') }}</th>
                <th class="px-4 py-3 w-40">{{ $t('import.column') }}</th>
                <th class="px-4 py-3 w-40">{{ $t('import.value') }}</th>
                <th class="px-4 py-3">{{ $t('import.error_message') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100 text-sm">
              <tr v-for="(err, idx) in errors" :key="idx" class="hover:bg-red-50/30 transition-colors">
                <td class="px-4 py-3 font-medium text-gray-900">{{ err.rowNumber }}</td>
                <td class="px-4 py-3 text-gray-600">{{ err.columnName }}</td>
                <td class="px-4 py-3">
                  <span v-if="err.cellValue" class="px-2 py-0.5 bg-gray-100 rounded text-xs font-mono">
                    {{ err.cellValue }}
                  </span>
                  <span v-else class="text-gray-300 italic">{{ $t('import.empty') }}</span>
                </td>
                <td class="px-4 py-3 text-red-600">{{ err.errorMessage }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Footer -->
      <div class="px-6 py-4 border-t border-gray-100 flex justify-end gap-3 sticky bottom-0">
        <button
          @click="$emit('close')"
          class="px-6 py-2  rounded-xl font-semibold"
        >
          {{ $t('common.close') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { AlertCircle, X } from 'lucide-vue-next';

defineProps({
  show: Boolean,
  errors: {
    type: Array,
    default: () => []
  }
});

defineEmits(['close']);
</script>
