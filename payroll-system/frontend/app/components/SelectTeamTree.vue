<template>
  <div class="relative w-full" v-click-outside="close">
    <!-- Label -->
    <label v-if="label" class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1 mb-1 block">
      {{ label }}
    </label>

    <!-- Trigger Button -->
    <div
      @click="toggle"
      class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-3 text-sm font-bold text-slate-700 cursor-pointer flex items-center justify-between hover:bg-slate-100 transition-all gap-2"
      :class="{
        'ring-2 ring-primary-500 bg-white shadow-md border-primary-100': isOpen,
        'border-red-200 ring-2 ring-red-300': hasError
      }"
    >
      <!-- Trigger Label -->
      <div class="flex items-center gap-2 flex-1 min-w-0">
        <Layers class="w-4 h-4 text-slate-400 shrink-0" />

        <!-- Multi-select: hiện chips số lượng -->
        <template v-if="multiple">
          <template v-if="selectedIds.length === 0">
            <span class="text-slate-400 font-medium truncate">{{ placeholder || 'Tất cả tổ đội' }}</span>
          </template>
          <template v-else-if="selectedIds.length === 1">
            <span class="truncate">{{ getTeamName(selectedIds[0]) }}</span>
          </template>
          <template v-else>
            <span class="inline-flex items-center gap-1.5 px-2 py-0.5 bg-primary-100 text-primary-700 rounded-lg text-xs font-black">
              <Check class="w-3 h-3" /> {{ selectedIds.length }} tổ đã chọn
            </span>
          </template>
        </template>

        <!-- Single-select -->
        <template v-else>
          <span v-if="selectedTeamName" class="truncate">{{ selectedTeamName }}</span>
          <span v-else class="text-slate-400 font-medium truncate">{{ placeholder || 'Chọn tổ đội...' }}</span>
        </template>
      </div>

      <div class="flex items-center gap-1.5 shrink-0">
        <!-- Multi: nút xóa tất cả -->
        <button
          v-if="multiple && selectedIds.length > 0"
          @click.stop="clearAll"
          class="p-0.5 rounded-full hover:bg-slate-200 text-slate-400 hover:text-red-500 transition-all"
          title="Bỏ chọn tất cả"
        >
          <X class="w-3.5 h-3.5" />
        </button>
        <!-- Single: nút clear -->
        <button
          v-if="!multiple && modelValue && allowClear"
          @click.stop="clearValue"
          class="p-0.5 rounded-full hover:bg-slate-200 text-slate-400 hover:text-slate-600 transition-all"
        >
          <X class="w-3.5 h-3.5" />
        </button>
        <ChevronDown :class="['w-4 h-4 text-slate-400 transition-transform duration-200', isOpen ? 'rotate-180' : '']" />
      </div>
    </div>

    <!-- Dropdown -->
    <Transition
      enter-active-class="transition-all duration-200 ease-out"
      enter-from-class="opacity-0 translate-y-1 scale-[0.98]"
      enter-to-class="opacity-100 translate-y-0 scale-100"
      leave-active-class="transition-all duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0 scale-100"
      leave-to-class="opacity-0 translate-y-1 scale-[0.98]"
    >
      <div
        v-if="isOpen"
        class="absolute z-[1000] w-full mt-2 bg-white rounded-2xl shadow-2xl border border-slate-100 overflow-hidden min-w-[280px]"
      >
        <!-- Search Bar -->
        <div class="p-3 border-b border-slate-50">
          <div class="relative">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-300" />
            <input
              ref="searchInput"
              v-model="search"
              type="text"
              class="w-full pl-10 pr-4 py-2.5 bg-slate-50 border-none rounded-xl text-xs font-bold focus:ring-2 focus:ring-primary-500 outline-none transition-all"
              placeholder="Tìm kiếm tổ đội..."
              @click.stop
            />
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="p-6 flex items-center justify-center gap-3">
          <div class="w-5 h-5 border-2 border-slate-100 border-t-primary-500 rounded-full animate-spin"></div>
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Đang tải...</span>
        </div>

        <!-- Tree Content -->
        <div v-else class="max-h-72 overflow-y-auto custom-scrollbar">

          <!-- "Tất cả" option (single mode only) -->
          <div
            v-if="allowAll && !multiple"
            @click="selectSingle(null, null)"
            class="px-4 py-3 text-xs font-bold cursor-pointer flex items-center justify-between border-b border-slate-50 hover:bg-slate-50 transition-all"
            :class="!modelValue ? 'text-primary-600 bg-primary-50/50' : 'text-slate-500'"
          >
            <span>-- Tất cả tổ đội --</span>
            <Check v-if="!modelValue" class="w-3.5 h-3.5 shrink-0" />
          </div>

          <!-- Multi: Select all toggle -->
          <div
            v-if="multiple && groupedTeams.length > 0"
            @click.stop="toggleSelectAll"
            class="px-4 py-2.5 flex items-center gap-3 cursor-pointer hover:bg-slate-50 transition-all border-b border-slate-50 text-[10px] font-black uppercase tracking-widest"
            :class="isAllSelected ? 'text-primary-600' : 'text-slate-500'"
          >
            <!-- Checkbox -->
            <div
              class="w-4 h-4 rounded border-2 flex items-center justify-center transition-all shrink-0"
              :class="isAllSelected ? 'bg-primary-600 border-primary-600 text-white' : isIndeterminate ? 'bg-primary-100 border-primary-400' : 'border-slate-300'"
            >
              <Minus v-if="isIndeterminate && !isAllSelected" class="w-3 h-3 text-primary-600" />
              <Check v-else-if="isAllSelected" class="w-3 h-3 text-white" />
            </div>
            {{ isAllSelected ? 'Bỏ chọn tất cả' : 'Chọn tất cả' }}
          </div>

          <!-- Empty -->
          <div v-if="groupedTeams.length === 0" class="p-8 text-center">
            <div class="w-10 h-10 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-3">
              <Layers class="w-5 h-5 text-slate-200" />
            </div>
            <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Không tìm thấy</p>
          </div>

          <!-- Department Groups (Tree) -->
          <div v-for="group in groupedTeams" :key="group.departmentId" class="border-b border-slate-50 last:border-0">

            <!-- Department Header -->
            <div
              @click.stop="toggleDept(group.departmentId)"
              class="px-4 py-2.5 flex items-center gap-2 cursor-pointer hover:bg-slate-50 transition-all select-none"
              :class="{ 'bg-slate-50/80': expandedDepts.has(group.departmentId) }"
            >
              <!-- Multi: dept-level checkbox (select all teams in dept) -->
              <div
                v-if="multiple"
                @click.stop="toggleDeptSelection(group)"
                class="w-4 h-4 rounded border-2 flex items-center justify-center transition-all shrink-0"
                :class="getDeptCheckboxClass(group)"
              >
                <Minus v-if="isDeptIndeterminate(group)" class="w-3 h-3 text-primary-600" />
                <Check v-else-if="isDeptAllSelected(group)" class="w-3 h-3 text-white" />
              </div>

              <!-- Collapse arrow -->
              <ChevronRight
                :class="['w-3.5 h-3.5 text-slate-400 transition-transform duration-200 shrink-0', expandedDepts.has(group.departmentId) ? 'rotate-90' : '']"
              />

              <!-- Folder icon -->
              <component
                :is="expandedDepts.has(group.departmentId) ? FolderOpen : FolderClosed"
                class="w-4 h-4 shrink-0 transition-all"
                :class="expandedDepts.has(group.departmentId) ? 'text-primary-500' : 'text-slate-400'"
              />

              <span class="text-[11px] font-black text-slate-700 uppercase tracking-widest truncate flex-1">
                {{ group.departmentName }}
              </span>

              <!-- Team count badge -->
              <span class="px-1.5 py-0.5 bg-slate-100 text-[9px] font-black text-slate-500 rounded-full shrink-0">
                <template v-if="multiple">
                  {{ countSelectedInDept(group) }}/{{ group.teams.length }}
                </template>
                <template v-else>{{ group.teams.length }}</template>
              </span>
            </div>

            <!-- Team List (Children) -->
            <Transition
              enter-active-class="transition-all duration-200 ease-out overflow-hidden"
              enter-from-class="opacity-0 max-h-0"
              enter-to-class="opacity-100 max-h-[500px]"
              leave-active-class="transition-all duration-150 ease-in overflow-hidden"
              leave-from-class="opacity-100 max-h-[500px]"
              leave-to-class="opacity-0 max-h-0"
            >
              <div v-if="expandedDepts.has(group.departmentId)" class="pb-1">
                <div
                  v-for="team in group.teams"
                  :key="team.id"
                  @click="handleTeamClick(team)"
                  class="pl-10 pr-4 py-2.5 flex items-center gap-2.5 cursor-pointer transition-all group"
                  :class="isTeamSelected(team.id)
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'"
                >
                  <!-- Tree connector -->
                  <div class="w-4 h-px bg-slate-200 shrink-0"></div>

                  <!-- Multi: checkbox -->
                  <div
                    v-if="multiple"
                    class="w-4 h-4 rounded border-2 flex items-center justify-center transition-all shrink-0"
                    :class="isTeamSelected(team.id) ? 'bg-primary-600 border-primary-600 text-white' : 'border-slate-300 group-hover:border-primary-300'"
                  >
                    <Check v-if="isTeamSelected(team.id)" class="w-3 h-3 text-white" />
                  </div>

                  <!-- Single: dot -->
                  <div
                    v-else
                    class="w-2 h-2 rounded-full border-2 transition-all shrink-0"
                    :class="isTeamSelected(team.id) ? 'bg-primary-500 border-primary-500' : 'border-slate-300 group-hover:border-primary-300'"
                  ></div>

                  <span class="text-xs font-bold truncate flex-1">{{ team.name }}</span>
                  <Check v-if="!multiple && isTeamSelected(team.id)" class="w-3.5 h-3.5 shrink-0 text-primary-600" />
                </div>
              </div>
            </Transition>
          </div>
        </div>

        <!-- Footer -->
        <div class="px-4 py-2.5 border-t border-slate-50 flex items-center justify-between">
          <!-- Multi: selected count -->
          <span v-if="multiple && selectedIds.length > 0" class="text-[9px] font-black text-primary-600 uppercase tracking-widest">
            {{ selectedIds.length }} tổ được chọn
          </span>
          <span v-else class="invisible text-[9px]">·</span>

          <div class="flex items-center gap-3" v-if="!loading && groupedTeams.length > 1">
            <button @click.stop="expandAll" class="text-[9px] font-black text-primary-500 hover:text-primary-700 uppercase tracking-widest transition-all flex items-center gap-1">
              <ChevronsDown class="w-3 h-3" /> Mở tất cả
            </button>
            <span class="text-slate-200">|</span>
            <button @click.stop="collapseAll" class="text-[9px] font-black text-slate-400 hover:text-slate-600 uppercase tracking-widest transition-all flex items-center gap-1">
              <ChevronsUp class="w-3 h-3" /> Thu gọn
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import {
  ChevronDown, ChevronRight, Search, Check, X, Layers, Minus,
  FolderOpen, FolderClosed, ChevronsDown, ChevronsUp
} from 'lucide-vue-next';
import { onMounted } from 'vue';

const { $api } = useNuxtApp();

// ─── Props ─────────────────────────────────────────────────────────────────────
const props = defineProps({
  /** Single: Number | String | null;  Multiple: Array<Number|String> */
  modelValue: { default: null },
  /** Bật multi-select mode */
  multiple: { type: Boolean, default: false },
  /** Nếu truyền vào -> chỉ hiển thị teams thuộc dept đó */
  departmentId: { type: [String, Number, null], default: null },
  label: String,
  placeholder: String,
  allowAll: { type: Boolean, default: false },
  allowClear: { type: Boolean, default: true },
  hasError: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue', 'select']);

// ─── State ────────────────────────────────────────────────────────────────────
const isOpen = ref(false);
const search = ref('');
const loading = ref(false);
const teams = ref([]);
const searchInput = ref(null);
const expandedDepts = ref(new Set());

// ─── Computed: lấy danh sách ID đang được chọn (chuẩn hóa) ──────────────────
const selectedIds = computed(() => {
  if (props.multiple) {
    return Array.isArray(props.modelValue) ? props.modelValue : [];
  }
  return props.modelValue ? [props.modelValue] : [];
});

// ─── Fetch ───────────────────────────────────────────────────────────────────
const fetchTeams = async () => {
  if (teams.value.length > 0) return; // cache
  loading.value = true;
  try {
    const res = await $api.get('/teams');
    teams.value = res.data || [];
  } catch (err) {
    console.error('SelectTeamTree: Lỗi tải tổ đội', err);
  } finally {
    loading.value = false;
  }
};

// ─── Computed: lọc + nhóm ────────────────────────────────────────────────────
const filteredTeams = computed(() => {
  let list = teams.value;
  // Lọc theo departmentId prop nếu có
  if (props.departmentId) {
    list = list.filter(t => (t.departmentId || t.department?.id) == props.departmentId);
  }
  // Lọc theo search
  if (search.value.trim()) {
    const q = search.value.toLowerCase().trim();
    list = list.filter(t =>
      t.name.toLowerCase().includes(q) ||
      (t.departmentName || t.department?.name || '').toLowerCase().includes(q)
    );
  }
  return list;
});

/** Group theo department */
const groupedTeams = computed(() => {
  const map = new Map();
  for (const team of filteredTeams.value) {
    const deptId = team.departmentId || team.department?.id || 0;
    const deptName = team.departmentName || team.department?.name || 'Không có phòng ban';
    if (!map.has(deptId)) {
      map.set(deptId, { departmentId: deptId, departmentName: deptName, teams: [] });
    }
    map.get(deptId).teams.push(team);
  }
  return [...map.values()].sort((a, b) => a.departmentName.localeCompare(b.departmentName, 'vi'));
});

/** Tên team đang được chọn (single mode) */
const selectedTeamName = computed(() => {
  if (props.multiple || !props.modelValue) return '';
  const team = teams.value.find(t => t.id == props.modelValue);
  return team ? team.name : '';
});

const getTeamName = (id) => {
  const team = teams.value.find(t => t.id == id);
  return team ? team.name : id;
};

// ─── Multi-select helpers ─────────────────────────────────────────────────────
const isTeamSelected = (id) => {
  if (props.multiple) return selectedIds.value.includes(id);
  return props.modelValue == id;
};

const allFilteredIds = computed(() => filteredTeams.value.map(t => t.id));

const isAllSelected = computed(() =>
  allFilteredIds.value.length > 0 && allFilteredIds.value.every(id => selectedIds.value.includes(id))
);

const isIndeterminate = computed(() =>
  selectedIds.value.some(id => allFilteredIds.value.includes(id)) && !isAllSelected.value
);

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    // Bỏ chọn tất cả filtered teams
    const newVal = selectedIds.value.filter(id => !allFilteredIds.value.includes(id));
    emit('update:modelValue', newVal);
  } else {
    // Thêm tất cả filtered teams (không trùng lặp)
    const newVal = [...new Set([...selectedIds.value, ...allFilteredIds.value])];
    emit('update:modelValue', newVal);
  }
};

// Department-level selection helpers
const isDeptAllSelected = (group) =>
  group.teams.length > 0 && group.teams.every(t => selectedIds.value.includes(t.id));

const isDeptIndeterminate = (group) =>
  group.teams.some(t => selectedIds.value.includes(t.id)) && !isDeptAllSelected(group);

const countSelectedInDept = (group) =>
  group.teams.filter(t => selectedIds.value.includes(t.id)).length;

const getDeptCheckboxClass = (group) => {
  if (isDeptAllSelected(group)) return 'bg-primary-600 border-primary-600 text-white';
  if (isDeptIndeterminate(group)) return 'bg-primary-50 border-primary-400';
  return 'border-slate-300 group-hover:border-primary-300';
};

const toggleDeptSelection = (group) => {
  if (isDeptAllSelected(group)) {
    // Bỏ chọn tất cả teams trong dept
    const deptIds = group.teams.map(t => t.id);
    emit('update:modelValue', selectedIds.value.filter(id => !deptIds.includes(id)));
  } else {
    // Chọn tất cả teams trong dept
    const deptIds = group.teams.map(t => t.id);
    emit('update:modelValue', [...new Set([...selectedIds.value, ...deptIds])]);
  }
};

// ─── Team click handler ───────────────────────────────────────────────────────
const handleTeamClick = (team) => {
  if (props.multiple) {
    // Toggle trong mảng
    const current = [...selectedIds.value];
    const idx = current.indexOf(team.id);
    if (idx > -1) {
      current.splice(idx, 1);
    } else {
      current.push(team.id);
    }
    emit('update:modelValue', current);
    emit('select', { id: team.id, name: team.name, selected: idx === -1 });
  } else {
    selectSingle(team.id, team.name);
  }
};

const selectSingle = (id, name) => {
  emit('update:modelValue', id || null);
  emit('select', { id, name });
  isOpen.value = false;
  search.value = '';
};

const clearValue = () => {
  emit('update:modelValue', null);
  emit('select', { id: null, name: null });
};

const clearAll = () => {
  emit('update:modelValue', []);
};

// ─── Dept expand/collapse ─────────────────────────────────────────────────────
const toggleDept = (deptId) => {
  const next = new Set(expandedDepts.value);
  if (next.has(deptId)) {
    next.delete(deptId);
  } else {
    next.add(deptId);
  }
  expandedDepts.value = next;
};

const expandAll = () => {
  expandedDepts.value = new Set(groupedTeams.value.map(g => g.departmentId));
};

const collapseAll = () => {
  expandedDepts.value = new Set();
};

// ─── Open/Close ───────────────────────────────────────────────────────────────
const toggle = () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) fetchTeams();
};

const close = () => {
  isOpen.value = false;
  search.value = '';
};

// ─── Watchers ─────────────────────────────────────────────────────────────────
watch(isOpen, async (val) => {
  if (!val) return;
  await fetchTeams();
  nextTick(() => searchInput.value?.focus());

  // Auto-expand logic
  if (props.departmentId) {
    expandedDepts.value = new Set([Number(props.departmentId)]);
  } else if (!props.multiple && props.modelValue) {
    // Single: expand dept chứa team đang chọn
    const current = teams.value.find(t => t.id == props.modelValue);
    const deptId = current?.departmentId || current?.department?.id;
    if (deptId) expandedDepts.value = new Set([deptId]);
  } else if (props.multiple && selectedIds.value.length > 0) {
    // Multi: expand depts chứa các team đang chọn
    const depts = new Set();
    for (const id of selectedIds.value) {
      const t = teams.value.find(t_ => t_.id == id);
      const deptId = t?.departmentId || t?.department?.id;
      if (deptId) depts.add(deptId);
    }
    expandedDepts.value = depts.size > 0 ? depts : new Set();
  } else {
    // Mặc định expand tất cả nếu ≤ 3 dept
    await nextTick();
    if (groupedTeams.value.length <= 3) expandAll();
  }
});

// Khi search có kết quả -> expand tất cả
watch(search, (val) => {
  if (val.trim()) expandAll();
});

onMounted(() => {
  if (props.modelValue && (!Array.isArray(props.modelValue) || props.modelValue.length > 0)) {
    fetchTeams();
  }
});

// Khi departmentId thay đổi -> reset giá trị nếu team hiện tại không thuộc dept mới
watch(() => props.departmentId, (newDeptId) => {
  if (!newDeptId) return;
  if (props.multiple) {
    const valid = selectedIds.value.filter(id => {
      const t = teams.value.find(t_ => t_.id == id);
      return (t?.departmentId || t?.department?.id) == newDeptId;
    });
    if (valid.length !== selectedIds.value.length) emit('update:modelValue', valid);
  } else if (props.modelValue) {
    const t = teams.value.find(t_ => t_.id == props.modelValue);
    const tDept = t?.departmentId || t?.department?.id;
    if (tDept && tDept != newDeptId) emit('update:modelValue', null);
  }
});

// ─── Click Outside Directive ──────────────────────────────────────────────────
const vClickOutside = {
  mounted(el, binding) {
    el.clickOutsideEvent = (event) => {
      if (!(el === event.target || el.contains(event.target))) binding.value();
    };
    document.addEventListener('click', el.clickOutsideEvent);
  },
  unmounted(el) {
    document.removeEventListener('click', el.clickOutsideEvent);
  },
};
</script>
