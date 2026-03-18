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
        <span v-else-if="multiple && selectedCount > 0" class="truncate line-clamp-1">
          {{ $t('common.selected') || 'Đã chọn' }}: {{ selectedCount }} {{ $t('common.department') || 'phòng' }}
        </span>
        <span v-else class="text-slate-400 font-medium">{{ placeholder || 'Tất cả phòng ban' }}</span>
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
          :placeholder="$t('common.search') || 'Tìm kiếm...'"
          @click.stop
        />
      </div>

      <div class="max-h-60 overflow-y-auto custom-scrollbar space-y-1 px-1">
        <!-- Option All -->
        <div 
          v-if="allowAll && !multiple"
          @click="selectOption('')"
          class="px-4 py-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center justify-between"
          :class="!modelValue ? 'bg-primary-50 text-primary-700' : 'text-slate-600 hover:bg-slate-50'"
        >
          {{ $t('common.all') || 'Tất cả' }}
          <Check v-if="!modelValue" class="w-3 h-3" />
        </div>

        <!-- Department Options -->
        <div 
          v-for="d in filteredDepartments" 
          :key="d.id"
          @click="selectOption(d.id)"
          class="px-4 py-2.5 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center justify-between group"
          :class="isSelected(d.id) ? 'bg-primary-50 text-primary-700' : 'text-slate-600 hover:bg-slate-50'"
        >
          <div class="flex items-center gap-3">
             <div v-if="multiple" class="w-4 h-4 rounded border-2 flex items-center justify-center transition-all" :class="isSelected(d.id) ? 'bg-primary-600 border-primary-600 text-white' : 'border-slate-200 group-hover:border-primary-300'">
              <Check v-if="isSelected(d.id)" class="w-3 h-3" />
            </div>
            <span class="truncate">{{ d.name }}</span>
          </div>
          <Check v-if="!multiple && isSelected(d.id)" class="w-3 h-3" />
        </div>

        <div v-if="filteredDepartments.length === 0" class="p-8 text-center text-slate-400 text-[10px] font-bold italic uppercase tracking-widest">
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
  modelValue: [String, Number, Array],
  label: String,
  placeholder: String,
  allowAll: {
    type: Boolean,
    default: true
  },
  multiple: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue']);

const isOpen = ref(false);
const search = ref('');
const departments = ref([]);
const searchInput = ref(null);

const fetchData = async () => {
  try {
    const res = await $api.get('/departments');
    departments.value = res.data || [];
  } catch (err) {
    console.error('Lỗi tải phòng ban:', err);
  }
};

const isSelected = (id) => {
  if (props.multiple) {
    return Array.isArray(props.modelValue) && props.modelValue.includes(id);
  }
  return props.modelValue == id;
};

const filteredDepartments = computed(() => {
  if (!search.value) return departments.value;
  return departments.value.filter(d => 
    d.name.toLowerCase().includes(search.value.toLowerCase())
  );
});

const selectedName = computed(() => {
  if (props.multiple) return '';
  if (!props.modelValue) return '';
  const dept = departments.value.find(d => d.id == props.modelValue);
  return dept ? dept.name : '';
});

const selectedCount = computed(() => {
  if (!props.multiple || !Array.isArray(props.modelValue)) return 0;
  return props.modelValue.length;
});

const selectOption = (id) => {
  if (props.multiple) {
    let newValue = Array.isArray(props.modelValue) ? [...props.modelValue] : [];
    const index = newValue.indexOf(id);
    if (index > -1) {
      newValue.splice(index, 1);
    } else {
      newValue.push(id);
    }
    emit('update:modelValue', newValue);
  } else {
    emit('update:modelValue', id);
    isOpen.value = false;
    search.value = '';
  }
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
