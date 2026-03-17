<template>
  <div class="flex items-center justify-center w-full h-full min-h-[40px] cursor-pointer hover:bg-slate-100/50 transition-colors group/cell-inner relative">
    <div v-if="loading" class="w-4 h-4 border-2 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
    <template v-else>
      <!-- Hiển thị code của loại công (NG, D, P...) nếu có -->
      <div v-if="definitionCode" 
           :class="['text-[10px] font-black tracking-tighter transition-all group-hover/cell-inner:scale-110', getCodeColorClass(definitionCode)]"
           :title="definitionName">
        {{ definitionCode }}
      </div>
      
      <!-- Fallback cho các trạng thái cũ hoặc đặc biệt -->
      <div v-else-if="status === 'PRESENT'" class="w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.4)] transition-all group-hover/cell-inner:scale-125" :title="$t('attendance.present')"></div>
      <div v-else-if="status === 'ABSENT'" class="w-1.5 h-1.5 rounded-full bg-slate-200 group-hover/cell-inner:bg-primary-300" :title="$t('attendance.absent')"></div>
      <div v-else-if="status === 'SUNDAY'" class="text-[8px] font-black text-red-200 uppercase tracking-tighter" :title="$t('attendance.sunday')">{{ $t('attendance.sunday_short') || 'CN' }}</div>
      <div v-else class="w-1 h-1 rounded-full bg-slate-100 opacity-20 group-hover/cell-inner:opacity-100"></div>
    </template>
  </div>
</template>

<script setup>
const props = defineProps({
  status: String, // 'PRESENT', 'ABSENT', 'SUNDAY', null
  definitionCode: String, // 'NG', 'D', 'P'...
  definitionName: String,
  loading: Boolean
});

const getCodeColorClass = (code) => {
  const c = code.toUpperCase();
  if (c === 'NG') return 'text-emerald-600';
  if (c === 'D') return 'text-blue-600';
  if (c === 'P') return 'text-amber-500';
  if (c === 'KP') return 'text-red-500';
  if (c === '/2') return 'text-indigo-500';
  if (c.includes('1.5')) return 'text-orange-600';
  return 'text-slate-600';
};
</script>
