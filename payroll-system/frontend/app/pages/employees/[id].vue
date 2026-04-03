<template>
  <div class="space-y-6 max-w-[1400px] mx-auto pb-12">
    <!-- Header with Back Button (Sticky) -->
    <div class="sticky top-0 z-30 -mx-6 px-6 py-4 bg-slate-50/80 backdrop-blur-md border-b border-slate-200/50 flex items-center justify-between transition-all">
      <div class="flex items-center gap-4">
        <button @click="handleBack" class="p-2 hover:bg-white rounded-xl transition-all shadow-sm border border-transparent hover:border-slate-100">
          <ChevronLeft class="w-6 h-6 text-slate-600" />
        </button>
        <div>
          <h2 class="text-2xl font-black text-slate-900 leading-none mb-1">{{ $t('employee.detail_title') }}</h2>
          <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ employee.fullName }} ({{ employee.code }})</p>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <UiButton variant="outline" @click="handleBack" class="shadow-lg shadow-primary-500/20">
          {{ $t('common.cancel') }}
        </UiButton>
        <UiButton @click="handleSave" :loading="saving" class="shadow-lg shadow-primary-500/20">
          <Save class="w-4 h-4" />
          {{ $t('common.save') }}
        </UiButton>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
      <!-- Left Column: Avatar & Basic Info (Sticky Sidebar) -->
      <div class="lg:col-span-4 space-y-6 lg:sticky lg:top-24 max-h-[calc(100vh-120px)] overflow-y-auto pr-1 custom-scrollbar pb-6">
        <div class="card p-8 flex flex-col items-center">
          <div class="relative group cursor-pointer" @click="$refs.fileInput.click()">
            <div class="w-32 h-32 rounded-3xl bg-slate-100 overflow-hidden border-4 border-white shadow-xl ring-1 ring-slate-100 flex items-center justify-center">
              <img v-if="employee.avatarUrl" :src="fullAvatarUrl" class="w-full h-full object-cover" />
              <div v-else class="w-full h-full flex items-center justify-center text-slate-300">
                <User class="w-16 h-16" />
              </div>
            </div>
            <div class="absolute inset-x-0 bottom-0 bg-slate-900/60 backdrop-blur-sm p-1.5 opacity-0 group-hover:opacity-100 transition-all rounded-b-3xl flex justify-center">
              <Camera class="w-4 h-4 text-white" />
            </div>
            <input type="file" ref="fileInput" class="hidden" accept="image/*" @change="handleAvatarUpload" />
          </div>
          <p class="mt-4 font-black text-slate-900 tracking-tight text-lg">{{ employee.fullName }}</p>
          <p class="text-[10px] font-black text-emerald-600 uppercase tracking-widest mt-1 bg-emerald-50 px-3 py-1 rounded-full">{{ employee.role?.name || 'Vị trí chưa cập nhật' }}</p>
          
          <div class="w-full mt-8 space-y-4">
             <div class="flex items-center justify-between text-[10px] font-black uppercase tracking-widest border-b border-slate-50 pb-2">
                <span class="text-slate-400">{{ $t('employee.status_label') }}</span>
                <span :class="employee.status === 'ACTIVE' ? 'text-emerald-500' : 'text-slate-400'">{{ employee.status === 'ACTIVE' ? $t('common.status_active') : $t('common.status_inactive') }}</span>
             </div>
             <div class="flex items-center justify-between text-[10px] font-black uppercase tracking-widest border-b border-slate-50 pb-2">
                <span class="text-slate-400">{{ $t('common.department') }}</span>
                <span class="text-slate-700 font-bold">{{ employee.department?.name || '---' }}</span>
             </div>
             <div class="flex items-center justify-between text-[10px] font-black uppercase tracking-widest border-b border-slate-50 pb-2">
                <span class="text-slate-400">{{ $t('employee.join_date') }}</span>
                <span class="text-slate-700 font-bold">{{ employee.joinDate || '---' }}</span>
             </div>
          </div>
        </div>

        <div class="card p-6 space-y-4">
           <h3 class="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-slate-50 pb-3">{{ $t('employee.system_account') }}</h3>
           <div class="space-y-3">
              <div>
                <p class="text-[9px] font-black text-slate-400 uppercase tracking-widest mb-1">{{ $t('employee.username_label') }}</p>
                <p class="text-xs font-bold text-slate-700">{{ employee.username || '---' }}</p>
              </div>
               <div class="flex items-center justify-between">
                 <p class="text-[9px] font-black text-slate-400 uppercase tracking-widest">{{ $t('employee.login_permission') }}</p>
                 <span :class="employee.canLogin ? 'text-emerald-500 font-black text-[10px]' : 'text-slate-400 font-bold text-[10px]'">
                   {{ employee.canLogin ? $t('employee.allow') : $t('employee.deny') }}
                 </span>
               </div>
            </div>
         </div>

         <!-- Penalty/Bonus Stats (Collapsible) -->
         <div class="card overflow-hidden transition-all duration-300 shadow-sm border border-slate-100">
            <button 
              @click="isStatsOpen = !isStatsOpen"
              class="w-full p-4 flex items-center justify-between hover:bg-slate-50 transition-all border-b border-transparent"
              :class="{ 'border-slate-200 bg-slate-50/50': isStatsOpen }"
            >
               <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-xl bg-red-100 flex items-center justify-center shadow-sm">
                     <TrendingDown class="w-4 h-4 text-red-600" />
                  </div>
                  <h3 class="text-xs font-black text-slate-900 uppercase tracking-widest">{{ $t('employee.stats_history') }}</h3>
               </div>
               <ChevronDown v-if="!isStatsOpen" class="w-4 h-4 text-slate-400" />
               <ChevronUp v-else class="w-4 h-4 text-slate-400" />
            </button>

            <div v-if="isStatsOpen" class="p-6 space-y-6 animate-in fade-in slide-in-from-top-2 duration-300">
               <div v-if="loadingStats" class="py-12 flex flex-col items-center justify-center gap-4 bg-white/50 rounded-2xl border border-dashed border-slate-200">
                 <div class="w-8 h-8 border-4 border-slate-100 border-t-primary-600 rounded-full animate-spin"></div>
                 <p class="text-slate-400 text-[10px] font-black animate-pulse uppercase tracking-widest">{{ $t('employee.stats_loading') }}</p>
               </div>
               
               <div v-else class="space-y-6">
                 <!-- Simple Counter Cards for Sidebar -->
                 <div class="grid grid-cols-1 gap-3">
                   <div class="p-4 bg-emerald-50 rounded-2xl border border-emerald-100 flex items-center justify-between shadow-sm hover:shadow-md transition-shadow">
                     <div>
                       <p class="text-[9px] font-black text-emerald-600 uppercase tracking-widest">{{ $t('employee.stats_bonus') }}</p>
                       <p class="text-lg font-black text-emerald-700 leading-none mt-1">+{{ (employeeStats.totalBonus || 0).toLocaleString() }}đ</p>
                     </div>
                     <p class="text-[9px] font-bold text-emerald-500 uppercase tracking-tighter">{{ employeeStats.bonusCount || 0 }} {{ $t('employee.stats_times') }}</p>
                   </div>

                   <div class="p-4 bg-red-50 rounded-2xl border border-red-100 flex items-center justify-between shadow-sm hover:shadow-md transition-shadow">
                     <div>
                       <p class="text-[9px] font-black text-red-600 uppercase tracking-widest">{{ $t('employee.stats_penalty') }}</p>
                       <p class="text-lg font-black text-red-700 leading-none mt-1">{{ (employeeStats.totalPenalty || 0).toLocaleString() }}đ</p>
                     </div>
                     <p class="text-[9px] font-bold text-red-500 uppercase tracking-tighter">{{ employeeStats.penaltyCount || 0 }} {{ $t('employee.stats_times') }}</p>
                   </div>

                   <div class="p-4 bg-slate-900 rounded-2xl flex items-center justify-between shadow-lg shadow-slate-200">
                     <div>
                       <p class="text-[9px] font-black text-slate-400 uppercase tracking-widest">{{ $t('employee.stats_net') }}</p>
                       <p class="text-lg font-black text-white leading-none mt-1">{{ (employeeStats.netAmount || 0).toLocaleString() }}đ</p>
                     </div>
                     <DollarSign class="w-5 h-5 text-white/20" />
                   </div>
                 </div>

                 <!-- compact history -->
                 <div class="space-y-2 max-h-60 overflow-y-auto pr-1 custom-scrollbar">
                   <div v-if="!employeeStats.history || employeeStats.history.length === 0" class="py-6 text-center text-slate-400 bg-slate-50/50 rounded-xl border border-dashed border-slate-100">
                     <p class="text-[10px] font-medium italic opacity-70">{{ $t('employee.stats_no_history') }}</p>
                   </div>
                   <div v-else v-for="h in employeeStats.history" :key="h.id" class="p-3 bg-white border border-slate-100 rounded-xl flex items-center justify-between group shadow-sm hover:border-slate-200 transition-all">
                     <div class="flex items-center gap-3">
                        <p class="text-[10px] font-black text-slate-900 leading-none clamp-1">{{ h.reason }}</p>
                     </div>
                     <span :class="['font-black text-[11px] shrink-0', h.amount > 0 ? 'text-emerald-600' : 'text-red-600']">
                       {{ h.amount > 0 ? '+' : '' }}{{ h.amount.toLocaleString() }}đ
                     </span>
                   </div>
                 </div>
               </div>
            </div>
         </div>

         <!-- Notes History (Collapsible) -->
         <div class="card overflow-hidden transition-all duration-300 shadow-sm border border-slate-100">
            <button 
              @click="isNotesOpen = !isNotesOpen"
              class="w-full p-4 flex items-center justify-between hover:bg-slate-50 transition-all border-b border-transparent"
              :class="{ 'border-slate-200 bg-slate-50/50': isNotesOpen }"
            >
               <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-xl bg-primary-100 flex items-center justify-center shadow-sm">
                     <Plus class="w-4 h-4 text-primary-600" />
                  </div>
                  <h3 class="text-xs font-black text-slate-900 uppercase tracking-widest">{{ $t('employee.notes_history') }}</h3>
               </div>
               <ChevronDown v-if="!isNotesOpen" class="w-4 h-4 text-slate-400" />
               <ChevronUp v-else class="w-4 h-4 text-slate-400" />
            </button>

            <div v-if="isNotesOpen" class="p-6 space-y-6 animate-in fade-in slide-in-from-top-2 duration-300">
               <!-- Add Note Form (Inside Sidebar Accordion) -->
               <div class="p-4 bg-slate-50 rounded-xl border border-slate-100 space-y-3 shadow-inner">
                  <textarea v-model="noteForm.content" rows="2" class="w-full bg-white border border-slate-200 rounded-lg px-3 py-2 text-xs font-medium text-slate-700 outline-none focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all resize-none shadow-sm" :placeholder="$t('employee.note_placeholder')"></textarea>
                  <div class="flex justify-end pt-1">
                     <UiButton size="xs" @click="handleAddNote" :loading="addingNote" :disabled="!noteForm.content">
                       <Plus class="w-3 h-3" />
                       {{ $t('employee.add_note') }}
                     </UiButton>
                  </div>
               </div>

               <!-- Compact Notes List -->
               <div class="space-y-3 max-h-80 overflow-y-auto pr-1 custom-scrollbar">
                  <div v-if="notes.length === 0" class="py-6 text-center bg-slate-50/50 rounded-xl border border-dashed border-slate-100">
                     <p class="text-slate-400 text-[10px] font-medium italic opacity-70">{{ $t('employee.no_notes') }}</p>
                  </div>
                  <div v-else v-for="note in notes" :key="note.id" class="p-4 bg-white border border-slate-100 rounded-xl relative group shadow-sm hover:border-slate-200 transition-all">
                     <div class="flex items-center justify-between mb-1.5">
                        <span class="text-[9px] font-black text-slate-400 uppercase tracking-widest">{{ formatDate(note.createdAt) }}</span>
                        <button @click="handleDeleteNote(note.id)" class="p-1 text-slate-300 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-all rounded">
                           <Trash2 class="w-3 h-3" />
                        </button>
                     </div>
                     <p class="text-xs font-medium text-slate-700 leading-normal">{{ note.content }}</p>
                     <div class="flex items-center gap-1.5 mt-2">
                        <span v-if="note.month && note.year" class="px-1.5 py-0.5 bg-primary-50 text-primary-600 text-[8px] font-black rounded uppercase tracking-tighter">{{ note.month }}/{{ note.year }}</span>
                        <p class="text-[8px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1">
                           <User class="w-2.5 h-2.5" /> {{ note.createdBy }}
                        </p>
                     </div>
                  </div>
               </div>
            </div>
         </div>
      </div>

      <!-- Right Column: Form (Content Area) -->
      <div class="lg:col-span-8 space-y-6">
        <div class="card p-10 shadow-sm border border-slate-100">
          <h3 class="text-sm font-black text-slate-900 uppercase tracking-widest mb-8 pb-3 border-b-2 border-primary-500 inline-block">{{ $t('employee.personal_info') }}</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
             <UiInput v-model="form.fullName" :label="$t('employee.full_name')" required />
             <div class="space-y-1.5">
                <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ $t('employee.gender') }}</label>
                <select v-model="form.gender" class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 outline-none focus:bg-white focus:border-slate-200 transition-all shadow-sm">
                  <option value="MALE">{{ $t('employee.male') }}</option>
                  <option value="FEMALE">{{ $t('employee.female') }}</option>
                  <option value="OTHER">{{ $t('employee.other') }}</option>
                </select>
             </div>
             <UiInput v-model="form.dob" type="date" :label="$t('employee.dob')" />
             <UiInput v-model="form.phone" :label="$t('employee.phone')" />
             <UiInput v-model="form.citizenId" :label="$t('employee.citizen_id')" />
             <UiInput v-model="form.citizenIdIssuedDate" type="date" :label="$t('employee.citizen_id_issued_date')" />
             <div class="col-span-2">
                <UiInput v-model="form.citizenIdIssuedPlace" :label="$t('employee.citizen_id_issued_place')" />
             </div>
             <div class="col-span-2">
                <UiInput v-model="form.birthAddress" :label="$t('employee.birth_address')" />
             </div>
             <div class="col-span-2">
                <UiInput v-model="form.permanentAddress" :label="$t('employee.permanent_address')" />
             </div>
          </div>

          <h3 class="text-sm font-black text-slate-900 uppercase tracking-widest mt-12 mb-8 pb-3 border-b-2 border-primary-500 inline-block">{{ $t('employee.work_insurance') }}</h3>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-x-8 gap-y-6">
             <UiInput v-model="form.joinDate" type="date" :label="$t('employee.join_date_at_company')" />
             <UiInput v-model="form.insuranceStartDate" type="date" :label="$t('employee.insurance_start_date')" />
             <UiInput v-model="form.lastWorkingDate" type="date" :label="$t('last_working_date')" />
          </div>

          <h3 class="text-sm font-black text-slate-900 uppercase tracking-widest mt-12 mb-8 pb-3 border-b-2 border-primary-500 inline-block">{{ $t('employee.salary_config') }} & {{ $t('common.team') }}</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
             <div class="space-y-1.5">
                <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ $t('employee.salary_type') }}</label>
                <select v-model="form.salaryType" class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 outline-none focus:bg-white focus:border-slate-200 transition-all shadow-sm">
                  <option value="PRODUCT_BASED">{{ $t('employee.salary_type_product') }}</option>
                  <option value="FIXED_MONTHLY">{{ $t('employee.salary_type_monthly') }}</option>
                  <option value="FIXED_DAILY">{{ $t('employee.salary_type_daily') }}</option>
                </select>
             </div>
             <div class="space-y-1.5">
                <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ $t('common.team') }}</label>
                <div class="flex items-center gap-3">
                   <div class="flex-1 px-4 py-2.5 bg-slate-50 rounded-xl text-sm font-bold text-slate-700 border border-slate-100">
                      {{ employee.team?.name || 'Chưa gán tổ' }}
                   </div>
                   <NuxtLink :to="`/work-histories`" class="p-2.5 bg-primary-50 text-primary-600 rounded-xl border border-primary-100 hover:bg-primary-100 transition-all" title="Điều chuyển & Lịch sử công tác">
                      <History class="w-5 h-5" />
                   </NuxtLink>
                </div>
                <p class="text-[9px] font-medium text-slate-400 mt-1 italic">* Để điều chuyển tổ, vui lòng sang trang Lịch sử công tác</p>
             </div>
             <UiNumericInput v-model="form.baseSalaryConfig" :label="$t('employee.base_salary')" is-currency />
             <UiNumericInput v-model="form.insuranceSalaryConfig" :label="$t('employee.insurance_salary')" is-currency />
          </div>

          <!-- History Sections -->
          <div class="mt-16 grid grid-cols-1 xl:grid-cols-2 gap-12">
            <!-- Salary History -->
            <div>
              <div class="flex items-center justify-between mb-6 pb-2 border-b border-slate-100">
                <h3 class="text-xs font-black text-slate-900 uppercase tracking-widest">{{ $t('employee.salary_history') }}</h3>
                <TrendingUp class="w-4 h-4 text-emerald-500" />
              </div>
              <div class="space-y-4 max-h-[400px] overflow-y-auto pr-3 custom-scrollbar">
                <div v-for="item in salaryHistory" :key="item.id" class="p-5 rounded-2xl border border-slate-100 bg-white shadow-sm relative overflow-hidden group hover:border-emerald-200 transition-all">
                  <div v-if="item.current" class="absolute top-0 right-0 px-3 py-1 bg-emerald-500 text-[9px] font-black text-white rounded-bl-xl uppercase tracking-wider">
                    {{ $t('employee.current') }}
                  </div>
                  <p class="text-[11px] font-black text-slate-900 uppercase tracking-tight mb-2 flex items-center gap-2">
                    <span class="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
                    {{ $t(`employee.salary_type_${item.salaryType.toLowerCase().replace('_based', '').replace('_fixed', '')}`) }}
                  </p>
                  <div class="flex items-baseline gap-2">
                    <span class="text-sm font-black text-slate-700">{{ item.baseSalary.toLocaleString() }}đ</span>
                    <span class="text-[10px] text-slate-400 font-bold bg-slate-50 px-2 py-0.5 rounded-lg border border-slate-100">BH: {{ item.insuranceSalary.toLocaleString() }}đ</span>
                  </div>
                  <p class="text-[10px] font-bold text-slate-400 mt-3 flex items-center gap-1.5">
                    <Clock class="w-3 h-3" />
                    {{ formatDateShort(item.startDate) }} - {{ item.endDate ? formatDateShort(item.endDate) : $t('employee.current') }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Team History -->
            <div>
              <div class="flex items-center justify-between mb-6 pb-2 border-b border-slate-100">
                <h3 class="text-xs font-black text-slate-900 uppercase tracking-widest">{{ $t('employee.team_history') }}</h3>
                <Clock class="w-4 h-4 text-primary-500" />
              </div>
              <div class="space-y-4 max-h-[400px] overflow-y-auto pr-3 custom-scrollbar">
                <div v-for="item in teamHistory" :key="item.id" class="p-5 rounded-2xl border border-slate-100 bg-white shadow-sm relative overflow-hidden group hover:border-primary-200 transition-all">
                  <div v-if="item.isCurrent" class="absolute top-0 right-0 px-3 py-1 bg-primary-500 text-[9px] font-black text-white rounded-bl-xl uppercase tracking-wider">
                    {{ $t('employee.current') }}
                  </div>
                  <p class="text-sm font-black text-slate-700 mb-1">{{ item.team?.name || '---' }}</p>
                  <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest bg-slate-50 px-2 py-0.5 rounded-lg border border-slate-100 inline-block">
                    {{ item.team?.departmentName }}
                  </p>
                  <p class="text-[10px] font-bold text-slate-400 mt-3 flex items-center gap-1.5">
                    <Clock class="w-3 h-3" />
                    {{ formatDateShort(item.startDate) }} - {{ item.endDate ? formatDateShort(item.endDate) : $t('employee.current') }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Common Error Modal -->
    <UiErrorModal
      :show="showErrorModal"
      :title="errorTitle"
      :message="errorMessage"
      :detail="errorDetail"
      @close="showErrorModal = false"
    />
  </div>
</template>

<script setup>
import { 
  ChevronLeft, Save, User, Camera, Trash2, Plus, 
  TrendingUp, TrendingDown, DollarSign, Clock, AlertCircle,
  ChevronDown, ChevronUp
} from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();
const { $api } = useNuxtApp();

const employee = ref({});
const notes = ref([]);
const salaryHistory = ref([]);
const teamHistory = ref([]);
const employeeStats = ref({});
const addingNote = ref(false);
const saving = ref(false);
const loadingStats = ref(false);
const isStatsOpen = ref(false);
const isNotesOpen = ref(false);

// Error Modal State
const showErrorModal = ref(false);
const errorTitle = ref('');
const errorMessage = ref('');
const errorDetail = ref('');

const triggerError = (title, message, detail = '') => {
  errorTitle.value = title;
  errorMessage.value = message;
  errorDetail.value = detail;
  showErrorModal.value = true;
};

const form = reactive({
  fullName: '',
  gender: 'MALE',
  dob: '',
  phone: '',
  citizenId: '',
  citizenIdIssuedDate: '',
  citizenIdIssuedPlace: '',
  birthAddress: '',
  permanentAddress: '',
  joinDate: '',
  insuranceStartDate: '',
  notes: '',
  salaryType: 'PRODUCT_BASED',
  baseSalaryConfig: 0,
  insuranceSalaryConfig: 0,
  teamId: null,
  roleId: null,
  departmentId: null,
  lastWorkingDate: ''
});

const noteForm = reactive({
  content: '',
  month: new Date().getMonth() + 1,
  year: new Date().getFullYear()
});

const fullAvatarUrl = computed(() => {
  if (!employee.value.avatarUrl) return '';
  return `http://localhost:8080${employee.value.avatarUrl}`;
});

const fetchData = async () => {
  try {
    const res = await $api.get(`/employees/${route.params.id}`);
    employee.value = res.data;
    
    // Copy data to form
    Object.keys(form).forEach(key => {
      form[key] = res.data[key] || '';
    });
    
    if (!form.gender) form.gender = 'MALE';
    
    // Set Team/Role IDs
    form.teamId = res.data.team?.id;
    form.departmentId = res.data.department?.id;
    form.roleId = res.data.role?.id;
    form.salaryType = res.data.salaryType || 'PRODUCT_BASED';
    form.baseSalaryConfig = res.data.baseSalaryConfig || 0;
    form.insuranceSalaryConfig = res.data.insuranceSalaryConfig || 0;

    fetchNotes();
    fetchEmployeeStats();
    fetchHistory();
  } catch (err) {
    console.error(err);
    triggerError('Lỗi tải dữ liệu', 'Không thể tải thông tin nhân viên này.', err.message);
  }
};

const fetchHistory = async () => {
  try {
    const [salaryRes, teamRes] = await Promise.all([
      $api.get(`/employees/${route.params.id}/salary-history`),
      $api.get(`/employees/${route.params.id}/team-history`)
    ]);
    salaryHistory.value = salaryRes.data;
    teamHistory.value = teamRes.data;
  } catch (err) {
    console.error('History fetch error:', err);
  }
};

const fetchEmployeeStats = async () => {
  loadingStats.value = true;
  try {
    const res = await $api.get(`/penalty-bonuses/employee/${route.params.id}/stats`);
    employeeStats.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loadingStats.value = false;
  }
};

const fetchNotes = async () => {
  try {
    const res = await $api.get(`/employees/${route.params.id}/notes`);
    notes.value = res.data;
  } catch (err) {
    console.error(err);
  }
};

const handleAddNote = async () => {
  if (!noteForm.content) return;
  addingNote.value = true;
  try {
    await $api.post(`/employees/${employee.value.id}/notes`, noteForm);
    fetchNotes();
  } catch (err) {
    triggerError('Lỗi thêm ghi chú', 'Đã xảy ra lỗi khi thêm ghi chú mới.', err.message);
  } finally {
    addingNote.value = false;
  }
};

const handleDeleteNote = async (noteId) => {
  if (!confirm('Bạn có chắc chắn muốn xóa ghi chú này?')) return;
  try {
    await $api.delete(`/employees/notes/${noteId}`);
    fetchNotes();
  } catch (err) {
    triggerError('Lỗi xóa ghi chú', 'Không thể xóa ghi chú này khỏi hệ thống.', err.message);
  }
};

const handleSave = async () => {
  saving.value = true;
  try {
    const payload = { 
      ...employee.value, 
      ...form
    };
    await $api.put(`/employees/${employee.value.id}`, payload);
    fetchData();
    // Use a success modal or simple notification if needed, but the requirement is to use the common error popup for errors.
    // For success, alert is okay if no common success modal is defined, but let's just use alert for now as requested.
    alert('Cập nhật thành công');
  } catch (err) {
    triggerError('Lỗi lưu thông tin', 'Đã xảy ra lỗi khi cập nhật thông tin nhân viên.', err.response?.data?.message || err.message);
  } finally {
    saving.value = false;
  }
};

const handleAvatarUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('file', file);

  try {
    await $api.post(`/employees/${employee.value.id}/avatar`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    fetchData();
  } catch (err) {
    const errorMsg = err.response?.data?.message || err.message;
    triggerError('Lỗi tải ảnh', 'Không thể tải ảnh đại diện lên hệ thống.', errorMsg);
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

const formatDateShort = (dateStr) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  });
};

const handleBack = () => router.push('/employees');

onMounted(fetchData);
</script>
