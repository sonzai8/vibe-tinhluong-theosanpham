<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">{{ $t('employee.title') }}</h2>
        <p class="text-slate-500 font-medium">{{ $t('employee.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-2">
        <UiButton v-if="hasPermission('EMPLOYEE_VIEW')" variant="outline" @click="handleExport">
          <Download class="w-4 h-4" />
          {{ $t('common.export') }}
        </UiButton>
        <div v-if="hasPermission('EMPLOYEE_EDIT')" class="relative group">
          <UiButton variant="outline">
            <Upload class="w-4 h-4" />
            {{ $t('common.import') }}
          </UiButton>
          <div class="absolute right-0 top-full mt-2 w-48 bg-white border border-slate-200 rounded-xl shadow-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50 p-2">
            <button @click="handleDownloadTemplate" class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2">
              <FileDown class="w-4 h-4" />
              {{ $t('common.download_template') }}
            </button>
            <label class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2 cursor-pointer">
              <FileUp class="w-4 h-4" />
              {{ $t('common.import') }}
              <input type="file" class="hidden" accept=".xlsx, .xls" @change="handleImport" />
            </label>
          </div>
        </div>
        <UiButton v-if="hasPermission('EMPLOYEE_EDIT')" @click="openModal()">
          <UserPlus class="w-4 h-4" />
          {{ $t('employee.add_new') }}
        </UiButton>
      </div>
    </div>

    <!-- Filters & Search -->
    <div class="card p-4 flex flex-col md:flex-row gap-4 items-center">
      <div class="relative flex-1 w-full">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
        <input 
          v-model="searchQuery"
          type="text" 
          :placeholder="$t('employee.search_placeholder')" 
          class="w-full pl-10 pr-4 py-2 bg-slate-50 border-none rounded-lg focus:ring-2 focus:ring-primary-500 transition-all font-medium text-sm"
        />
      </div>
      <div class="flex flex-wrap md:flex-nowrap gap-3 w-full md:w-auto">
        <div class="w-full md:w-64">
          <SelectDepartment v-model="filterDept" :placeholder="$t('employee.all_departments')" />
        </div>
        <div class="w-full md:w-64">
          <SelectTeam v-model="filterTeamId" :departmentId="filterDept" :placeholder="$t('employee.all_teams')" />
        </div>
        <select v-model="filterStatus" class="bg-slate-50 border-none rounded-xl px-4 py-3 text-sm font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 transition-all">
          <option value="">{{ $t('employee.all_statuses') }}</option>
          <option value="ACTIVE">{{ $t('common.status_active') }}</option>
          <option value="INACTIVE">{{ $t('common.status_inactive') }}</option>
        </select>
      </div>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-12 flex flex-col items-center justify-center gap-4">
        <div class="w-10 h-10 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">{{ $t('common.loading') }}</p>
      </div>

      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
            <th class="px-6 py-4">{{ $t('production.employee') }}</th>
            <th class="px-6 py-4">{{ $t('employee.contact_cccd') }}</th>
            <th class="px-6 py-4">{{ $t('common.department') }}</th>
            <th class="px-6 py-4">{{ $t('common.role') }}</th>
            <th class="px-6 py-4 text-center">{{ $t('employee.system_access') }}</th>
            <th class="px-6 py-4">{{ $t('attendance.status') }}</th>
            <th class="px-6 py-4 text-right">{{ $t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          <tr v-for="emp in paginatedEmployees" :key="emp.id" class="hover:bg-slate-50/50 transition-colors group">
            <td class="px-6 py-4">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center overflow-hidden border-2 border-white shadow-sm ring-1 ring-slate-100">
                  <img v-if="emp.avatarUrl" :src="`http://localhost:8080${emp.avatarUrl}`" class="w-full h-full object-cover" />
                  <span v-else class="text-slate-400 font-black text-xs">{{ emp.fullName.substring(0, 1).toUpperCase() }}</span>
                </div>
                <div>
                  <NuxtLink 
                    v-if="hasPermission('EMPLOYEE_VIEW') || hasPermission('EMPLOYEE_EDIT') || hasPermission('SYSTEM_ADMIN')" 
                    :to="`/employees/${emp.id}`" 
                    class="font-bold text-slate-900 tracking-tight leading-none mb-1 hover:text-primary-600 transition-colors block"
                  >
                    {{ emp.fullName }}
                  </NuxtLink>
                  <p v-else class="font-bold text-slate-900 tracking-tight leading-none mb-1">{{ emp.fullName }}</p>
                  <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ emp.code }}</p>
                </div>
              </div>
            </td>
            <td class="px-6 py-4">
              <div v-if="emp.phone || emp.citizenId" class="space-y-1">
                <p v-if="emp.phone" class="text-xs font-bold text-slate-600 flex items-center gap-1.5">
                  <Phone class="w-3 h-3 text-slate-400" /> {{ emp.phone }}
                </p>
                <p v-if="emp.citizenId" class="text-[10px] font-black text-slate-400 uppercase tracking-widest flex items-center gap-1.5">
                  <CreditCard class="w-3 h-3" /> {{ emp.citizenId }}
                </p>
              </div>
              <span v-else class="text-xs text-slate-300 italic">{{ $t('employee.not_updated') }}</span>
            </td>
            <td class="px-6 py-4">
              <div class="space-y-1">
                <p class="text-xs font-bold text-slate-600 leading-none">{{ emp.department?.name || emp.team?.department?.name || '---' }}</p>
                <p v-if="emp.team" class="text-[10px] font-black text-emerald-600 uppercase tracking-widest italic">{{ emp.team.name }}</p>
              </div>
            </td>
            <td class="px-6 py-4">
              <span class="text-xs font-bold text-slate-500 uppercase tracking-tighter">{{ emp.role?.name || '---' }}</span>
            </td>
            <td class="px-6 py-4 text-center">
              <span 
                :class="`px-2 py-0.5 rounded-lg text-[9px] font-black uppercase tracking-widest ${emp.canLogin ? 'bg-primary-50 text-primary-700 border border-primary-100' : 'bg-slate-50 text-slate-400 border border-slate-100'}`"
              >
                {{ emp.canLogin ? $t('employee.login_enabled') : $t('employee.login_disabled') }}
              </span>
            </td>
            <td class="px-6 py-4">
              <div class="flex items-center gap-1.5">
                <div :class="`w-2 h-2 rounded-full ${emp.status === 'ACTIVE' ? 'bg-emerald-500 shadow-sm shadow-emerald-200' : 'bg-slate-300'}`"></div>
                <span :class="`text-[10px] font-black uppercase tracking-widest ${emp.status === 'ACTIVE' ? 'text-emerald-600' : 'text-slate-400'}`">
                  {{ emp.status === 'ACTIVE' ? $t('common.status_active') : $t('common.status_inactive') }}
                </span>
              </div>
            </td>
            <td class="px-6 py-4 text-right pr-6">
              <div v-if="hasPermission('EMPLOYEE_VIEW') || hasPermission('EMPLOYEE_EDIT') || hasPermission('SYSTEM_ADMIN')" class="flex items-center justify-end gap-1.5 text-slate-400">
                <button v-if="hasPermission('EMPLOYEE_EDIT') || hasPermission('SYSTEM_ADMIN')" @click="openAuditLogs(emp)" class="p-2 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-all" :title="$t('employee.history_title')">
                  <History class="w-4 h-4" />
                </button>
                <NuxtLink :to="`/employees/${emp.id}`" class="p-2 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition-all inline-flex items-center justify-center" title="Xem chi tiết">
                  <UserCircle class="w-4 h-4" />
                </NuxtLink>
                <button @click="openModal(emp)" class="p-2 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all" :title="$t('employee.edit_profile')">
                  <PencilLine class="w-4 h-4" />
                </button>
                <button @click="openResetPassword(emp)" class="p-2 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition-all" :title="$t('employee.reset_password')">
                  <KeyRound class="w-4 h-4" />
                </button>
                <button @click="handleDelete(emp.id)" class="p-2 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" :title="$t('common.delete')">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
              <span v-else class="text-[10px] font-bold text-slate-300 uppercase italic px-4">{{ $t('employee.view_only') }}</span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination remains same -->
      <div v-if="filteredEmployees.length > 0" class="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
        <div class="flex items-center gap-4">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('production.displaying') }}</span>
          <select v-model="itemsPerPage" class="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 outline-none">
            <option :value="10">10 {{ $t('common.rows') }}</option>
            <option :value="20">20 {{ $t('common.rows') }}</option>
            <option :value="50">50 {{ $t('common.rows') }}</option>
          </select>
          <span class="text-xs font-bold text-slate-500">
            {{ (currentPage - 1) * itemsPerPage + 1 }}-{{ Math.min(currentPage * itemsPerPage, filteredEmployees.length) }} {{ $t('common.of') }} {{ filteredEmployees.length }}
          </span>
        </div>
        <div class="flex items-center gap-2">
          <button 
            @click="currentPage--" 
            :disabled="currentPage === 1"
            class="p-2 rounded-lg bg-white border border-slate-200 text-slate-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 transition-all shadow-sm"
          >
            <ChevronLeft class="w-4 h-4" />
          </button>
          <div class="flex items-center gap-1 overflow-x-auto max-w-[200px] md:max-w-none">
            <button 
              v-for="p in totalPages" 
              :key="p"
              @click="currentPage = p"
              :class="['w-8 h-8 rounded-lg flex items-center justify-center text-xs font-black transition-all shrink-0', 
                       currentPage === p ? 'bg-primary-600 text-white shadow-lg shadow-primary-200' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 shadow-sm']"
            >
              {{ p }}
            </button>
          </div>
          <button 
            @click="currentPage++" 
            :disabled="currentPage === totalPages"
            class="p-2 rounded-lg bg-white border border-slate-200 text-slate-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 transition-all shadow-sm"
          >
            <ChevronRight class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- Employee Modal (Nâng cấp) -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-2xl p-8 animate-in zoom-in duration-200 h-auto max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-8 sticky top-0 bg-white z-10 pb-4">
          <h3 class="text-2xl font-black text-slate-900 tracking-tight">{{ currentEmp.id ? $t('common.update') : $t('common.add_new') }}</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-6 h-6" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
            <UiInput v-model="form.fullName" label="Họ và tên" placeholder="VD: Nguyễn Văn A" required />
            <UiInput v-if="currentEmp.id" v-model="form.code" label="Mã nhân viên" disabled />
            
            <UiInput v-model="form.phone" label="Số điện thoại" placeholder="0xxx xxx xxx" />
            <UiInput v-model="form.citizenId" label="Số CCCD" placeholder="Nhập 12 số CCCD" />

            <div v-if="!currentEmp.id" class="col-span-1 md:col-span-2">
              <UiInput v-model="form.password" type="password" label="Mật khẩu khởi tạo" placeholder="Nhập mật khẩu" required />
            </div>

            <div class="flex flex-col gap-1.5 col-span-1 md:col-span-2">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ $t('attendance.status') }}</label>
              <select v-model="form.status" class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 outline-none focus:bg-white focus:border-slate-200 transition-all">
                <option value="ACTIVE">{{ $t('common.status_active') }}</option>
                <option value="INACTIVE">{{ $t('common.status_inactive') }}</option>
              </select>
            </div>

            <SelectDepartment 
              v-model="form.departmentId" 
              label="Phòng ban trực thuộc" 
              placeholder="Chọn phòng ban"
              :allowAll="false"
            />

            <SelectTeam 
              v-model="form.teamId" 
              label="Tổ đội sản xuất (Ván dán)" 
              placeholder="Chọn tổ đội"
              :departmentId="form.departmentId"
              :allowAll="false"
            />

            <UiSelect 
              v-model="form.roleId" 
              label="Chức vụ - Quyền hạn" 
              :options="roleOptions" 
              placeholder="Chọn chức vụ"
            />

            <!-- Quyền đăng nhập -->
            <div class="col-span-1 md:col-span-2 p-5 bg-primary-50/30 rounded-2xl border border-primary-50 flex items-center justify-between">
              <div>
                <p class="font-black text-slate-900 text-sm tracking-tight italic">Quyền truy cập hệ thống</p>
                <p class="text-[10px] font-bold text-slate-500 uppercase tracking-widest opacity-60">Cho phép nhân viên dùng tài khoản đăng nhập</p>
              </div>
              <label class="relative inline-flex items-center cursor-pointer">
                <input type="checkbox" v-model="form.canLogin" class="sr-only peer">
                <div class="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>
          </div>
          
          <div class="flex gap-4 pt-4 border-t border-slate-100 sticky bottom-0 bg-white">
            <button type="button" @click="showModal = false" class="flex-1 py-3 rounded-xl border border-slate-200 text-slate-600 font-extrabold hover:bg-slate-50 transition-all">{{ $t('common.cancel') }}</button>
            <UiButton type="submit" class="flex-[2] py-3 text-lg font-black" :loading="saving">{{ $t('common.save') }}</UiButton>
          </div>
        </form>
      </div>
    </div>

    <!-- Reset Password Modal -->
    <div v-if="showResetModal" class="fixed inset-0 z-[110] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4 animate-in fade-in duration-300">
      <div class="card w-full max-w-sm p-8 shadow-2xl border-amber-100 animate-in zoom-in duration-200">
        <div class="flex flex-col items-center text-center mb-8">
          <div class="w-16 h-16 bg-amber-100 rounded-2xl flex items-center justify-center text-amber-600 mb-4 shadow-inner">
            <KeyRound class="w-8 h-8" />
          </div>
          <h3 class="text-xl font-black text-slate-900 tracking-tight">{{ $t('employee.reset_pwd_title') }}</h3>
          <p class="text-slate-500 text-sm font-medium mt-1 uppercase tracking-widest">{{ resetForm.name }}</p>
        </div>

        <form @submit.prevent="handleResetPassword" class="space-y-5">
          <UiInput v-model="resetForm.newPassword" type="password" :label="$t('employee.new_pwd')" placeholder="••••••••" required />
          <UiInput v-model="resetForm.confirmPassword" type="password" :label="$t('employee.confirm_pwd')" placeholder="••••••••" required />
          
          <div class="flex flex-col gap-3 pt-4">
            <UiButton type="submit" class="w-full h-12 text-sm font-black shadow-lg shadow-amber-100 bg-amber-600 hover:bg-amber-700 border-amber-600" :loading="saving">{{ $t('employee.update_pwd_btn') }}</UiButton>
            <button type="button" @click="showResetModal = false" class="w-full py-2.5 text-xs font-bold text-slate-400 hover:text-slate-600 transition-all">{{ $t('employee.later') }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Audit Log Modal -->
    <div v-if="showAuditModal" class="fixed inset-0 z-[110] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-3xl p-8 animate-in zoom-in duration-200 h-auto max-h-[85vh] flex flex-col">
        <div class="flex items-center justify-between mb-8">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 bg-slate-100 rounded-2xl flex items-center justify-center text-slate-600">
              <History class="w-6 h-6" />
            </div>
            <div>
              <h3 class="text-xl font-black text-slate-900 tracking-tight">{{ $t('employee.history_title') }}</h3>
              <p class="text-slate-400 text-xs font-black uppercase tracking-widest">{{ activeAuditEmp.fullName }}</p>
            </div>
          </div>
          <button @click="showAuditModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-6 h-6" />
          </button>
        </div>

        <div class="flex-1 overflow-y-auto pr-2 custom-scrollbar space-y-4">
          <div v-if="loadingLogs" class="py-12 flex flex-col items-center justify-center gap-4">
            <div class="w-8 h-8 border-4 border-slate-100 border-t-primary-600 rounded-full animate-spin"></div>
            <p class="text-slate-400 text-xs font-bold animate-pulse">{{ $t('employee.loading_logs') }}</p>
          </div>
          <div v-else-if="auditLogs.length === 0" class="py-12 text-center">
            <p class="text-slate-400 text-sm font-medium italic">{{ $t('employee.no_logs') }}</p>
          </div>
          <div v-else class="relative pl-8 space-y-8 before:content-[''] before:absolute before:left-[15px] before:top-2 before:bottom-2 before:w-[2px] before:bg-slate-100">
            <div v-for="(log, idx) in auditLogs" :key="idx" class="relative">
              <div class="absolute -left-[31px] top-1 w-6 h-6 rounded-full bg-white border-2 border-slate-200 flex items-center justify-center z-10">
                <Clock v-if="idx > 0" class="w-3 h-3 text-slate-400" />
                <CheckCircle2 v-else class="w-4 h-4 text-emerald-500" />
              </div>
              
              <div class="bg-slate-50 border border-slate-100 rounded-2xl p-5 hover:bg-white hover:shadow-md transition-all">
                <div class="flex items-center justify-between mb-3">
                  <span class="px-2.5 py-1 bg-slate-900 text-white text-[9px] font-black uppercase tracking-widest rounded-lg">{{ log.action }}</span>
                  <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ formatDateFull(log.changedAt) }}</span>
                </div>
                
                <div class="space-y-4">
                  <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-lg bg-white border border-slate-200 flex items-center justify-center shadow-sm">
                      <UserCog class="w-4 h-4 text-slate-400" />
                    </div>
                    <div>
                      <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest leading-none mb-1">{{ $t('employee.performed_by') }}</p>
                      <p class="text-xs font-bold text-slate-700 leading-none">{{ log.changedBy || 'Hệ thống' }}</p>
                    </div>
                  </div>

                  <div v-if="log.fieldName" class="grid grid-cols-1 md:grid-cols-2 gap-4 pt-3 border-t border-slate-200/50">
                    <div class="space-y-2">
                       <p class="text-[9px] font-black text-slate-400 uppercase tracking-widest italic">{{ $t('employee.old_value') }} ({{ log.fieldName }})</p>
                       <div class="p-3 bg-red-50 text-red-600 rounded-xl text-xs font-bold break-words min-h-[40px] border border-red-100/50 line-through opacity-60">
                         {{ log.oldValue || 'Trống' }}
                       </div>
                    </div>
                    <div class="space-y-2">
                       <p class="text-[9px] font-black text-primary-600 uppercase tracking-widest italic">{{ $t('employee.new_value') }}</p>
                       <div class="p-3 bg-primary-50 text-primary-700 rounded-xl text-xs font-black break-words min-h-[40px] border border-primary-100/50">
                         {{ log.newValue || 'Trống' }}
                       </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="pt-6 border-t border-slate-100 flex justify-end">
          <UiButton variant="outline" @click="showAuditModal = false" class="font-bold">{{ $t('employee.close_window') }}</UiButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { 
  Plus, UserPlus, PencilLine, Trash2, X, Search, Briefcase, 
  ChevronLeft, ChevronRight, Download, Upload, FileDown, FileUp,
  Phone, CreditCard, KeyRound, History, Clock, UserCog, CheckCircle2, UserCircle
} from 'lucide-vue-next';

const { downloadTemplate: dlTemplate, importExcel, exportExcel } = useExcel();
const { hasPermission } = useAuth();
const { locale } = useI18n();
const localePath = useLocalePath();

const handleExport = async () => {
  try {
    await exportExcel('/employees/export', 'danh_sach_nhan_vien.xlsx');
  } catch (err) {
    alert('Không thể xuất dữ liệu');
  }
};

const route = useRoute();
const { $api } = useNuxtApp();
const employees = ref([]);
const departments = ref([]);
const roles = ref([]);
const teams = ref([]);

const deptOptions = computed(() => departments.value.map(d => ({ value: d.id, label: d.name })));
const roleOptions = computed(() => roles.value.map(r => ({ value: r.id, label: r.name })));
const teamOptions = computed(() => teams.value.map(t => ({ value: t.id, label: t.name })));

const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);

const searchQuery = ref('');
const filterDept = ref('');
const filterStatus = ref('');
const filterTeamId = ref(route.query.teamId ? parseInt(route.query.teamId) : '');

// Pagination
const currentPage = ref(1);
const itemsPerPage = ref(10);
const totalPages = computed(() => Math.ceil(filteredEmployees.value.length / itemsPerPage.value) || 1);

const paginatedEmployees = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return filteredEmployees.value.slice(start, end);
});

watch([searchQuery, filterDept, filterStatus, filterTeamId, itemsPerPage], () => {
  currentPage.value = 1;
});

const currentEmp = ref({});
const form = reactive({
  code: '',
  fullName: '',
  username: '',
  password: '',
  phone: '',
  citizenId: '',
  status: 'ACTIVE',
  departmentId: null,
  teamId: null,
  roleId: null,
  canLogin: false
});

// Logic Reset Password
const showResetModal = ref(false);
const resetForm = reactive({
  id: null,
  name: '',
  newPassword: '',
  confirmPassword: ''
});

const openResetPassword = (emp) => {
  resetForm.id = emp.id;
  resetForm.name = emp.fullName;
  resetForm.newPassword = '';
  resetForm.confirmPassword = '';
  showResetModal.value = true;
};

const handleResetPassword = async () => {
  if (resetForm.newPassword !== resetForm.confirmPassword) {
    alert('Mật khẩu xác nhận không khớp');
    return;
  }
  try {
    await $api.put(`/employees/${resetForm.id}/reset-password`, { newPassword: resetForm.newPassword });
    alert('Đã đặt lại mật khẩu thành công');
    showResetPassword.value = false;
  } catch (err) {
    alert(err.response?.data?.message || 'Có lỗi xảy ra');
  }
};

// Logic Audit Logs
const showAuditModal = ref(false);
const auditLogs = ref([]);
const activeAuditEmp = ref({});
const loadingLogs = ref(false);

const openAuditLogs = async (emp) => {
  activeAuditEmp.value = emp;
  showAuditModal.value = true;
  loadingLogs.value = true;
  try {
    const res = await $api.get(`/employees/${emp.id}/audit-logs`);
    auditLogs.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loadingLogs.value = false;
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const [empRes, roleRes] = await Promise.all([
      $api.get('/employees'),
      $api.get('/roles')
    ]);
    employees.value = empRes.data;
    roles.value = roleRes.data;
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const filteredEmployees = computed(() => {
  return employees.value.filter(e => {
    const matchSearch = e.fullName.toLowerCase().includes(searchQuery.value.toLowerCase()) || 
                      e.code.toLowerCase().includes(searchQuery.value.toLowerCase());
    const matchDept = !filterDept.value || e.department?.id == filterDept.value;
    const matchStatus = !filterStatus.value || e.status === filterStatus.value;
    const matchTeam = !filterTeamId.value || e.team?.id == filterTeamId.value;
    return matchSearch && matchDept && matchStatus && matchTeam;
  });
});

const openModal = (emp = null) => {
  if (emp) {
    currentEmp.value = { ...emp };
    form.code = emp.code;
    form.fullName = emp.fullName;
    form.username = emp.username;
    form.password = '';
    form.phone = emp.phone || '';
    form.citizenId = emp.citizenId || '';
    form.status = emp.status;
    form.departmentId = emp.department?.id || null;
    form.teamId = emp.team?.id || null;
    form.roleId = emp.role?.id || null;
    form.canLogin = emp.canLogin || false;
  } else {
    currentEmp.value = {};
    form.code = '';
    form.fullName = '';
    form.username = '';
    form.password = '';
    form.phone = '';
    form.citizenId = '';
    form.status = 'ACTIVE';
    form.departmentId = null;
    form.teamId = null;
    form.roleId = null;
    form.canLogin = false;
  }
  showModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    const payload = { ...form };
    // Không gửi password nếu để trống trong chế độ chỉnh sửa
    if (currentEmp.value.id && !payload.password) {
      delete payload.password;
    }

    if (currentEmp.value.id) {
      await $api.put(`/employees/${currentEmp.value.id}`, payload);
    } else {
      await $api.post('/employees', payload);
    }
    showModal.value = false;
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn chuyển nhân viên này sang trạng thái Không hoạt động?')) return;
  try {
    await $api.delete(`/employees/${id}`);
    fetchData();
  } catch (err) {
    alert(err.message || 'Có lỗi xảy ra');
  }
};

const handleDownloadTemplate = async () => {
  try {
    await dlTemplate('/employees/download-template', 'mau_nhap_nhan_vien.xlsx');
  } catch (err) {
    alert('Không thể tải file mẫu');
  }
};

const handleImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  try {
    loading.value = true;
    const res = await importExcel('/employees/import', file);
    alert('Nhập dữ liệu thành công');
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || 'Lỗi khi nhập file Excel');
  } finally {
    loading.value = false;
    event.target.value = ''; // Reset input
  }
};

const formatDateFull = (dateStr) => {
  if (!dateStr) return '---';
  return new Date(dateStr).toLocaleString(locale.value === 'vi' ? 'vi-VN' : (locale.value === 'en' ? 'en-US' : 'zh-CN'), {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
};

onMounted(fetchData);
</script>
