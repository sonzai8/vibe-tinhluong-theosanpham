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
          class="w-full pl-10 pr-10 py-2 bg-slate-50 border-none rounded-xl text-xs font-bold focus:ring-2 focus:ring-primary-500 outline-none"
          :placeholder="$t('common.search') || 'Tìm tên hoặc mã nhân viên...'"
          @click.stop
        />
        <button 
          v-if="search" 
          @click="search = ''" 
          class="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-slate-300 hover:text-slate-500 transition-colors"
        >
          <X class="w-3 h-3" />
        </button>
      </div>

      <div class="max-h-60 overflow-y-auto custom-scrollbar space-y-1 px-1">
        <div v-if="searching" class="p-4 text-center text-slate-400 text-[10px] font-bold animate-pulse uppercase tracking-widest">
           {{ $t('common.loading') || 'Đang tìm...' }}
        </div>
        <div 
          v-for="e in filteredEmployees" 
          :key="e.id"
          @click="selectOption(e)"
          class="px-4 py-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center justify-between group"
          :class="modelValue == e.id ? 'bg-primary-50 text-primary-700' : 'text-slate-600 hover:bg-slate-50'"
        >
          <div class="flex flex-col">
            <span class="font-medium text-slate-900">
              {{ e.fullName }}
            </span>
            <span class="text-[10px] text-slate-500">
              {{ e.code }} - {{ e.teamName || $t('common.noTeam') || 'Chưa có tổ' }}
            </span>
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
import { ChevronDown, Search, Check, X } from 'lucide-vue-next';

const { $api } = useNuxtApp();

const props = defineProps({
  modelValue: [String, Number],
  label: String,
  placeholder: String,
  departmentId: [String, Number],
  teamId: [String, Number],
  attendanceDate: String, // YYYY-MM-DD
  excludeIds: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:modelValue']);

const isOpen = ref(false);
const search = ref('');
const employees = ref([]);
const selectedEmployee = ref(null);
const searchInput = ref(null);
const searching = ref(false);

const fetchData = async (query = '') => {
  searching.value = true;
  try {
    let res;
    if (props.attendanceDate) {
      // Tìm kiếm nhân viên chưa chấm công
      res = await $api.get('/employees/unrecorded', { 
        params: { 
          date: props.attendanceDate,
          teamId: props.teamId || null,
          search: query,
          limit: 20
        } 
      });
    } else {
      // Tìm kiếm thông thường
      res = await $api.get('/employees/search', { params: { search: query } });
    }
    
    if (res.success) {
      employees.value = res.data || [];
    }
  } catch (err) {
    console.error('Lỗi tải nhân viên:', err);
  } finally {
    searching.value = false;
  }
};

const fetchSelected = async (id) => {
  if (!id) return;
  try {
    const res = await $api.get(`/employees/${id}`);
    if (res.success) {
      selectedEmployee.value = res.data;
      // Thêm vào danh sách nếu chưa có để hiển thị tên khi load trang
      if (!employees.value.find(e => e.id == id)) {
        employees.value.push(res.data);
      }
    }
  } catch (err) {
    console.error('Lỗi tải thông tin nhân viên chọn:', err);
  }
};

// Debounce search
let timeout = null;
watch(search, (val) => {
  if (timeout) clearTimeout(timeout);
  timeout = setTimeout(() => {
    fetchData(val);
  }, 500);
});

// Watch for external dependency changes to re-fetch
watch(() => [props.attendanceDate, props.teamId], () => {
  if (isOpen.value) fetchData(search.value);
}, { immediate: false });

const filteredEmployees = computed(() => {
    let result = employees.value;
    if (props.excludeIds && props.excludeIds.length > 0) {
        result = result.filter(e => !props.excludeIds.includes(e.id));
    }
    
    // Nếu chưa tìm kiếm, lọc theo phòng ban/tổ nếu có để thu hẹp dữ liệu ban đầu
    if (!search.value.trim() && !props.attendanceDate) {
        if (props.departmentId) {
            result = result.filter(e => e.department?.id == props.departmentId);
        }
        if (props.teamId) {
            result = result.filter(e => e.team?.id == props.teamId);
        }
    }
    return result;
});

const selectedName = computed(() => {
  let empFound = null;
  if (selectedEmployee.value && (selectedEmployee.value.id == props.modelValue)) {
    empFound = selectedEmployee.value;
  } else if (props.modelValue) {
    empFound = employees.value.find(e => e.id == props.modelValue);
  }

  if (empFound) {
    const teamName = empFound.teamName || (empFound.department ? empFound.department.name : '') || (empFound.team ? empFound.team.name : '');
    return teamName ? `${empFound.fullName} - ${teamName}` : empFound.fullName;
  }
  return '';
});

const selectOption = (emp) => {
  selectedEmployee.value = emp;
  emit('update:modelValue', emp.id);
  isOpen.value = false;
  search.value = '';
};

const close = () => {
  isOpen.value = false;
  search.value = '';
};

watch(isOpen, (val) => {
  if (val) {
    fetchData(search.value);
    nextTick(() => {
      searchInput.value?.focus();
    });
  }
});

const init = () => {
  if (props.modelValue) {
    fetchSelected(props.modelValue);
  }
};

watch(() => props.modelValue, (newVal) => {
  if (newVal && (!selectedEmployee.value || selectedEmployee.value.id != newVal)) {
    fetchSelected(newVal);
  } else if (!newVal) {
    selectedEmployee.value = null;
  }
});

onMounted(init);

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
