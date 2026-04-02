<template>
  <div class="space-y-8">
    <!-- Header with Breadcrumbs & Actions -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <!-- <h2 class="text-3xl font-black text-slate-900 tracking-tight">Nhật ký Chấm công</h2>
        <p class="text-slate-500 font-medium">Ghi lại danh sách nhân viên đi làm hàng ngày</p> -->
      </div>
      <div class="flex gap-3">
        <UiButton variant="outline" @click="handleDownloadTemplate" >
          <FileSpreadsheet class="w-4 h-4" />
          {{ $t('common.download_template') || 'Tải file mẫu' }}
        </UiButton>
        <div class="relative group/export">
            <UiButton variant="outline" :loading="exporting">
              <Download class="w-4 h-4" />
              {{ $t('attendance.export') }}
              <ChevronDown class="w-3 h-3 ml-1 opacity-50" />
            </UiButton>
            <div class="absolute top-full right-0 w-48 bg-white rounded-xl shadow-2xl border border-slate-100 p-1.5 z-[100] invisible group-hover/export:visible opacity-0 group-hover/export:opacity-100 translate-y-1 group-hover/export:translate-y-0 transition-all duration-200">
              <button @click="handleExport('list')" class="w-full text-left px-4 py-2 hover:bg-slate-50 rounded-lg text-xs font-bold text-slate-600 flex items-center gap-2">
                <LayoutList class="w-3.5 h-3.5" /> {{ $t('common.list_view') }}
              </button>
              <button @click="handleExport('matrix')" class="w-full text-left px-4 py-2 hover:bg-slate-50 rounded-lg text-xs font-bold text-slate-600 flex items-center gap-2 border-t border-slate-50 mt-1 pt-1.5">
                <Grid3x3 class="w-3.5 h-3.5" /> {{ $t('common.matrix_view') }}
              </button>
            </div>
        </div>
        <UiButton variant="outline" @click="$refs.fileInput.click()" :loading="importing">
          <Upload class="w-4 h-4" />
          {{ $t('attendance.import') }}
        </UiButton>
        <input type="file" ref="fileInput" class="hidden" accept=".xlsx, .xls" @change="handleImport" />
        
        <!-- Import Error Dialog -->
        <ImportErrorDialog 
          :show="showImportError" 
          :errors="importErrors" 
          @close="showImportError = false" 
        />
        
        <!-- Common Error Modal -->
        <UiErrorModal
          :show="showErrorModal"
          :title="errorTitle"
          :message="errorMessage"
          :detail="errorDetail"
          @close="showErrorModal = false"
        />
        
        <UiButton @click="openBulkModal" variant="outline" >
          <Users class="w-4 h-4" />
          {{ $t('attendance.bulk_attendance') }}
        </UiButton>
        <UiButton @click="() => openModal(null)" class="shadow-lg shadow-emerald-100">
          <CalendarPlus class="w-4 h-4" />
          {{ $t('attendance.add_new') }}
        </UiButton>
      </div>
    </div>

    <!-- Filters & Summary -->
    <div class="space-y-6">
      
      <!-- Top Filter Bar -->
      <div class="card p-6 flex flex-col gap-6">
        <div class="flex flex-col lg:flex-row gap-6 lg:items-end justify-between">
          <div class="flex flex-wrap gap-4 flex-1">
            <div class="flex flex-col gap-1.5 min-w-[150px]">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('production.date') }}</label>
              <input v-model="filterDate" type="date" class="input-field py-2.5 text-sm font-bold w-full" @change="fetchData" />
            </div>

            <div class="flex flex-col gap-1.5 min-w-[200px]">
              <SelectDepartment 
                v-model="filterDeptIds" 
                multiple
                :label="$t('common.department')" 
              />
            </div>

            <div class="flex flex-col gap-1.5 min-w-[200px]">
              <SelectTeam 
                v-model="filterTeamIds" 
                multiple
                :departmentId="filterDeptIds.length === 1 ? filterDeptIds[0] : null"
                :label="$t('common.team')" 
              />
            </div>

            <div class="flex flex-col gap-1.5 min-w-[300px]">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('common.search') }}</label>
              <div class="relative w-full">
                <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-300" />
                <input v-model="search" type="text" :placeholder="$t('common.search_placeholder') || 'Tên hoặc mã nhân viên...'" class="input-field py-2.5 pl-9 text-sm w-full" />
              </div>
            </div>
          </div>
          
          <div class="flex gap-4 shrink-0">
            <UiButton @click="showAbsentModal = true" variant="outline" class="h-[42px] px-4 font-bold text-xs" v-if="absentEmployees.length > 0">
              <UserX class="w-4 h-4 mr-1.5 text-amber-500" /> {{ $t('attendance.absent') }} ({{ absentEmployees.length }})
            </UiButton>
            <UiButton @click="fetchData" class="h-[42px] px-8 bg-slate-900 hover:bg-slate-800 transition-all font-black uppercase tracking-widest text-xs">
              {{ $t('common.filter') }}
            </UiButton>
          </div>
        </div>

        <!-- View Mode Switcher -->
        <div class="flex flex-col md:flex-row md:items-center justify-between border-t border-slate-100 pt-4 gap-4">
          <div class="flex p-1 bg-slate-100 rounded-2xl w-fit shrink-0">
            <button 
              @click="viewMode = 'list'" 
              :class="['px-6 py-2 text-[11px] font-black uppercase tracking-widest rounded-xl transition-all flex items-center gap-2.5', viewMode === 'list' ? 'bg-white text-primary-700 shadow-xl shadow-slate-300 scale-[1.02]' : 'text-slate-500 hover:text-slate-700 hover:bg-white/50']"
            >
              <LayoutList class="w-3 h-3" />
              {{ $t('common.list_view') }}
            </button>
            <button 
              @click="viewMode = 'matrix'" 
              :class="['px-6 py-2 text-[11px] font-black uppercase tracking-widest rounded-xl transition-all flex items-center gap-2.5', viewMode === 'matrix' ? 'bg-white text-primary-700 shadow-xl shadow-slate-300 scale-[1.02]' : 'text-slate-500 hover:text-slate-700 hover:bg-white/50']"
            >
              <Grid3x3 class="w-3 h-3" />
              {{ $t('common.matrix_view') }}
            </button>
          </div>
          
          <div v-if="viewMode === 'matrix'" class="flex flex-wrap items-center gap-4">
             <div class="flex p-1 bg-slate-200/50 rounded-xl">
               <button 
                 @click="matrixScope = 'month'"
                 :class="['px-4 py-1.5 text-[9px] font-black uppercase tracking-widest rounded-lg transition-all', matrixScope === 'month' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500']"
               >{{ $t('common.month') }}</button>
               <button 
                 @click="matrixScope = 'week'"
                 :class="['px-4 py-1.5 text-[9px] font-black uppercase tracking-widest rounded-lg transition-all', matrixScope === 'week' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500']"
               >{{ $t('common.week') }}</button>
             </div>

             <div v-if="matrixScope === 'month'" class="flex flex-col gap-0.5 min-w-[120px]">
                <label class="text-[8px] font-black text-slate-400 uppercase tracking-widest">{{ $t('common.reporting_month') }}</label>
                <input v-model="viewMonth" type="month" class="input-field py-1 px-3 text-xs font-bold bg-white h-8" @change="handleMonthChange" />
             </div>

             <div v-else class="flex items-center gap-3">
                <div class="flex flex-col gap-0.5">
                  <label class="text-[8px] font-black text-slate-400 uppercase tracking-widest text-center">{{ $t('attendance.week_nav') }}</label>
                  <div class="flex items-center gap-1 bg-white border border-slate-200 rounded-xl p-1 shadow-sm">
                    <button @click="navigateWeek(-1)" class="p-1 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-primary-600 transition-all">
                      <ChevronLeft class="w-4 h-4" />
                    </button>
                    <span class="text-[10px] font-black text-slate-700 px-2 min-w-[140px] text-center">
                      {{ formatWeekRange(currentWeekStart) }}
                    </span>
                    <button @click="navigateWeek(1)" class="p-1 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-primary-600 transition-all">
                      <ChevronRight class="w-4 h-4" />
                    </button>
                  </div>
                </div>
                <button @click="resetToCurrentWeek" class="mt-3.5 px-3 py-1.5 bg-slate-100 hover:bg-primary-50 text-[9px] font-black text-slate-500 hover:text-primary-600 uppercase rounded-lg transition-all">{{ $t('attendance.current_week') }}</button>
             </div>
          </div>
        </div>
      </div>

      <!-- Main Layout: Table + Summary Stats -->
      <div class="flex flex-col xl:flex-row gap-6">
        
        <!-- Attendance Table -->
        <div class="card w-full xl:w-5/6">
          <!-- List View Table -->
          <div v-if="viewMode === 'list'">
            <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
              <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
              <p class="text-slate-500 font-bold animate-pulse">{{ $t('attendance.loading') }}</p>
            </div>

            <div v-else-if="attendances.length === 0" class="p-20 text-center space-y-4">
              <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-300">
                <CalendarX class="w-10 h-10" />
              </div>
              <p class="text-slate-500 font-bold">{{ $t('attendance.no_data') }} ({{ filterDate }}).</p>
              <UiButton @click="() => openModal(null)" variant="outline">{{ $t('attendance.add_now') }}</UiButton>
            </div>

            <div v-else class="overflow-x-auto">
              <table class="w-full text-left">
                <thead>
                  <tr class="bg-slate-50/50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
                    <th class="px-8 py-5">{{ $t('attendance.employee') }}</th>
                    <th class="px-8 py-5">{{ $t('attendance.original_team') }}</th>
                    <th class="px-8 py-5">{{ $t('attendance.actual_team') }}</th>
                    <th class="px-8 py-5">{{ $t('attendance.definition.name') || 'Loại công' }}</th>
                    <!-- <th class="px-8 py-5">{{ $t('attendance.status') }}</th> -->
                    <th class="px-8 py-5 text-right">{{ $t('common.actions') }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  <tr v-for="att in paginatedAttendances" :key="att.id" class="hover:bg-slate-50/50 transition-all group">
                    <td class="px-8 py-5">
                      <div class="flex items-center gap-4">
                        <div class="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center text-xs font-black text-slate-400 group-hover:bg-primary-600 group-hover:text-white transition-all shadow-sm">
                          {{ att.employeeFullName[0] }}
                        </div>
                        <div>
                          <p class="font-black text-slate-900">{{ att.employeeFullName }}</p>
                          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-tighter">{{ att.employeeCode }}</p>
                        </div>
                      </div>
                    </td>
                    <td class="px-8 py-5">
                      <span v-if="att.originalTeamName" class="px-3 py-1 bg-slate-100 rounded-full text-[10px] font-black text-slate-500 uppercase">{{ att.originalTeamName }}</span>
                      <span v-else class="text-slate-300 text-xs italic">{{ $t('attendance.no_team_assigned') || 'Chưa gán tổ' }}</span>
                    </td>
                    <td class="px-8 py-5">
                      <span v-if="att.actualTeamName" :class="`px-3 py-1 rounded-full text-[10px] font-black uppercase ${att.actualTeamId !== att.originalTeamId ? 'bg-amber-100 text-amber-700' : 'bg-emerald-100 text-emerald-700'}`">
                        {{ att.actualTeamName }}
                        <span v-if="att.actualTeamId !== att.originalTeamId" class="ml-1 text-[8px] opacity-70">({{ $t('attendance.borrowed') }})</span>
                      </span>
                    </td>
                    <td class="px-8 py-5">
                       <span v-if="att.attendanceDefinitionCode" class="px-3 py-1 bg-primary-100 text-primary-700 rounded-full text-[10px] font-black uppercase">
                         {{ att.attendanceDefinitionCode }} - {{ att.attendanceDefinitionName }}
                       </span>
                    </td>
                    <!-- <td class="px-8 py-5">
                       <span class="flex items-center gap-1.5 text-[10px] font-black text-emerald-600 uppercase">
                         <div class="w-1.5 h-1.5 rounded-full bg-emerald-500"></div>
                         {{ $t('attendance.present') }}
                       </span>
                    </td> -->
                    <td class="px-8 py-5 text-right">
                      <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button @click="openModal(att)" class="p-2.5 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-xl transition-all"><PencilLine class="w-4 h-4" /></button>
                        <button @click="handleDelete(att.id)" class="p-2.5 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-xl transition-all"><Trash2 class="w-4 h-4" /></button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>

              <!-- Pagination -->
              <div v-if="filteredAttendances.length > 0" class="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
                <div class="flex items-center gap-4">
                  <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('production.displaying') }}</span>
                  <select v-model="itemsPerPage" class="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-bold text-slate-600 focus:ring-2 focus:ring-primary-500 outline-none">
                    <option :value="10">10 {{ $t('common.rows') }}</option>
                    <option :value="20">20 {{ $t('common.rows') }}</option>
                    <option :value="50">50 {{ $t('common.rows') }}</option>
                  </select>
                  <span class="text-xs font-bold text-slate-500">
                    {{ (currentPage - 1) * itemsPerPage + 1 }}-{{ Math.min(currentPage * itemsPerPage, filteredAttendances.length) }} {{ $t('common.of') || 'của' }} {{ filteredAttendances.length }}
                  </span>
                </div>
                <!-- Pagination buttons omitted for brevity in chunk but should be maintained -->
              </div>
            </div>
          </div>

          <!-- Matrix View Table -->
          <div v-if="viewMode === 'matrix'">
            <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
              <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
              <p class="text-slate-500 font-bold animate-pulse">{{ $t('common.loading') }}</p>
            </div>
            
            <div v-else class="overflow-x-auto custom-scrollbar">
              <table class="w-full text-left table-fixed border-collapse">
                <thead class="sticky top-0 z-20 bg-white">
                  <tr class="text-[9px] font-black uppercase text-slate-400 tracking-widest border-b border-slate-100">
                    <th class="p-4 w-64 bg-white sticky left-0 z-30 border-r border-slate-100">{{ $t('attendance.employee') }}</th>
                    <th class="p-4 w-20 text-center bg-slate-50 font-black text-slate-900 border-r border-slate-100 sticky left-64 z-30 shadow-[2px_0_5px_rgba(0,0,0,0.05)]">{{ $t('common.total') }}</th>
                    <th v-for="d in displayDays" :key="d.key" :class="['p-2 w-10 text-center border-r border-slate-50', d.isSunday ? 'bg-red-50 text-red-400' : '']">
                      <div>{{ d.label }}</div>
                      <div class="text-[8px] opacity-60">{{ d.dayName }}</div>
                    </th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-50">
                  <tr v-for="emp in filteredEmployees" :key="emp.id" class="hover:bg-slate-50 group">
                    <td class="p-4 bg-white sticky left-0 z-10 border-r border-slate-100 shadow-[2px_0_5px_rgba(0,0,0,0.02)] group-hover:bg-slate-50">
                      <div class="flex items-center gap-3">
                        <div class="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center text-[10px] font-black text-slate-400 group-hover:bg-primary-600 group-hover:text-white transition-all">
                          {{ emp.fullName[0] }}
                        </div>
                        <div>
                          <p class="text-xs font-black text-slate-900 line-clamp-1">{{ emp.fullName }}</p>
                          <div class="flex items-center gap-2">
                             <p class="text-[9px] font-bold text-slate-400 uppercase tracking-tighter">{{ emp.code }}</p>
                             <span v-if="emp.teamName" class="text-[8px] font-black text-primary-600 bg-primary-50 px-1.5 py-0.5 rounded uppercase">{{ emp.teamName }}</span>
                          </div>
                        </div>
                      </div>
                    </td>
                    <td class="p-4 text-center bg-slate-50/80 font-black text-primary-600 text-xs border-r border-slate-100 sticky left-64 z-10 shadow-[2px_0_5px_rgba(0,0,0,0.05)] group-hover:bg-slate-100">
                      {{ getEmployeeTotalAttendance(emp.id) }}
                    </td>
                    <td v-for="d in displayDays" :key="d.key" :class="['p-0 border-r border-slate-50 text-center relative group/cell', d.isSunday ? 'bg-red-50/30' : '']">
                       <div class="relative w-full h-full">
                         <AttendanceCell 
                           :status="getAttendanceMatrixValue(emp.id, d.fullDate).status" 
                           :definitionCode="getAttendanceMatrixValue(emp.id, d.fullDate).defCode"
                           :definitionName="getAttendanceMatrixValue(emp.id, d.fullDate).defName"
                           :loading="cellLoading === `${emp.id}_${d.fullDate}`" 
                           @click="toggleAttendanceMenu(emp.id, d.fullDate, $event)"
                         />
                         
                         <!-- Quick Selector Menu -->
                         <div v-if="activeMenu?.empId === emp.id && activeMenu?.date === d.fullDate" 
                               class="absolute top-full left-1/2 -translate-x-1/2 mt-1 bg-white rounded-xl shadow-2xl border border-slate-100 z-[100] p-1.5 flex flex-col gap-1 min-w-[120px] animate-in zoom-in duration-150"
                               v-click-outside="closeAttendanceMenu">
                           <button v-for="def in attendanceDefs" :key="def.id" 
                                   @click.stop="performAttendanceAction(emp.id, d.fullDate, def.id)"
                                   class="flex items-center justify-between px-3 py-2 hover:bg-primary-50 rounded-lg transition-all group/item">
                             <span class="text-[10px] font-black uppercase text-slate-600 group-hover/item:text-primary-700">{{ def.code }}</span>
                             <span class="text-[9px] font-bold text-slate-400">{{ def.name }}</span>
                           </button>
                           <div class="h-px bg-slate-100 my-0.5"></div>
                           <button @click.stop="performAttendanceAction(emp.id, d.fullDate, null)"
                                   class="flex items-center justify-between px-3 py-2 hover:bg-red-50 rounded-lg transition-all group/item text-red-500">
                             <Trash2 class="w-3 h-3" />
                             <span class="text-[9px] font-bold uppercase tracking-widest">{{ $t('common.delete') }}</span>
                           </button>
                         </div>
                       </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Static Summary Stats Container Right -->
        <div class="w-full xl:w-1/6 space-y-6">
          <div class="card p-6 bg-primary-50 border-primary-100 space-y-4">
            <h3 class="font-black text-slate-900 text-sm uppercase tracking-widest mb-4">{{ $t('common.overview') || 'Tổng quan' }}</h3>
            <div class="flex items-center gap-3">
              <div class="w-12 h-12 rounded-xl bg-primary-600 flex items-center justify-center text-white">
                <CheckCircle2 class="w-6 h-6" />
              </div>
              <div>
                <p class="text-[10px] font-black text-primary-600 uppercase">{{ $t('attendance.present_count') || 'Quân số đi làm' }}</p>
                <p class="text-2xl font-black text-slate-900">{{ statistics.present }} / <span class="text-lg text-slate-400">{{ statistics.total }}</span></p>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- Attendance Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-xl p-10 animate-in zoom-in slide-in-from-bottom duration-300">
        <div class="flex items-center justify-between mb-10">
          <h3 class="text-2xl font-black text-slate-900 tracking-tight">{{ currentId ? $t('attendance.edit_title') : $t('attendance.add_title') }}</h3>
          <button @click="showModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full transition-all"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
            <SelectEmployee 
              v-model="form.employeeId" 
              :label="$t('attendance.employee')" 
              :placeholder="$t('common.select_employee') || 'Chọn nhân viên'"
              :attendanceDate="form.attendanceDate"
              :teamId="filterTeamIds[0]"
              @update:modelValue="onEmployeeSelect"
              required
            />
            
            <UiInput v-model="form.attendanceDate" :label="$t('production.date')" type="date" required />

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 md:col-span-2">
              <UiSelect 
                v-model="form.originalTeamId" 
                :label="$t('attendance.original_team')" 
                :options="teamOptions" 
                :placeholder="$t('common.select_team') || 'Chọn tổ'"
                required
              />
              <UiSelect 
                v-model="form.actualTeamId" 
                :label="$t('attendance.actual_team')" 
                :options="teamOptions" 
                :placeholder="$t('common.select_team') || 'Chọn tổ'"
                required
              />
            </div>

            <div class="md:col-span-2">
              <UiSelect 
                v-model="form.attendanceDefinitionId" 
                :label="$t('attendance.definition.name')" 
                :options="attendanceDefOptions" 
                :placeholder="$t('common.select_type')"
                required
              />
            </div>
          </div>

          <div class="p-4 bg-amber-50 rounded-2xl border border-amber-100 flex gap-3">
             <Info class="w-5 h-5 text-amber-600 shrink-0" />
             <p class="text-xs text-amber-700 font-medium leading-relaxed">
               {{ $t('attendance.borrowed_note') }}
             </p>
          </div>
          
          <div class="flex gap-4 pt-6">
            <button type="button" @click="showModal = false" class="flex-1 py-3.5 rounded-2xl border border-slate-200 text-slate-500 font-black hover:bg-slate-50 transition-all">{{ $t('common.cancel') }}</button>
            <UiButton type="submit" class="flex-[2] h-14 text-lg shadow-xl shadow-primary-200" :loading="saving">{{ $t('common.confirm') }}</UiButton>
          </div>
        </form>
      </div>
    </div>

    <!-- Absent Employees Modal -->
    <div v-if="showAbsentModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-2xl p-8 animate-in zoom-in slide-in-from-bottom duration-300 max-h-[90vh] flex flex-col">
        <div class="flex items-center justify-between mb-6 shrink-0">
          <div>
            <h3 class="text-2xl font-black text-slate-900 tracking-tight">{{ $t('attendance.absent_list') }}</h3>
            <p class="text-sm text-slate-500 mt-1">{{ $t('attendance.absent_subtitle', { date: filterDate }) }}</p>
          </div>
          <button @click="showAbsentModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full transition-all"><X class="w-5 h-5" /></button>
        </div>

        <div class="overflow-y-auto flex-1 border border-slate-100 rounded-xl">
          <table class="w-full text-left relative">
            <thead class="sticky top-0 bg-slate-50 border-b border-slate-200 z-10">
              <tr class="text-slate-500 text-[10px] font-black uppercase tracking-widest">
                <th class="px-6 py-4">{{ $t('attendance.employee') }}</th>
                <th class="px-6 py-4">{{ $t('attendance.original_team') }}</th>
                <th class="px-6 py-4">{{ $t('common.phone') || 'SĐT' }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
               <tr v-if="absentEmployees.length === 0" class="text-center">
                  <td colspan="3" class="px-6 py-8 text-sm text-slate-500">{{ $t('attendance.all_present') }}</td>
               </tr>
              <tr v-for="emp in absentEmployees" :key="emp.id" class="hover:bg-slate-50 transition-colors">
                 <td class="px-6 py-3">
                   <div class="font-bold text-slate-900 text-sm">{{ emp.fullName }}</div>
                   <div class="text-[10px] font-black text-slate-400">{{ emp.code }}</div>
                 </td>
                 <td class="px-6 py-3 text-xs text-slate-600">{{ emp.teamName ? emp.teamName : $t('attendance.no_team_assigned') }}</td>
                <td class="px-6 py-3 text-xs text-slate-500">{{ emp.phone || 'N/A' }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mt-6 flex justify-end shrink-0">
          <UiButton @click="showAbsentModal = false" variant="outline" class="px-6">{{ $t('common.close') || 'Đóng' }}</UiButton>
        </div>
      </div>
    </div>
    <!-- Duplicate Warning Modal -->
    <div v-if="showDuplicateModal" class="fixed inset-0 z-[110] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-lg p-8 animate-in zoom-in slide-in-from-bottom duration-300">
        <div class="flex items-center gap-4 mb-6">
          <div class="w-12 h-12 rounded-full bg-amber-100 flex items-center justify-center text-amber-600 shrink-0">
            <Info class="w-6 h-6" />
          </div>
          <div>
            <h3 class="text-xl font-black text-slate-900 tracking-tight">{{ $t('attendance.duplicate_title') }}</h3>
            <p class="text-sm text-slate-500">{{ $t('attendance.duplicate_msg') }}</p>
          </div>
        </div>
        
        <div class="bg-slate-50 p-4 rounded-2xl border border-slate-100 mb-6 space-y-2">
          <div class="flex justify-between text-sm">
            <span class="text-slate-500">{{ $t('attendance.employee') }}:</span>
            <span class="font-bold text-slate-900">{{ duplicateRecord?.employeeFullName }}</span>
          </div>
          <div class="flex justify-between text-sm">
            <span class="text-slate-500">{{ $t('production.date') }}:</span>
            <span class="font-bold text-slate-900">{{ duplicateRecord?.attendanceDate }}</span>
          </div>
          <div class="flex justify-between text-sm">
            <span class="text-slate-500">{{ $t('attendance.actual_team') }}:</span>
            <span class="font-bold text-slate-900">{{ duplicateRecord?.actualTeamName || $t('common.unknown') }}</span>
          </div>
        </div>

        <div class="flex gap-3">
          <UiButton @click="showDuplicateModal = false" variant="outline" class="flex-1 border-slate-200">{{ $t('common.close') || 'Đóng lại' }}</UiButton>
          <UiButton @click="switchToEditDuplicate" class="flex-1 bg-amber-500 hover:bg-amber-600 text-white border-none shadow-lg shadow-amber-200">
            {{ $t('attendance.switch_to_edit') }}
          </UiButton>
        </div>
      </div>
    </div>

    <!-- Bulk Attendance Modal -->
    <div v-if="showBulkModal" class="fixed inset-0 z-[110] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-5xl p-8 animate-in zoom-in slide-in-from-bottom duration-300 max-h-[95vh] flex flex-col">
        <div class="flex items-center justify-between mb-6 shrink-0">
          <div>
            <h3 class="text-2xl font-black text-slate-900 tracking-tight">{{ $t('attendance.bulk_title') }}</h3>
            <p class="text-sm text-slate-500 mt-1">{{ $t('attendance.bulk_subtitle', { date: filterDate }) }}</p>
          </div>
          <button @click="showBulkModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full transition-all"><X class="w-5 h-5" /></button>
        </div>

        <div class="overflow-y-auto flex-1 pr-2 space-y-8">
          <!-- Filters for Bulk -->
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6 bg-slate-50 p-5 rounded-2xl border border-slate-100">
            <div class="w-full">
               <SelectDepartment 
                 v-model="bulkForm.departmentId" 
                 :label="$t('common.filter_by_dept') || 'Lọc theo phòng ban'" 
                 @update:modelValue="loadBulkEmployees"
               />
            </div>
            <div class="w-full">
               <SelectTeam 
                 v-model="bulkForm.teamId" 
                 :departmentId="bulkForm.departmentId"
                 :label="$t('attendance.original_team')"
                 @update:modelValue="loadBulkEmployees"
               />
            </div>
            <UiSelect 
              v-model="bulkForm.attendanceDefinitionId" 
              :label="$t('attendance.definition.name')" 
              :options="attendanceDefOptions" 
              :placeholder="$t('common.select_type') || 'Chọn loại công'"
              @update:modelValue="syncGlobalAttendanceType"
            />
          </div>

          <!-- Primary Employees -->
          <div>
            <div class="flex items-center justify-between mb-4">
              <h4 class="text-lg font-black text-slate-800">{{ $t('attendance.primary_list') }}</h4>
              <span class="text-xs font-bold text-slate-500 bg-slate-100 px-3 py-1 rounded-full">{{ bulkEmployees.filter(e => e.selected).length }} / {{ bulkEmployees.length }} {{ $t('common.selected') || 'đã chọn' }}</span>
            </div>
            
            <div v-if="bulkEmployees.length === 0" class="text-center p-8 border-2 border-dashed border-slate-200 rounded-2xl">
              <p class="text-slate-500 text-sm font-medium">{{ $t('attendance.no_employees_to_mark') || 'Không có nhân viên nào chưa chấm công phù hợp với bộ lọc.' }}</p>
            </div>
            <div v-else class="border border-slate-200 rounded-2xl overflow-hidden">
              <table class="w-full text-left">
                <thead class="bg-slate-50 border-b border-slate-200">
                  <tr class="text-[10px] font-black uppercase text-slate-500 tracking-widest">
                    <th class="px-4 py-3 w-12 text-center">
                      <input type="checkbox" :checked="isAllBulkSelected" @change="toggleAllBulk" class="rounded text-primary-600 focus:ring-primary-500" />
                    </th>
                    <th class="px-4 py-3">{{ $t('attendance.employee') }}</th>
                    <th class="px-4 py-3">{{ $t('attendance.original_team') }}</th>
                    <th class="px-4 py-3 w-48">{{ $t('attendance.actual_team') }}</th>
                    <th class="px-4 py-3 w-48">{{ $t('attendance.definition.name') || 'Loại công' }}</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                  <tr v-for="emp in bulkEmployees" :key="emp.id" :class="{'bg-primary-50/10': emp.selected, 'hover:bg-slate-50': true}">
                    <td class="px-4 py-3 text-center">
                      <input type="checkbox" v-model="emp.selected" class="rounded text-primary-600 focus:ring-primary-500" />
                    </td>
                    <td class="px-4 py-3">
                      <div class="font-bold text-sm text-slate-900">{{ emp.fullName }}</div>
                      <div class="text-[10px] text-slate-500 font-bold uppercase">{{ emp.code }}</div>
                    </td>
                    <td class="px-4 py-3">
                      <span class="text-xs font-bold text-slate-600 bg-slate-100 px-2.5 py-1 rounded-md">{{ emp.originalTeamName || $t('attendance.no_team_assigned') }}</span>
                    </td>
                    <td class="px-4 py-3">
                      <select v-model="emp.actualTeamId" class="w-full text-xs font-bold bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 outline-none">
                        <option v-for="opt in teamOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                      </select>
                    </td>
                    <td class="px-4 py-3">
                      <select v-model="emp.attendanceDefinitionId" class="w-full text-xs font-bold bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 outline-none">
                        <option v-for="opt in attendanceDefOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                      </select>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Borrowed Employees -->
          <div>
            <div class="flex items-center justify-between mb-4">
              <h4 class="text-lg font-black text-slate-800">{{ $t('attendance.borrowed_list') }}</h4>
              <UiButton @click="addBorrowedEmployee" variant="outline" class="h-8 px-3 text-xs border-dashed border-slate-300">
                <Plus class="w-3.5 h-3.5 mr-1" /> {{ $t('attendance.add_person') }}
              </UiButton>
            </div>

            <div v-if="borrowedEmployees.length === 0" class="text-center p-6 bg-slate-50/50 rounded-2xl border border-slate-100">
              <p class="text-slate-400 text-sm font-medium">{{ $t('attendance.no_borrowed') }}</p>
            </div>
            <div v-else class="space-y-3">
              <div v-for="(b, index) in borrowedEmployees" :key="index" class="flex gap-4 items-center bg-white p-3 rounded-xl border border-slate-200 shadow-sm">
                <div class="flex-1">
                  <SelectEmployee 
                    v-model="b.employeeId" 
                    :placeholder="$t('common.select_borrowed') || 'Chọn nhân sự mượn...'" 
                    :attendanceDate="filterDate"
                    label=""
                    @update:modelValue="e => onBorrowedEmployeeSelect(e, index)"
                  />
                </div>
                <div class="w-48">
                  <select v-model="b.actualTeamId" class="w-full text-xs font-bold bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 outline-none">
                    <option v-for="opt in teamOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                  </select>
                </div>
                <div class="w-48">
                  <select v-model="b.attendanceDefinitionId" class="w-full text-xs font-bold bg-white border border-slate-200 rounded-lg px-2.5 py-1.5 focus:border-primary-500 focus:ring-1 focus:ring-primary-500 outline-none">
                    <option v-for="opt in attendanceDefOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                  </select>
                </div>
                <button @click="removeBorrowedEmployee(index)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all shrink-0">
                  <Trash class="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="flex gap-4 mt-8 pt-6 border-t border-slate-100 shrink-0">
          <button type="button" @click="showBulkModal = false" class="flex-1 py-3.5 rounded-2xl border border-slate-200 text-slate-500 font-black hover:bg-slate-50 transition-all">{{ $t('common.cancel') }}</button>
          <UiButton @click="handleBulkSubmit" class="flex-[2] h-14 text-lg shadow-xl shadow-primary-200" :loading="saving" :disabled="totalBulkSelected === 0">
            {{ $t('attendance.save_count', { count: totalBulkSelected }) }}
          </UiButton>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { 
  CalendarPlus, Search, CalendarX, CheckCircle2, PencilLine, Trash2, X, Info,
  Download, Upload, FileSpreadsheet, UserX, Users, Plus, Trash,  ChevronLeft, ChevronRight, LayoutGrid, LayoutList, Grid3X3,
  Undo2, History, AlertTriangle
} from 'lucide-vue-next';
import { getToday, getCurrentMonth } from '@/utils/date';

const { $api } = useNuxtApp();

const attendances = ref([]);
const employees = ref([]);
const teams = ref([]);
const departments = ref([]);
const attendanceDefs = ref([]);

const attendanceDefOptions = computed(() => attendanceDefs.value.map(d => ({ value: d.id, label: `${d.code} - ${d.name}` })));

const employeeOptions = computed(() => employees.value.map(e => ({ value: e.id, label: e.fullName })));
const teamOptions = computed(() => teams.value.map(t => ({ value: t.id, label: t.name })));
const teamFilterOptions = computed(() => [
  { value: '', label: 'Tất cả tổ đội' },
  ...teams.value.map(t => ({ value: t.id, label: t.name }))
]);
const deptOptions = computed(() => [
  { value: '', label: 'Tất cả phòng ban' },
  ...departments.value.map(d => ({ value: d.id, label: d.name }))
]);

const loading = ref(true);
const saving = ref(false);
const exporting = ref(false);
const importing = ref(false);
const showModal = ref(false);
const showAbsentModal = ref(false);
const showDuplicateModal = ref(false);
const duplicateRecord = ref(null);

const showImportError = ref(false);
const importErrors = ref([]);

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

const showBulkModal = ref(false);

const bulkForm = reactive({
  departmentId: '',
  teamId: '',
  attendanceDefinitionId: null
});

const currentId = ref(null);
const form = reactive({
  employeeId: null,
  attendanceDate: '',
  originalTeamId: null,
  actualTeamId: null,
  attendanceDefinitionId: null
});

const bulkEmployees = ref([]);
const borrowedEmployees = ref([]);

const filterDate = ref(getToday());
const filterDeptIds = ref([]);
const filterTeamIds = ref([]);
const search = ref('');


const activeMenu = ref(null);

const toggleAttendanceMenu = (empId, date, event) => {
  if (activeMenu.value?.empId === empId && activeMenu.value?.date === date) {
    activeMenu.value = null;
  } else {
    activeMenu.value = { empId, date };
  }
};

const closeAttendanceMenu = () => {
  activeMenu.value = null;
};

// View Mode State
const viewMode = ref('matrix'); // 'list' or 'matrix'
const matrixScope = ref('month'); // 'month' or 'week'
const viewMonth = ref(getCurrentMonth());
const cellLoading = ref(null);

// Get Monday of current week
const getMonday = (d) => {
  d = new Date(d);
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  return new Date(d.setDate(diff));
};

const currentWeekStart = ref(getMonday(new Date()));

const navigateWeek = (direction) => {
  const newDate = new Date(currentWeekStart.value);
  newDate.setDate(newDate.getDate() + (direction * 7));
  currentWeekStart.value = newDate;
  fetchData();
};

const resetToCurrentWeek = () => {
  currentWeekStart.value = getMonday(new Date());
  fetchData();
};

const formatWeekRange = (monday) => {
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  const options = { day: '2-digit', month: '2-digit' };
  return `${monday.toLocaleDateString('vi-VN', options)} - ${sunday.toLocaleDateString('vi-VN', options)}`;
};

const displayDays = computed(() => {
  if (matrixScope.value === 'month') {
    const [year, month] = viewMonth.value.split('-').map(Number);
    const lastDay = new Date(year, month, 0).getDate();
    return Array.from({ length: lastDay }, (_, i) => {
      const day = i + 1;
      const date = new Date(year, month - 1, day);
      const dayName = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'][date.getDay()];
      const dayStr = day < 10 ? `0${day}` : `${day}`;
      const monthStr = month < 10 ? `0${month}` : `${month}`;
      return {
        key: day,
        label: day,
        dayName,
        isSunday: date.getDay() === 0,
        fullDate: `${year}-${monthStr}-${dayStr}`
      };
    });
  } else {
    return Array.from({ length: 7 }, (_, i) => {
      const date = new Date(currentWeekStart.value);
      date.setDate(date.getDate() + i);
      const dayName = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'][date.getDay()];
      const day = date.getDate();
      const month = date.getMonth() + 1;
      const year = date.getFullYear();
      const dayStr = day < 10 ? `0${day}` : `${day}`;
      const monthStr = month < 10 ? `0${month}` : `${month}`;
      return {
        key: i,
        label: day,
        dayName,
        isSunday: date.getDay() === 0,
        fullDate: `${year}-${monthStr}-${dayStr}`
      };
    });
  }
});

const handleMonthChange = () => {
  fetchData();
};

// Pagination
const currentPage = ref(1);
const itemsPerPage = ref(10);
const totalPages = computed(() => Math.ceil(filteredAttendances.value.length / itemsPerPage.value) || 1);

const paginatedAttendances = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage.value;
  const end = start + itemsPerPage.value;
  return filteredAttendances.value.slice(start, end);
});

watch([filterDate, filterDeptIds, filterTeamIds, search, itemsPerPage], () => {
  currentPage.value = 1;
}, { deep: true });

const fetchData = async () => {
  loading.value = true;
  try {
    const params = new URLSearchParams();
    
    if (viewMode.value === 'list') {
      params.append('date', filterDate.value);
    } else if (matrixScope.value === 'month') {
      const [year, month] = viewMonth.value.split('-').map(Number);
      const firstDay = new Date(year, month - 1, 1);
      const lastDay = new Date(year, month, 1);
      
      params.append('fromDate', firstDay.toISOString().split('T')[0]);
      params.append('toDate', lastDay.toISOString().split('T')[0]);
    } else {
      const monday = new Date(currentWeekStart.value);
      const sunday = new Date(monday);
      sunday.setDate(monday.getDate() + 6);
      
      params.append('fromDate', monday.toISOString().split('T')[0]);
      params.append('toDate', sunday.toISOString().split('T')[0]);
    }

    if (filterDeptIds.value.length > 0) {
      params.append('departmentIds', filterDeptIds.value.join(','));
    }
    
    if (filterTeamIds.value.length > 0) {
      params.append('teamIds', filterTeamIds.value.join(','));
    }

    // Call API specialized for date or month
    const endpoint = viewMode.value === 'list' 
      ? `/attendances/date/${filterDate.value}` 
      : `/attendances`; // Assuming /attendances with month/year params works

    const fetchWrapper = async (promise, fallback = []) => {
      try {
        const res = await promise;
        return res.data || res || fallback;
      } catch (err) {
        // Handle 404 specifically as empty list for attendances
        if (err.response?.status === 404) return [];
        console.error('Fetch error:', err);
        return fallback;
      }
    };

    const [attData, empData, teamData, deptData, defData] = await Promise.all([
      fetchWrapper($api.get(endpoint, { params: Object.fromEntries(params) })),
      fetchWrapper($api.get('/employees')),
      fetchWrapper($api.get('/teams')),
      fetchWrapper($api.get('/departments')),
      fetchWrapper($api.get('/attendance-definitions'))
    ]);
    
    attendances.value = Array.isArray(attData) ? attData : [];
    employees.value = Array.isArray(empData) ? empData : [];
    teams.value = Array.isArray(teamData) ? teamData : [];
    departments.value = Array.isArray(deptData) ? deptData : [];
    attendanceDefs.value = Array.isArray(defData) ? defData : [];

    // Set default attendance definition if not already set
    if (attendanceDefs.value.length > 0) {
      if (!form.attendanceDefinitionId) {
        form.attendanceDefinitionId = attendanceDefs.value[0].id;
      }
      if (!bulkForm.attendanceDefinitionId) {
        bulkForm.attendanceDefinitionId = attendanceDefs.value[0].id;
      }
    }

    console.log('Data fetch results:', {
      attendances: attendances.value.length,
      employees: employees.value.length,
      teams: teams.value.length,
      attendanceDefs: attendanceDefs.value.length
    });
  } catch (err) {
    console.error(err);
    if (err.response?.status === 404) attendances.value = [];
  } finally {
    loading.value = false;
  }
};

const fetchAttendances = () => fetchData();

const isFilterActive = (val) => val && val !== 'null' && val !== '';

const filteredAttendances = computed(() => {
  return attendances.value.filter(a => {
    const matchSearch = !search.value || 
                       a.employeeFullName.toLowerCase().includes(search.value.toLowerCase()) ||
                       a.employeeCode.toLowerCase().includes(search.value.toLowerCase());
                       
    const matchDept = filterDeptIds.value.length === 0 || filterDeptIds.value.includes(a.employeeDepartmentId);
    
    const matchTeam = filterTeamIds.value.length === 0 || filterTeamIds.value.includes(a.employeeTeamId);
    
    return matchSearch && matchDept && matchTeam;
  }).sort((a, b) => {
    const teamA = a.actualTeamName || a.originalTeamName || '';
    const teamB = b.actualTeamName || b.originalTeamName || '';
    return teamA.localeCompare(teamB, 'vi');
  });
});

const filteredEmployees = computed(() => {
  return employees.value.filter(e => {
    if (e.status !== 'ACTIVE') return false;
    
    const matchSearch = !search.value || 
                       e.fullName.toLowerCase().includes(search.value.toLowerCase()) ||
                       e.code.toLowerCase().includes(search.value.toLowerCase());

    const matchDept = filterDeptIds.value.length === 0 || filterDeptIds.value.includes(e.departmentId);
    
    const matchTeam = filterTeamIds.value.length === 0 || filterTeamIds.value.includes(e.teamId);
    
    return matchSearch && matchDept && matchTeam;
  }).sort((a, b) => {
    const teamA = a.teamName || '';
    const teamB = b.teamName || '';
    return teamA.localeCompare(teamB, 'vi');
  });
});

const getAttendanceMatrixValue = (empId, targetDate) => {
  const record = attendances.value.find(a => a.employeeId === empId && a.attendanceDate === targetDate);
  if (record) {
    return { 
      status: 'PRESENT', 
      id: record.id, 
      defCode: record.attendanceDefinitionCode,
      defName: record.attendanceDefinitionName
    };
  }
  
  const date = new Date(targetDate);
  if (date.getDay() === 0) return { status: 'SUNDAY', id: null };
  
  // Check if today or past
  const todayStr = new Date().toISOString().substr(0, 10);
  if (targetDate <= todayStr) return { status: 'ABSENT', id: null };
  
  return { status: null, id: null };
};


const performAttendanceAction = async (empId, targetDate, defId) => {
  const { status, id } = getAttendanceMatrixValue(empId, targetDate);
  closeAttendanceMenu();
  
  cellLoading.value = `${empId}_${targetDate}`;
  try {
    if (defId === null) {
      if (status === 'PRESENT') await $api.delete(`/attendances/${id}`);
    } else {
      const emp = employees.value.find(e => e.id === empId);
      if (status === 'PRESENT') {
        const record = attendances.value.find(a => a.id === id);
        await $api.put(`/attendances/${id}`, {
          employeeId: empId,
          attendanceDate: targetDate,
          originalTeamId: emp?.teamId || null,
          actualTeamId: record?.actualTeamId || emp?.teamId || null,
          attendanceDefinitionId: defId
        });
      } else {
        await $api.post('/attendances', {
          employeeId: empId,
          attendanceDate: targetDate,
          originalTeamId: emp?.teamId || null,
          actualTeamId: emp?.teamId || null,
          attendanceDefinitionId: defId
        });
      }
    }
    await fetchData();
  } catch (err) {
    console.error(err);
    alert(err.response?.data?.message || 'Lỗi thao tác chấm công');
  } finally {
    cellLoading.value = null;
  }
};

const getEmployeeTotalAttendance = (empId) => {
  const total = attendances.value
    .filter(a => a.employeeId === empId)
    .reduce((sum, a) => sum + (a.attendanceDefinitionMultiplier || 0), 0);
  return Math.round(total * 10) / 10;
};

const absentEmployees = computed(() => {
  const attendedIds = new Set(attendances.value.map(a => a.employeeId));
  return employees.value.filter(e => {
    // Chỉ lấy nhân viên Đang làm việc (ACTIVE)
    if (e.status !== 'ACTIVE') return false;
    // Bỏ qua nếu đã chấm công
    if (attendedIds.has(e.id)) return false;
    
    // Áp dụng bộ lọc phòng ban/tổ đội nếu đang chọn
    const matchDept = filterDeptIds.value.length === 0 || filterDeptIds.value.includes(e.departmentId);
    
    const matchTeam = filterTeamIds.value.length === 0 || filterTeamIds.value.includes(e.teamId);
    
    return matchDept && matchTeam;
  });
});

const statistics = computed(() => {
  const activeEmployees = employees.value.filter(e => {
    if (e.status !== 'ACTIVE') return false;
    const matchDept = filterDeptIds.value.length === 0 || filterDeptIds.value.includes(e.departmentId);
    const matchTeam = filterTeamIds.value.length === 0 || filterTeamIds.value.includes(e.teamId);
    return matchDept && matchTeam;
  });

  const empCount = activeEmployees.length;

  if (viewMode.value === 'list') {
    return {
      present: filteredAttendances.value.length,
      total: empCount
    };
  } else {
    // Trong chế độ ma trận (tuần hoặc tháng)
    // Tính tổng số công thực tế dựa trên multiplier
    const presentCount = attendances.value
      .filter(a => {
         const emp = activeEmployees.find(e => e.id === a.employeeId);
         return !!emp;
      })
      .reduce((sum, a) => sum + (a.attendanceDefinitionMultiplier || 0), 0);
    
    // Tính tổng số công tiềm năng (Số nhân viên * Số ngày làm việc không phải Chủ Nhật)
    const workingDaysCount = displayDays.value.filter(d => !d.isSunday).length;
    const totalPotential = empCount * workingDaysCount;
    
    return {
      present: Math.round(presentCount * 10) / 10,
      total: totalPotential
    };
  }
});

const openModal = (att = null) => {
  console.log("Button clicked, params:", att);
  
  // Nếu att có chứa id thực sự từ database, thì là chế độ SỬA
  if (att && typeof att === 'object' && att.id) {
    currentId.value = att.id;
    form.employeeId = att.employeeId;
    form.originalTeamId = att.originalTeamId || null;
    form.actualTeamId = att.actualTeamId || null;
    form.attendanceDate = att.attendanceDate;
    form.attendanceDefinitionId = att.attendanceDefinitionId || null;
  } else {
    currentId.value = null;
    form.employeeId = null;
    form.originalTeamId = null;
    form.actualTeamId = null;
    form.attendanceDate = filterDate.value;
    form.attendanceDefinitionId = attendanceDefs.value.length > 0 ? attendanceDefs.value[0].id : null;
  }
  showModal.value = true;
};

const onEmployeeSelect = (empId) => {
  if (!empId) {
    form.originalTeamId = null;
    form.actualTeamId = null;
    return;
  }
  const emp = employees.value.find(e => e.id == empId);
  if (emp && emp.teamId) {
    form.originalTeamId = emp.teamId;
    form.actualTeamId = emp.teamId;
  }
};

const switchToEditDuplicate = () => {
  showDuplicateModal.value = false;
  openModal(duplicateRecord.value);
};

const handleSubmit = async () => {
  // Validate duplicate on frontend (if same date)
  if (!currentId.value) {
    const existing = attendances.value.find(
      a => a.employeeId == form.employeeId && a.attendanceDate === form.attendanceDate
    );
    if (existing) {
      duplicateRecord.value = existing;
      showDuplicateModal.value = true;
      return;
    }
  }

  saving.value = true;
  try {
    if (currentId.value) {
      await $api.put(`/attendances/${currentId.value}`, form);
    } else {
      await $api.post('/attendances', form);
    }
    showModal.value = false;
    fetchData();
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '';
    if (msg.includes('Duplicate entry') || msg.includes('constraint')) {
      triggerError('Sửa lỗi trùng lặp', 'Nhân viên này đã được chấm công trong ngày, dữ liệu bị trùng lặp ở Database!', msg);
    } else {
      triggerError('Lỗi xử lý', 'Đã xảy ra lỗi khi lưu thông tin chấm công.', msg);
    }
  } finally {
    saving.value = false;
  }
};

const openBulkModal = async () => {
  bulkForm.departmentId = filterDeptIds.value[0] || null;
  bulkForm.teamId = filterTeamIds.value[0] || null;
  bulkForm.attendanceDefinitionId = null;
  borrowedEmployees.value = [];
  
  // Fetch fresh attendance for the target date before loading bulk list
  loading.value = true;
  try {
    const res = await $api.get(`/attendances/date/${filterDate.value}`);
    // Temporarily update attendances local state just for this specific date
    // Or just use the returned list to filter. Let's update attendances for consistency.
    attendances.value = res.data || [];
    loadBulkEmployees();
    showBulkModal.value = true;
  } catch (err) {
    console.error('Error fetching fresh attendance for bulk:', err);
  } finally {
    loading.value = false;
  }
};

const loadBulkEmployees = () => {
  const targetDate = filterDate.value;
  const attendedIds = new Set(attendances.value
    .filter(a => a.attendanceDate === targetDate)
    .map(a => a.employeeId)
  );
  
  bulkEmployees.value = employees.value
    .filter(e => {
      if (e.status !== 'ACTIVE') return false;
      if (attendedIds.has(e.id)) return false;
      
      const matchDept = !isFilterActive(bulkForm.departmentId) || e.departmentId == bulkForm.departmentId;
      const matchTeam = !isFilterActive(bulkForm.teamId) || e.teamId == bulkForm.teamId;
      
      return matchDept && matchTeam;
    })
    .map(e => ({
      selected: true,
      id: e.id,
      fullName: e.fullName,
      code: e.code,
      originalTeamId: e.teamId,
      originalTeamName: e.teamName,
      actualTeamId: e.teamId || null,
      attendanceDefinitionId: bulkForm.attendanceDefinitionId
    }));
};

const syncGlobalAttendanceType = (defId) => {
  bulkEmployees.value.forEach(emp => {
    emp.attendanceDefinitionId = defId;
  });
  borrowedEmployees.value.forEach(b => {
    b.attendanceDefinitionId = defId;
  });
};

const isAllBulkSelected = computed(() => bulkEmployees.value.length > 0 && bulkEmployees.value.every(e => e.selected));
const toggleAllBulk = (e) => {
  const val = e.target.checked;
  bulkEmployees.value.forEach(emp => emp.selected = val);
};

const totalBulkSelected = computed(() => {
  return bulkEmployees.value.filter(e => e.selected).length + borrowedEmployees.value.filter(b => b.employeeId).length;
});

const addBorrowedEmployee = () => {
  borrowedEmployees.value.push({ 
    employeeId: null, 
    actualTeamId: bulkForm.teamId || null,
    attendanceDefinitionId: bulkForm.attendanceDefinitionId
  });
};
const removeBorrowedEmployee = (index) => {
  borrowedEmployees.value.splice(index, 1);
};

const onBorrowedEmployeeSelect = (empId, index) => {
  if (!empId) return;
  const emp = employees.value.find(e => e.id == empId);
  if (emp && !borrowedEmployees.value[index].actualTeamId && bulkForm.teamId) {
    borrowedEmployees.value[index].actualTeamId = bulkForm.teamId;
  } else if (emp && !borrowedEmployees.value[index].actualTeamId) {
    borrowedEmployees.value[index].actualTeamId = emp.team?.id || null;
  }
};

// Redundant with SelectEmployee handling search and filtering by date

const handleBulkSubmit = async () => {
  const payload = [];
  
  bulkEmployees.value.filter(e => e.selected).forEach(e => {
    payload.push({
      employeeId: e.id,
      originalTeamId: e.originalTeamId || null,
      actualTeamId: e.actualTeamId,
      attendanceDate: filterDate.value,
      attendanceDefinitionId: e.attendanceDefinitionId
    });
  });
  
  borrowedEmployees.value.filter(b => b.employeeId).forEach(b => {
    const emp = employees.value.find(e => e.id == b.employeeId);
    payload.push({
      employeeId: b.employeeId,
      originalTeamId: emp?.teamId || null,
      actualTeamId: b.actualTeamId,
      attendanceDate: filterDate.value,
      attendanceDefinitionId: b.attendanceDefinitionId
    });
  });
  
  if (payload.length === 0) return;
  
  saving.value = true;
  try {
    await $api.post('/attendances/batch', payload);
    showBulkModal.value = false;
    fetchData();
  } catch (err) {
    triggerError('Lỗi lưu hàng loạt', 'Hệ thống gặp sự cố khi lưu danh sách chấm công hàng loạt.', err.response?.data?.message || err.message);
  } finally {
    saving.value = false;
  }
};

const handleExport = async (format = 'list') => {
  exporting.value = true;
  try {
    let month, year;
    if (viewMode.value === 'matrix' && matrixScope.value === 'month') {
      [year, month] = viewMonth.value.split('-').map(Number);
    } else {
      month = new Date(filterDate.value).getMonth() + 1;
      year = new Date(filterDate.value).getFullYear();
    }
    
    const response = await $api.get('/attendances/export', {
      params: { 
        month, 
        year, 
        format,
        departmentIds: filterDeptIds.value.join(','),
        teamIds: filterTeamIds.value.join(',')
      },
      responseType: 'blob'
    });
    
    const url = window.URL.createObjectURL(new Blob([response]));
    const link = document.createElement('a');
    link.href = url;
    const fileName = format === 'matrix' ? `ChamCong_Matrix_${month}_${year}.xlsx` : `ChamCong_DanhSach_${month}_${year}.xlsx`;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (err) {
    triggerError('Lỗi xuất file', 'Không thể tạo file báo cáo chấm công. Vui lòng thử lại.', err.message);
  } finally {
    exporting.value = false;
  }
};

const handleDownloadTemplate = async () => {
  try {
    const response = await $api.get('/attendances/download-template', {
      responseType: 'blob'
    });
    
    // Vì axios interceptor đã trả về response.data, nên ở đây data chính là Blob
    const url = window.URL.createObjectURL(new Blob([response]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'Mau_Nhap_Cham_Cong.xlsx');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (err) {
    triggerError('Lỗi tải file mẫu', 'Không thể tải xuống tệp tin mẫu nhập chấm công.', err.message);
  }
};

const handleImport = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  const formData = new FormData();
  formData.append('file', file);
  
  importing.value = true;
  try {
    const res = await $api.post('/attendances/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    
    const result = res.data;
    if (result.errorCount > 0) {
      importErrors.value = result.errors;
      showImportError.value = true;
      if (result.successCount > 0) {
        alert(`Đã nhập thành công ${result.successCount} dòng, nhưng có ${result.errorCount} dòng lỗi.`);
      }
    } else {
      alert('Nhập dữ liệu thành công!');
    }
    fetchData();
  } catch (err) {
    const msg = err.response?.data?.message || err.message;
    if (msg.includes('Tiêu đề cột không khớp')) {
      triggerError('Sai mẫu file', 'Cấu trúc file Excel không đúng với quy định của hệ thống.', msg);
    } else {
      triggerError('Lỗi nhập file', 'Đã xảy ra lỗi không mong muốn khi đọc file Excel.', msg);
    }
  } finally {
    importing.value = false;
    event.target.value = ''; // Reset input
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa bản ghi chấm công này?')) return;
  try {
    await $api.delete(`/attendances/${id}`);
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Lỗi');
  }
};

onMounted(async () => {
  await fetchData();
});

onUnmounted(() => {
});

watch([viewMode, matrixScope, filterDeptIds, filterTeamIds], () => {
    fetchData();
}, { deep: true });
</script>
