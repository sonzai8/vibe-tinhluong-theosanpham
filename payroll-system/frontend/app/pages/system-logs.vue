<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900 tracking-tight">Log Hệ thống</h2>
        <p class="text-slate-500 font-medium tracking-tight">Theo dõi và xử lý các lỗi vận hành, đồng bộ dữ liệu</p>
      </div>
      <div class="flex items-center gap-2">
        <UiButton variant="outline" @click="fetchLogs">
          <RefreshCw :class="['w-4 h-4', loading && 'animate-spin']" />
          Làm mới
        </UiButton>
      </div>
    </div>

    <!-- Filters -->
    <div class="card p-4 flex gap-4 items-center">
      <div class="flex-1 relative">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
        <input 
          v-model="searchQuery"
          type="text" 
          placeholder="Tìm kiếm log..." 
          class="w-full pl-10 pr-4 py-2.5 bg-slate-50 border-none rounded-xl focus:ring-2 focus:ring-primary-500 transition-all font-medium text-sm"
        />
      </div>
      <select v-model="filterStatus" class="bg-slate-50 border-none rounded-xl px-4 py-2.5 text-sm font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 transition-all">
        <option value="">Tất cả trạng thái</option>
        <option value="pending">Chưa xử lý</option>
        <option value="resolved">Đã xử lý</option>
      </select>
    </div>

    <!-- Logs Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold tracking-tighter animate-pulse">Đang tải dữ liệu log...</p>
      </div>

      <div v-else-if="filteredLogs.length === 0" class="p-20 text-center">
        <div class="w-16 h-16 bg-slate-50 rounded-2xl flex items-center justify-center text-slate-300 mx-auto mb-4 border border-slate-100">
          <ClipboardList class="w-8 h-8" />
        </div>
        <p class="text-slate-500 font-bold tracking-tight italic">Không có log nào được tìm thấy</p>
      </div>

      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-[0.15em] border-b border-slate-100 px-6">
            <th class="px-6 py-4">Thời gian / Loại</th>
            <th class="px-6 py-4">Thông điệp</th>
            <th class="px-6 py-4">Chi tiết</th>
            <th class="px-6 py-4">Trạng thái</th>
            <th class="px-6 py-4 text-right">Thao tác</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="log in filteredLogs" :key="log.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4">
              <div class="flex flex-col gap-0.5">
                <span class="text-xs font-black text-slate-900 tracking-tighter">{{ formatDateTime(log.createdAt) }}</span>
                <span :class="['text-[9px] font-black uppercase tracking-widest px-1.5 py-0.5 rounded w-fit', getTypeClass(log.type)]">
                  {{ log.type }}
                </span>
              </div>
            </td>
            <td class="px-6 py-4">
              <p class="text-sm font-bold text-slate-700 tracking-tight">{{ log.message }}</p>
            </td>
            <td class="px-6 py-4 max-w-xs">
              <p class="text-[11px] font-medium text-slate-400 truncate group-hover:whitespace-normal group-hover:break-words transition-all cursor-help" :title="log.detail">
                {{ log.detail }}
              </p>
            </td>
            <td class="px-6 py-4">
              <div v-if="log.resolved" class="space-y-1">
                <span class="inline-flex items-center gap-1.5 px-2 py-0.5 bg-emerald-50 text-emerald-700 text-[10px] font-black uppercase tracking-widest rounded-lg border border-emerald-100">
                  <CheckCircle2 class="w-3 h-3" /> Đã xử lý
                </span>
                <p v-if="log.resolutionNote" class="text-[9px] font-bold text-slate-400 italic bg-slate-50 p-1.5 rounded-lg border border-slate-100">
                  Note: {{ log.resolutionNote }}
                </p>
              </div>
              <span v-else class="inline-flex items-center gap-1.5 px-2 py-0.5 bg-amber-50 text-amber-700 text-[10px] font-black uppercase tracking-widest rounded-lg border border-amber-100">
                <AlertCircle class="w-3 h-3" /> Chờ xử lý
              </span>
            </td>
            <td class="px-6 py-4 text-right">
              <button 
                v-if="!log.resolved"
                @click="openResolveModal(log)"
                class="inline-flex items-center gap-1.5 text-xs font-black text-primary-600 hover:text-primary-700 bg-primary-50 hover:bg-primary-100 px-3 py-1.5 rounded-xl transition-all border border-primary-100/50"
              >
                Xử lý ngay
              </button>
              <button v-else class="text-slate-300 opacity-50 cursor-not-allowed">
                <Check class="w-4 h-4" />
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Resolve Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-md p-8 animate-in zoom-in duration-200">
        <div class="flex flex-col items-center text-center mb-8">
          <div class="w-16 h-16 bg-primary-50 rounded-2xl flex items-center justify-center text-primary-600 mb-4 shadow-inner">
            <CheckCircle2 class="w-8 h-8" />
          </div>
          <h3 class="text-xl font-black text-slate-900 tracking-tight">Đánh dấu đã xử lý</h3>
          <p class="text-slate-500 text-sm font-medium mt-2">Vui lòng nhập ghi chú về phương án đã xử lý cho log này để lưu lại lịch sử.</p>
        </div>

        <div class="space-y-6">
          <div class="bg-slate-50 p-4 rounded-xl border border-slate-100 text-left">
            <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1 italic">Log Cần xử lý</p>
            <p class="text-sm font-bold text-slate-700 tracking-tight">{{ currentLog.message }}</p>
          </div>

          <div class="space-y-1.5">
            <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Phương án xử lý</label>
            <textarea 
              v-model="resolutionNote" 
              class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-3 text-sm font-bold text-slate-700 outline-none focus:bg-white focus:border-slate-200 transition-all min-h-[100px]"
              placeholder="VD: Đã cập nhật lại mã chấm công cho nhân viên X..."
            ></textarea>
          </div>
          
          <div class="flex gap-4 pt-4 border-t border-slate-100">
            <button @click="showModal = false" class="flex-1 py-3 rounded-xl border border-slate-200 text-slate-600 font-extrabold hover:bg-slate-50 transition-all">Hủy</button>
            <UiButton @click="handleResolve" class="flex-[2] font-black" :loading="saving" :disabled="!resolutionNote">
              Xác nhận Xử lý
            </UiButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { 
  RefreshCw, Search, CheckCircle2, AlertCircle, ClipboardList, Check, X
} from 'lucide-vue-next';

const { $api } = useNuxtApp();
const logs = ref([]);
const loading = ref(true);
const saving = ref(false);
const searchQuery = ref('');
const filterStatus = ref('');

const showModal = ref(false);
const currentLog = ref({});
const resolutionNote = ref('');

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await $api.get('/system-logs');
    logs.value = res.data.content; // Page object structure
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const fetchLogs = () => fetchData();

const filteredLogs = computed(() => {
  return logs.value.filter(l => {
    const matchSearch = l.message.toLowerCase().includes(searchQuery.value.toLowerCase()) || 
                      l.type.toLowerCase().includes(searchQuery.value.toLowerCase());
    const matchStatus = !filterStatus.value || 
                       (filterStatus.value === 'resolved' ? l.resolved : !l.resolved);
    return matchSearch && matchStatus;
  });
});

const getTypeClass = (type) => {
  const map = {
    'ZKTECO_MAPPING_ERROR': 'bg-red-100 text-red-700 border border-red-200',
    'ERROR': 'bg-red-100 text-red-700 border border-red-200',
    'WARNING': 'bg-amber-100 text-amber-700 border border-amber-200',
    'INFO': 'bg-blue-100 text-blue-700 border border-blue-200'
  };
  return map[type] || 'bg-slate-100 text-slate-700 border border-slate-200';
};

const formatDateTime = (val) => {
  if (!val) return '---';
  const d = new Date(val);
  return d.toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
};

const openResolveModal = (log) => {
  currentLog.value = log;
  resolutionNote.value = '';
  showModal.value = true;
};

const handleResolve = async () => {
  if (!resolutionNote.value) return;
  saving.value = true;
  try {
    await $api.post(`/system-logs/${currentLog.value.id}/resolve`, resolutionNote.value);
    showModal.value = false;
    fetchData();
  } catch (err) {
    console.error(err);
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  fetchData();
});
</script>
