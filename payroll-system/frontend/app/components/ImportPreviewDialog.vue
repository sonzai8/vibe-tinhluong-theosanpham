<template>
  <div v-if="show" class="fixed inset-0 z-[110] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-md">
    <div class="bg-white rounded-3xl shadow-2xl w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in duration-300 border border-white/20">
      <!-- Header -->
      <div class="px-8 py-6 border-b border-slate-100 flex items-center justify-between bg-white/80 backdrop-blur-md sticky top-0 z-10">
        <div class="flex items-center gap-4">
          <div class="p-3 bg-primary-50 rounded-2xl text-primary-600 ring-4 ring-primary-50/50">
            <ClipboardCheck class="w-6 h-6" />
          </div>
          <div>
            <h3 class="text-2xl font-black text-slate-900 tracking-tight">{{ title }}</h3>
            <p class="text-sm text-slate-500 font-bold flex items-center gap-2">
              <span class="text-primary-600">✓ {{ data.length }} hợp lệ</span>
              <span v-if="errors.length > 0" class="text-red-500">• {{ errors.length }} lỗi</span>
            </p>
          </div>
        </div>
        <button 
          @click="$emit('close')" 
          class="p-2 hover:bg-slate-100 rounded-full transition-all text-slate-400 hover:text-slate-600"
        >
          <X class="w-6 h-6" />
        </button>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-auto p-8 space-y-8">
        <!-- Valid Data Section -->
        <div v-if="data.length > 0">
          <h4 class="text-xs font-black text-slate-400 uppercase tracking-widest mb-4 flex items-center gap-2">
            <CheckCircle2 class="w-4 h-4 text-primary-500" />
            Dữ liệu hợp lệ (Sẽ được nhập)
          </h4>
          <div class="overflow-hidden rounded-2xl border border-slate-100 bg-slate-50/50">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-100/50 text-[10px] font-black text-slate-500 uppercase tracking-widest border-b border-slate-200/50">
                  <th class="px-6 py-4 w-16">STT</th>
                  <th v-for="col in columns" :key="col.key" class="px-6 py-4">{{ col.label }}</th>
                  <th class="px-6 py-4 text-right">Trạng thái</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 text-sm">
                <tr v-for="(item, idx) in data" :key="idx" class="hover:bg-white transition-colors group">
                  <td class="px-6 py-4 font-black text-slate-400 text-xs">#{{ idx + 1 }}</td>
                  <td v-for="col in columns" :key="col.key" class="px-6 py-4 font-bold text-slate-700">{{ item[col.key] }}</td>
                  <td class="px-6 py-4 text-right">
                    <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-50 text-primary-600 text-[10px] font-black uppercase tracking-wider ring-1 ring-primary-100">
                      Mới
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Errors Section -->
        <div v-if="errors.length > 0">
          <h4 class="text-xs font-black text-slate-400 uppercase tracking-widest mb-4 flex items-center gap-2">
            <AlertCircle class="w-4 h-4 text-red-500" />
            Dữ liệu không hợp lệ (Sẽ bị bỏ qua)
          </h4>
          <div class="overflow-hidden rounded-2xl border border-red-100 bg-red-50/20">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-red-50/50 text-[10px] font-black text-red-700/50 uppercase tracking-widest border-b border-red-100">
                  <th class="px-6 py-4 w-16">Dòng</th>
                  <th class="px-6 py-4">Cột/Dữ liệu</th>
                  <th class="px-6 py-4">Lý do</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-red-50 text-sm">
                <tr v-for="(err, idx) in errors" :key="idx" class="hover:bg-white transition-colors">
                  <td class="px-6 py-4 font-black text-red-400 text-xs">{{ err.rowNumber }}</td>
                  <td class="px-6 py-4 font-bold text-red-900/60">
                    <span class="block">{{ err.columnName }}</span>
                    <span class="text-[10px] opacity-70">{{ err.cellValue || '(Trống)' }}</span>
                  </td>
                  <td class="px-6 py-4 text-red-600 font-medium">{{ err.errorMessage }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="px-8 py-6 border-t border-slate-100 bg-slate-50 flex justify-end gap-3 sticky bottom-0">
        <button
          @click="$emit('close')"
          class="px-8 py-3 bg-white border border-slate-200 text-slate-600 rounded-2xl font-black text-sm hover:bg-slate-50 hover:border-slate-300 transition-all shadow-sm active:scale-95"
        >
          Hủy bỏ
        </button>
        <UiButton
          v-if="data.length > 0"
          @click="$emit('confirm')"
          :loading="loading"
          class="px-8 py-3 !rounded-2xl shadow-xl shadow-primary-200 active:scale-95"
        >
          {{ $t('common.confirm_import') }}
        </UiButton>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ClipboardCheck, X, AlertCircle, CheckCircle2 } from 'lucide-vue-next';

defineProps({
  show: Boolean,
  loading: Boolean,
  title: {
    type: String,
    default: 'Xác nhận nhập dữ liệu'
  },
  data: {
    type: Array,
    default: () => []
  },
  errors: {
    type: Array,
    default: () => []
  },
  columns: {
    type: Array,
    default: () => [
      { label: 'Tên', key: 'name' }
    ]
  }
});

defineEmits(['close', 'confirm']);
</script>
