<template>
  <div class="relative w-full" v-click-outside="close">
    <div class="space-y-2">
      <label v-if="label" class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ label }}</label>
      <div 
        @click="isOpen = !isOpen"
        class="w-full bg-slate-50 border-none rounded-xl px-4 py-3 text-sm font-bold text-slate-700 cursor-pointer flex items-center justify-between hover:bg-slate-100 transition-all font-sans"
        :class="{ 'ring-2 ring-primary-500 bg-white shadow-lg shadow-primary-50': isOpen }"
      >
        <span v-if="selectedName" class="truncate line-clamp-1">{{ selectedName }}</span>
        <span v-else class="text-slate-400 font-medium">{{ placeholder || 'Chọn nhân viên...' }}</span>
        <ChevronDown :class="['w-4 h-4 text-slate-400 transition-transform', isOpen ? 'rotate-180' : '']" />
      </div>
    </div>

    <!-- Dropdown -->
    <div v-if="isOpen" class="absolute z-[1000] w-full mt-2 bg-white rounded-2xl shadow-2xl border border-slate-100 p-2 animate-in fade-in zoom-in-95 duration-200 min-w-[250px]">
      <div class="relative mb-2">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-300" />
        <input 
          ref="searchInput"
          v-model="search"
          type="text"
          class="w-full pl-10 pr-4 py-2 bg-slate-50 border-none rounded-xl text-xs font-bold focus:ring-2 focus:ring-primary-500 outline-none"
          :placeholder="$t('common.search') || 'Tìm tên hoặc mã nhân viên...'"
          @click.stop
        />
      </div>

      <div class="max-h-60 overflow-y-auto custom-scrollbar space-y-1 px-1">
        <div 
          v-for="e in filteredEmployees" 
          :key="e.id"
          @click="selectOption(e.id)"
          class="px-4 py-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center justify-between group"
          :class="modelValue == e.id ? 'bg-primary-50 text-primary-700' : 'text-slate-600 hover:bg-slate-50'"
        >
          <div class="flex flex-col">
            <span class="truncate">{{ e.fullName }}</span>
            <span class="text-[9px] text-slate-400 uppercase tracking-tighter">{{ e.code }}</span>
          </div>
          <Check v-if="modelValue == e.id" class="w-3 h-3 text-primary-600" />
        </div>

        <div v-if="filteredEmployees.length === 0" class="p-8 text-center text-slate-400 text-[10px] font-bold italic uppercase tracking-widest">
          {{ $t('common.not_found') || 'Không tìm thấy' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ChevronDown, Search, Check } from 'lucide-vue-next';

const { $api } = useNuxtApp();

const props = defineProps({
  modelValue: [String, Number],
  label: String,
  placeholder: String,
  departmentId: [String, Number],
  teamId: [String, Number]
});

const emit = defineEmits(['update:modelValue']);

const isOpen = ref(false);
const search = ref('');
const employees = ref([]);
const searchInput = ref(null);

const fetchData = async () => {
  try {
    const res = await $api.get('/employees');
    employees.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải nhân viên:', err);
  }
};

const filteredEmployees = computed(() => {
  let result = employees.value;
  
  if (props.departmentId) {
    result = result.filter(e => e.department?.id == props.departmentId);
  }
  if (props.teamId) {
    result = result.filter(e => e.team?.id == props.teamId);
  }
  
  if (!search.value) return result;
  
  const q = search.value.toLowerCase();
  return result.filter(e => 
    e.fullName.toLowerCase().includes(q) || 
    e.code.toLowerCase().includes(q)
  );
});

const selectedName = computed(() => {
  if (!props.modelValue) return '';
  const emp = employees.value.find(e => e.id == props.modelValue);
  return emp ? `${emp.fullName} (${emp.code})` : '';
});

const selectOption = (id) => {
  emit('update:modelValue', id);
  isOpen.value = false;
  search.value = '';
};

const close = () => {
  isOpen.value = false;
  search.value = '';
};

watch(isOpen, (val) => {
  if (val) {
    nextTick(() => {
      searchInput.value?.focus();
    });
  }
});

onMounted(fetchData);

const vClickOutside = {
  mounted(el, binding) {
    el.clickOutsideEvent = (event) => {
      if (!(el === event.target || el.contains(event.target))) {
        binding.value();
      }
    };
    document.addEventListener("click", el.clickOutsideEvent);
  },
  unmounted(el) {
    document.removeEventListener("click", el.clickOutsideEvent);
  },
};
</script>
