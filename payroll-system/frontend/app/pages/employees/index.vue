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
    
    <!-- Common Error Modal -->
    <UiErrorModal
      :show="showErrorModal"
      :title="errorTitle"
      :message="errorMessage"
      :detail="errorDetail"
      @close="showErrorModal = false"
    />
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
          <SelectTeamTree v-model="filterTeamId" :departmentId="filterDept" :placeholder="$t('employee.all_teams')" :allowAll="true" />
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
            <th class="px-6 py-4">ID Chấm công</th>
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
              <div class="flex items-center gap-2 group/zk">
                <input 
                  v-model="emp.zkDeviceId" 
                  type="text" 
                  class="w-20 bg-transparent border-none focus:ring-1 focus:ring-primary-500 rounded px-1 py-0.5 text-xs font-black text-slate-700 transition-all"
                  placeholder="Chưa có"
                  @blur="handleQuickUpdateZkId(emp)"
                  @keyup.enter="$event.target.blur()"
                />
                <div v-if="savingZkId === emp.id" class="w-3 h-3 border-2 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
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
                <p class="text-xs font-bold text-slate-600 leading-none">{{ emp.departmentName || '---' }}</p>
                <p v-if="emp.teamName" class="text-[10px] font-black text-emerald-600 uppercase tracking-widest italic">{{ emp.teamName }}</p>
              </div>
            </td>
            <td class="px-6 py-4">
              <span class="text-xs font-bold text-slate-500 uppercase tracking-tighter">{{ emp.roleName || '---' }}</span>
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
        <div class="flex items-center justify-between mb-8 sticky top-0 bg-white z-10 pb-4 border-b border-slate-100">
          <div class="flex flex-col gap-1">
            <h3 class="text-2xl font-black text-slate-900 tracking-tight">{{ currentEmp.id ? $t('common.update') : $t('common.add_new') }}</h3>
            <div v-if="currentEmp.id" class="flex gap-2">
              <button 
                @click="activeModalTab = 'form'"
                :class="['text-[10px] font-black uppercase tracking-widest px-3 py-1 rounded-full transition-all', 
                         activeModalTab === 'form' ? 'bg-primary-600 text-white shadow-lg' : 'bg-slate-100 text-slate-400 hover:bg-slate-200']"
              >
                Thông tin
              </button>
              <button 
                @click="activeModalTab = 'stats'"
                :class="['text-[10px] font-black uppercase tracking-widest px-3 py-1 rounded-full transition-all', 
                         activeModalTab === 'stats' ? 'bg-primary-600 text-white shadow-lg' : 'bg-slate-100 text-slate-400 hover:bg-slate-200']"
              >
                Thưởng/Phạt
              </button>
            </div>
          </div>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-6 h-6" />
          </button>
        </div>

        <div v-if="activeModalTab === 'form'" class="animate-in fade-in slide-in-from-bottom-2 duration-300">
          <form @submit.prevent="handleSubmit" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
            <UiInput v-model="form.fullName" label="Họ và tên" placeholder="VD: Nguyễn Văn A" required />
            <UiInput v-if="currentEmp.id" v-model="form.code" label="Mã nhân viên" disabled />
            
            <UiInput v-model="form.phone" label="Số điện thoại" placeholder="0xxx xxx xxx" />
            <UiInput v-model="form.citizenId" label="Số CCCD" placeholder="Nhập 12 số CCCD" />
            <UiInput v-model="form.zkDeviceId" label="ID Máy chấm công (ZKTeco)" placeholder="VD: 101" />

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

            <UiInput 
              v-if="form.status !== 'ACTIVE'"
              v-model="form.lastWorkingDate" 
              type="date" 
              :label="$t('last_working_date')" 
            />

            <SelectDepartment 
              v-model="form.departmentId" 
              label="Phòng ban trực thuộc" 
              placeholder="Chọn phòng ban"
              :allowAll="false"
            />

            <SelectTeamTree 
              v-model="form.teamId" 
              label="Tổ đội sản xuất (Ván dán)" 
              placeholder="Chọn tổ đội"
              :departmentId="form.departmentId"
            />

            <UiSelect 
              v-model="form.roleId" 
              label="Chức vụ - Quyền hạn" 
              :options="roleOptions" 
              placeholder="Chọn chức vụ"
            />

            <!-- Configuration Lương -->
            <div class="col-span-1 md:col-span-2 mt-4 space-y-6">
              <h4 class="text-xs font-black text-slate-400 uppercase tracking-[0.2em] border-b border-slate-100 pb-2">
                {{ $t('employee.salary_config') }}
              </h4>
              
              <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div class="flex flex-col gap-1.5">
                  <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">{{ $t('employee.salary_type') }}</label>
                  <select v-model="form.salaryType" class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 outline-none focus:bg-white focus:border-slate-200 transition-all">
                    <option value="PRODUCT_BASED">{{ $t('employee.salary_type_product') }}</option>
                    <option value="FIXED_MONTHLY">{{ $t('employee.salary_type_monthly') }}</option>
                    <option value="FIXED_DAILY">{{ $t('employee.salary_type_daily') }}</option>
                  </select>
                </div>

                <UiInput 
                  v-if="form.salaryType !== 'PRODUCT_BASED'"
                  v-model="form.baseSalaryConfig" 
                  type="number"
                  :label="$t('employee.base_salary')" 
                  placeholder="0"
                />

                <UiInput 
                  v-model="form.insuranceSalaryConfig" 
                  type="number"
                  :label="$t('employee.insurance_salary')" 
                  placeholder="0"
                />
              </div>
            </div>

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

        <!-- Stats Tab -->
        <div v-else-if="activeModalTab === 'stats'" class="animate-in fade-in slide-in-from-bottom-2 duration-300 space-y-8">
          <div v-if="loadingStats" class="py-20 flex flex-col items-center justify-center gap-4">
            <div class="w-10 h-10 border-4 border-slate-100 border-t-primary-600 rounded-full animate-spin"></div>
            <p class="text-slate-400 text-xs font-bold animate-pulse">Đang tải thống kê...</p>
          </div>
          <div v-else class="space-y-8">
            <!-- Stats Summary Cards -->
            <div class="grid grid-cols-3 gap-4">
              <div class="p-4 bg-emerald-50 rounded-2xl border border-emerald-100">
                <p class="text-[10px] font-black text-emerald-600 uppercase tracking-widest mb-1">Tổng thưởng</p>
                <p class="text-lg font-black text-emerald-700 leading-none">+{{ (employeeStats.totalBonusAmount || 0).toLocaleString() }}đ</p>
                <p class="text-[10px] font-bold text-emerald-500 mt-1">{{ employeeStats.bonusCount || 0 }} lần</p>
              </div>
              <div class="p-4 bg-red-50 rounded-2xl border border-red-100">
                <p class="text-[10px] font-black text-red-600 uppercase tracking-widest mb-1">Tổng phạt</p>
                <p class="text-lg font-black text-red-700 leading-none">{{ (employeeStats.totalPenaltyAmount || 0).toLocaleString() }}đ</p>
                <p class="text-[10px] font-bold text-red-500 mt-1">{{ employeeStats.penaltyCount || 0 }} lần</p>
              </div>
              <div class="p-4 bg-slate-900 rounded-2xl shadow-xl shadow-slate-200">
                <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Thực nhận</p>
                <p class="text-lg font-black text-white leading-none">{{ (employeeStats.netAmount || 0).toLocaleString() }}đ</p>
                <p class="text-[10px] font-bold text-slate-500 mt-1 italic">Khấu trừ lương</p>
              </div>
            </div>

            <!-- History List -->
            <div class="space-y-4">
              <h4 class="text-xs font-black text-slate-400 uppercase tracking-[0.2em] px-1">Lịch sử chi tiết</h4>
              <div v-if="employeeStats.history?.length === 0" class="py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-dashed border-slate-200">
                <p class="text-xs font-medium italic">Chưa có bản ghi thưởng phạt nào</p>
              </div>
              <div v-else class="space-y-3">
                <div v-for="h in employeeStats.history" :key="h.id" class="p-4 bg-white border border-slate-100 rounded-xl hover:shadow-sm transition-all flex items-center justify-between group">
                  <div class="flex items-center gap-4">
                    <div :class="['w-10 h-10 rounded-lg flex items-center justify-center font-black text-xs shrink-0', h.amount > 0 ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700']">
                      {{ h.amount > 0 ? '+' : '-' }}
                    </div>
                    <div>
                      <p class="font-bold text-slate-900 text-sm leading-none mb-1">{{ h.reason }}</p>
                      <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ h.recordDate }}</p>
                    </div>
                  </div>
                  <span :class="['font-black text-sm', h.amount > 0 ? 'text-emerald-600' : 'text-red-600']">
                    {{ h.amount > 0 ? '+' : '' }}{{ h.amount.toLocaleString() }}đ
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Import Preview Dialog -->
    <ImportPreviewDialog
      :show="showImportPreview"
      :data="previewData"
      :errors="importErrors"
      :loading="importing"
      :columns="importCols"
      :title="$t('employee.preview_title')"
      @close="showImportPreview = false"
      @confirm="handleConfirmImport"
    />

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
    triggerError('Lỗi xuất file', 'Không thể xuất danh sách nhân viên ra Excel.', err.message);
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

const loading = ref(true);
const saving = ref(false);
const showModal = ref(false);
const activeModalTab = ref('form');
const loadingStats = ref(false);
const employeeStats = ref({});

// Import Preview State
const showImportPreview = ref(false);
const previewData = ref([]);
const importErrors = ref([]);
const importing = ref(false);

const importCols = [
  { label: 'Mã NV', key: 'code' },
  { label: 'Họ Tên', key: 'fullName' },
  { label: 'SĐT', key: 'phone' },
  { label: 'CCCD', key: 'citizenId' }
];

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

const savingZkId = ref(null);
const handleQuickUpdateZkId = async (emp) => {
  if (savingZkId.value === emp.id) return;
  savingZkId.value = emp.id;
  try {
    await $api.patch(`/employees/${emp.id}/zk-device-id`, emp.zkDeviceId || '');
    // alert('Đã cập nhật ID chấm công');
  } catch (err) {
    triggerError('Lỗi cập nhật', 'Không thể cập nhật ID chấm công.', err.message);
  } finally {
    savingZkId.value = null;
  }
};

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
  canLogin: false,
  salaryType: 'PRODUCT_BASED',
  baseSalaryConfig: 0,
  insuranceSalaryConfig: 0,
  zkDeviceId: '',
  lastWorkingDate: ''
});

watch(activeModalTab, (newTab) => {
  if (newTab === 'stats' && currentEmp.value.id) {
    fetchEmployeeStats(currentEmp.value.id);
  }
});

const fetchEmployeeStats = async (empId) => {
  loadingStats.value = true;
  try {
    const res = await $api.get(`/penalty-bonuses/employee/${empId}/stats`);
    employeeStats.value = res.data;
  } catch (err) {
    console.error(err);
  } finally {
    loadingStats.value = false;
  }
};

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
    triggerError('Lỗi xác thực', 'Mật khẩu xác nhận không khớp.');
    return;
  }
  try {
    await $api.put(`/employees/${resetForm.id}/reset-password`, { newPassword: resetForm.newPassword });
    // Dùng alert cho thành công cũng được, hoặc triggerSuccess nếu có. Nhưng ở đây user yêu cầu Error Modal cho lỗi.
    alert('Đã đặt lại mật khẩu thành công');
    showResetModal.value = false;
  } catch (err) {
    triggerError('Lỗi đặt lại mật khẩu', 'Hệ thống không thể cập nhật mật khẩu mới.', err.response?.data?.message || err.message);
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
    const matchDept = !filterDept.value || e.departmentId == filterDept.value;
    const matchStatus = !filterStatus.value || e.status === filterStatus.value;
    const matchTeam = !filterTeamId.value || e.teamId == filterTeamId.value;
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
    form.departmentId = emp.departmentId || null;
    form.teamId = emp.teamId || null;
    form.roleId = emp.roleId || null;
    form.canLogin = emp.canLogin || false;
    form.salaryType = emp.salaryType || 'PRODUCT_BASED';
    form.baseSalaryConfig = emp.baseSalaryConfig || 0;
    form.insuranceSalaryConfig = emp.insuranceSalaryConfig || 0;
    form.zkDeviceId = emp.zkDeviceId || '';
    form.lastWorkingDate = emp.lastWorkingDate || '';
  } else {
    currentEmp.value = {};
    activeModalTab.value = 'form';
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
    form.salaryType = 'PRODUCT_BASED';
    form.baseSalaryConfig = 0;
    form.insuranceSalaryConfig = 0;
    form.zkDeviceId = '';
    form.lastWorkingDate = '';
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
    triggerError('Lỗi lưu nhân viên', 'Đã xảy ra lỗi khi lưu thông tin nhân viên.', err.response?.data?.message || err.message);
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
    triggerError('Lỗi xóa nhân viên', 'Không thể xóa nhân viên này khỏi hệ thống.', err.message || 'Có lỗi xảy ra');
  }
};

const handleDownloadTemplate = async () => {
  try {
    await dlTemplate('/employees/download-template', 'mau_nhap_nhan_vien.xlsx');
  } catch (err) {
    triggerError('Lỗi tải mẫu', 'Không thể tải xuống tệp tin mẫu nhập liệu.', err.message);
  }
};

const handleImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('file', file);

  try {
    loading.value = true;
    const res = await $api.post('/employees/import/preview', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    
    previewData.value = res.data.data;
    importErrors.value = res.data.errors;
    showImportPreview.value = true;
  } catch (err) {
    const msg = err.response?.data?.message || 'Hệ thống không thể đọc nội dung file Excel này. Vui lòng kiểm tra lại định dạng hoặc template.';
    triggerError('Lỗi nhập dữ liệu', msg, err.response?.data?.errors?.join('\n') || err.message);
  } finally {
    loading.value = false;
    event.target.value = ''; // Reset input
  }
};

const handleConfirmImport = async () => {
  if (previewData.value.length === 0) return;
  
  importing.value = true;
  try {
    await $api.post('/employees/import/confirm', previewData.value);
    alert('Nhập dữ liệu thành công');
    showImportPreview.value = false;
    fetchData();
  } catch (err) {
    triggerError('Lỗi lưu dữ liệu', 'Đã xảy ra lỗi khi lưu danh sách nhân viên mới vào hệ thống.', err.response?.data?.message || err.message);
  } finally {
    importing.value = false;
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
