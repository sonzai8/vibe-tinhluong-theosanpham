<template>
  <div class="space-y-8">
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 class="text-3xl font-black text-slate-900 tracking-tight">{{ $t('production.title') }}</h2>
        <p class="text-slate-500 font-medium">{{ $t('production.subtitle') }}</p>
      </div>
      <div class="flex gap-3">
        <UiButton variant="outline" @click="handleExportList" :loading="exporting">
          <Download class="w-4 h-4" />
          {{ $t('production.export_list') }}
        </UiButton>
        <UiButton variant="outline" @click="handleExportMatrix" :loading="exporting">
          <FileSpreadsheet class="w-4 h-4" />
          {{ $t('production.export_matrix') }}
        </UiButton>
        <UiButton @click="showBulkModal = true" variant="outline" >
          <PlusCircle class="w-4 h-4" />
          {{ $t('production.bulk_add') }}
        </UiButton>
        <UiButton @click="() => openModal(null)" class="shadow-lg shadow-emerald-100">
          <Plus class="w-4 h-4" />
          {{ $t('production.add_new') }}
        </UiButton>
      </div>
    </div>

    <!-- Filter -->
    <div class="card overflow-visible border-none shadow-sm bg-white/80 backdrop-blur-md">
      <div class="p-6">
        <div class="flex flex-wrap items-end gap-6">
          <!-- Select Group -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 flex-1 min-w-[300px]">
            <div class="flex flex-col gap-1.5 min-w-[200px]">
              <SelectDepartment 
                v-model="filter.departmentIds" 
                multiple
                :label="$t('common.department')" 
              />
            </div>

            <div class="flex flex-col gap-1.5 min-w-[200px]">
              <SelectTeam 
                v-model="filter.teamIds" 
                multiple
                :departmentId="filter.departmentIds.length === 1 ? filter.departmentIds[0] : null"
                :label="$t('common.team')" 
              />
            </div>
          </div>

          <!-- Time Group -->
          <div class="flex flex-wrap items-end gap-4 shrink-0">
            <template v-if="viewMode === 'list'">
              <div class="flex flex-col gap-1.5 w-[160px]">
                <label class="text-[10px] font-black text-slate-400 border-l-2 border-primary-500 pl-2 uppercase tracking-widest">{{ $t('common.from_date') }}</label>
                <input v-model="filter.from" type="date" class="input-field py-3 bg-white border-slate-200 focus:border-primary-500 transition-all font-bold" />
              </div>
              <div class="flex flex-col gap-1.5 w-[160px]">
                <label class="text-[10px] font-black text-slate-400 border-l-2 border-primary-500 pl-2 uppercase tracking-widest">{{ $t('common.to_date') }}</label>
                <input v-model="filter.to" type="date" class="input-field py-3 bg-white border-slate-200 focus:border-primary-500 transition-all font-bold" />
              </div>
            </template>
            <template v-else>
              <div class="flex flex-col gap-1.5 w-[180px]">
                <label class="text-[10px] font-black text-slate-400 border-l-2 border-primary-500 pl-2 uppercase tracking-widest">{{ $t('common.reporting_month') }}</label>
                <input v-model="viewMonth" type="month" class="input-field py-3 bg-white border-slate-200 focus:border-primary-500 transition-all font-bold" @change="handleMonthChange" />
              </div>
              <div class="flex flex-col gap-1.5 w-[180px]">
                <label class="text-[10px] font-black text-slate-400 border-l-2 border-primary-500 pl-2 uppercase tracking-widest">{{ $t('common.view_by_week') }}</label>
                <select v-model="viewWeek" class="input-field py-3 bg-white border-slate-200 focus:border-primary-500 transition-all text-sm font-bold appearance-none">
                  <option :value="null">{{ $t('common.full_month') }}</option>
                  <option v-for="w in monthWeeks" :key="w.id" :value="w.id">{{ w.label }}</option>
                </select>
              </div>
            </template>

            <UiButton @click="fetchRecords" variant="secondary" class="bg-primary-600 text-white hover:bg-primary-700 h-[46px] px-8 shadow-lg shadow-primary-200 ring-offset-2 focus:ring-2 ring-primary-500 transition-all">
              <Filter class="w-4 h-4 mr-2" />
              {{ $t('common.filter') }}
            </UiButton>
          </div>
        </div>
      </div>
    </div>

    <!-- View Switcher & Stats -->
    <div class="flex flex-col sm:flex-row items-center justify-between gap-4 py-2">
      <div class="flex items-center gap-6 px-4">
        <div v-if="viewMode === 'matrix'" class="flex items-center gap-3">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('production.displaying') }}:</span>
          <div class="flex items-center gap-2 bg-white px-3 py-1.5 rounded-full border border-slate-100 shadow-sm">
            <span class="text-sm font-black text-primary-600">{{ filteredTeams.length }}</span>
            <span class="text-[10px] font-bold text-slate-500 uppercase">{{ $t('production.teams_count') }}</span>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ $t('production.total_records') }}:</span>
          <div class="flex items-center gap-2 bg-white px-3 py-1.5 rounded-full border border-slate-100 shadow-sm">
            <span class="text-sm font-black text-primary-600">{{ records.length }}</span>
          </div>
        </div>
      </div>
      
      <div class="flex bg-slate-200/50 p-1.5 rounded-2xl shrink-0 backdrop-blur-sm shadow-inner overflow-hidden border border-slate-100">
        <button 
          @click="viewMode = 'list'" 
          :class="['px-6 py-2.5 text-[11px] font-black uppercase tracking-widest rounded-xl transition-all flex items-center gap-2.5', viewMode === 'list' ? 'bg-white text-primary-700 shadow-xl shadow-slate-300 scale-[1.02]' : 'text-slate-500 hover:text-slate-700 hover:bg-white/50']"
        >
          <LayoutList class="w-3 h-3" />
          {{ $t('common.list_view') }}
        </button>
        <button 
          @click="viewMode = 'matrix'" 
          :class="['px-6 py-2.5 text-[11px] font-black uppercase tracking-widest rounded-xl transition-all flex items-center gap-2.5', viewMode === 'matrix' ? 'bg-white text-primary-700 shadow-xl shadow-slate-300 scale-[1.02]' : 'text-slate-500 hover:text-slate-700 hover:bg-white/50']"
        >
          <Grid3X3 class="w-3 h-3" />
          {{ $t('common.matrix_view') }}
        </button>
      </div>

      
    </div>

    <!-- Table / Matrix View -->
    <div class="card overflow-hidden">
      <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
        <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold">{{ $t('production.loading') }}</p>
      </div>

      <div v-else-if="records.length === 0 && viewMode === 'list'" class="p-20 text-center space-y-4">
        <div class="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-300">
          <History class="w-10 h-10" />
        </div>
        <p class="text-slate-500 font-bold">{{ $t('production.no_data') }}</p>
      </div>

      <!-- List View -->
      <div v-else-if="viewMode === 'list'" class="overflow-x-auto">
        <table class="w-full text-left">
          <thead>
            <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
              <th class="px-8 py-5">{{ $t('production.date') }}</th>
              <th class="px-8 py-5">{{ $t('common.team') }}</th>
              <th class="px-8 py-5">{{ $t('production.product') }}</th>
              <th class="px-8 py-5">{{ $t('production.quality') }}</th>
              <th class="px-8 py-5">{{ $t('production.quantity') }}</th>
              <th class="px-8 py-5 text-right">{{ $t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="r in records" :key="r.id" class="hover:bg-slate-50/50 transition-all group">
              <td class="px-8 py-5 text-sm font-bold text-slate-500">{{ r.productionDate }}</td>
              <td class="px-8 py-5 font-black text-slate-900">
                <div class="flex flex-col">
                  <span>{{ r.team?.name }}</span>
                  <span class="text-[10px] text-slate-400 font-black uppercase tracking-tighter">{{ r.team?.productionStep?.name }}</span>
                </div>
              </td>
              <td class="px-8 py-5">
                <div class="flex flex-col">
                  <span class="font-bold text-primary-700">{{ r.product?.code }}</span>
                  <span class="text-[10px] text-slate-400 font-medium">
                    {{ r.product?.thickness }}mm x {{ r.product?.length }}m x {{ r.product?.width }}m
                  </span>
                </div>
              </td>
              <td class="px-8 py-5">
                <span class="px-2.5 py-1 bg-primary-100 rounded-lg text-[10px] font-black text-primary-700 uppercase tracking-widest">
                  {{ r.quality?.code }}
                </span>
              </td>
              <td class="px-8 py-5 font-black text-slate-900 text-lg">{{ r.quantity?.toLocaleString() }}</td>
              <td class="px-8 py-5 text-right">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button v-if="!isPastMonth" @click="openModal(r)" class="p-2 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all"><PencilLine class="w-4 h-4" /></button>
                  <button v-if="!isPastMonth" @click="handleDelete(r.id)" class="p-2 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"><Trash2 class="w-4 h-4" /></button>
                  <span v-else class="text-[10px] font-black text-slate-300 uppercase italic">{{ $t('common.locked') || 'Khoá' }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Matrix View -->
      <div v-else class="overflow-x-auto relative min-h-[500px] border border-slate-200 rounded-xl shadow-sm bg-white">
        <div v-if="isPastMonth" class="bg-amber-50 p-2 text-center border-b border-amber-100">
          <p class="text-[10px] font-bold text-amber-700">{{ $t('production.locked_month_msg', { month: viewMonth }) }}</p>
        </div>
        <table 
          class="w-full text-left border-collapse"
          :class="[viewWeek ? 'table-auto' : 'table-fixed min-w-[2000px]']"
        >
          <thead>
            <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-200 sticky top-0 z-30">
              <th class="px-4 py-4 w-40 min-w-[160px] sticky left-0 bg-slate-50 z-40 shadow-[2px_0_5px_rgba(0,0,0,0.05)] border-r border-slate-200">{{ $t('common.team') }}</th>
              <th v-for="day in displayedDays" :key="day" 
                  class="px-2 py-3 text-center border-r border-slate-200 last:border-r-0"
                  :class="[viewWeek ? '' : 'w-24', getDayHighlightClass(day, 'header'), isSunday(day) ? 'bg-blue-50/50' : '']"
              >
                <div class="flex flex-col gap-0.5">
                  <span class="text-slate-400 font-bold opacity-60">{{ getDayOfWeek(day) }}</span>
                  <span class="text-[11px]">{{ day }}</span>
                </div>
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-for="team in paginatedTeams" :key="team.id" class="hover:bg-slate-50/50 group">
              <td class="px-4 py-4 font-black text-slate-900 text-xs sticky left-0 bg-white group-hover:bg-slate-50 z-20 shadow-[2px_0_5px_rgba(0,0,0,0.05)] border-r border-slate-200">
                <div class="flex flex-col gap-1.5">
                  <span class="whitespace-nowrap">{{ team.name }}</span>
                  <div class="mt-1 pt-1 border-t border-slate-100">
                    <div class="text-[8px] text-slate-400 font-bold uppercase">Tổng:</div>
                    <div class="text-xs font-black text-primary-600">{{ getTeamTotalQuantity(team.id).toLocaleString() }}</div>
                  </div>
                </div>
              </td>
              <td v-for="day in displayedDays" :key="day" 
                  class="p-2 text-center border-r border-slate-200 last:border-r-0 cursor-pointer transition-all font-bold group/cell relative h-32 align-top"
                  :class="[getDayHighlightClass(day, 'cell'), isSunday(day) ? 'bg-blue-50/30' : '']"
                  @click="showCellDetails(team.id, day)"
                  @mouseenter="setHoveredCell(team.id, day, $event)"
                  @mouseleave="clearHoveredCell"
              >
                <div v-if="getMatrixCell(team.id, day).length > 0" class="flex flex-col h-full overflow-hidden">
                  <span class="text-xs font-black text-primary-700 mb-1">{{ getMatrixCellQuantity(team.id, day).toLocaleString() }}</span>
                  <div class="flex flex-col gap-0.5 items-center w-full grow overflow-y-auto no-scrollbar">
                    <div v-for="r in getMatrixCell(team.id, day)" :key="r.id" class="w-full">
                      <div class="text-[9px] bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded-md flex justify-between items-center whitespace-nowrap overflow-hidden">
                        <span class="font-black truncate mr-1">{{ r.product?.code }}</span>
                        <span class="text-[8px] font-bold text-slate-400 shrink-0">{{ r.quantity }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <span v-else class="text-slate-100">-</span>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Floating Tooltip -->
        <div 
          v-if="hoverTooltip.show" 
          class="fixed z-[200] w-72 bg-white rounded-2xl shadow-2xl border border-slate-100 pointer-events-none p-5 animate-in fade-in zoom-in duration-200"
          :style="{ top: hoverTooltip.y + 'px', left: hoverTooltip.x + 'px' }"
        >
          <div class="flex flex-col gap-4">
            <div class="flex justify-between items-start border-b border-slate-50 pb-3">
              <div>
                <h4 class="text-xs font-black text-slate-900 uppercase tracking-widest">{{ hoverTooltip.teamName }}</h4>
                <p class="text-[10px] text-slate-400 font-bold">{{ hoverTooltip.date }}</p>
              </div>
              <div class="bg-primary-50 px-2 py-1 rounded-lg text-primary-700 text-xs font-black">
                {{ hoverTooltip.totalQuantity?.toLocaleString() }}
              </div>
            </div>

            <div class="space-y-3">
              <h5 class="text-[9px] font-black text-slate-300 uppercase tracking-tighter">{{ $t('production.product_quality') }}</h5>
              <div class="space-y-1.5 rotate-0">
                <div v-for="r in hoverTooltip.records" :key="r.id" class="flex items-center justify-between text-xs transition-transform transform-gpu">
                  <div class="flex items-center gap-2">
                    <span class="font-black text-slate-700">{{ r.product?.code }}</span>
                    <span class="text-[8px] bg-amber-50 text-amber-600 px-1 rounded uppercase font-bold">{{ r.quality?.code }}</span>
                  </div>
                  <span class="font-bold text-slate-500">{{ r.quantity }}</span>
                </div>
              </div>
            </div>

            <div class="space-y-3 border-t border-slate-50 pt-3">
              <div class="flex justify-between items-center">
                <h5 class="text-[9px] font-black text-slate-300 uppercase tracking-tighter">{{ $t('production.participating_employees') }} ({{ hoverTooltip.employees.length }})</h5>
              </div>
              <div class="flex flex-wrap gap-1.5 max-h-40 overflow-y-auto pr-1 pb-1">
                <div v-for="emp in hoverTooltip.employees" :key="emp.id" 
                  :class="['text-[10px] px-2 py-1 rounded-full font-bold border truncate max-w-full flex items-center gap-1.5', 
                           emp.isBorrowed ? 'bg-red-50 text-red-700 border-red-100' : 'bg-slate-50 text-slate-600 border-slate-100']"
                  :title="emp.isBorrowed ? `Mượn từ: ${emp.fromTeam}` : 'Chính thức'"
                >
                  <ArrowRightLeft v-if="emp.isBorrowed" class="w-3 h-3 shrink-0" />
                  <User v-else class="w-3 h-3 shrink-0 opacity-50" />
                  {{ emp.name }}
                </div>
              </div>
              <p v-if="hoverTooltip.employees.length === 0" class="text-[10px] text-slate-400 italic">Chưa có dữ liệu chấm công</p>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div class="p-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between sticky bottom-0 z-20">
          <div class="text-xs font-bold text-slate-500 uppercase tracking-widest">
            {{ $t('production.displaying') }} {{ paginatedTeams.length }} / {{ teams.length }} {{ $t('common.team').toLowerCase() }}
          </div>
          <div class="flex items-center gap-2">
            <button 
              @click="currentPage--" 
              :disabled="currentPage === 1"
              class="p-2 rounded-lg bg-white border border-slate-200 text-slate-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 transition-colors"
            >
              <ChevronLeft class="w-4 h-4" />
            </button>
            <div class="flex items-center gap-1">
              <button 
                v-for="p in totalPages" 
                :key="p"
                @click="currentPage = p"
                :class="['w-8 h-8 rounded-lg flex items-center justify-center text-xs font-black transition-all', 
                         currentPage === p ? 'bg-primary-600 text-white shadow-lg shadow-primary-200' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50']"
              >
                {{ p }}
              </button>
            </div>
            <button 
              @click="currentPage++" 
              :disabled="currentPage === totalPages"
              class="p-2 rounded-lg bg-white border border-slate-200 text-slate-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-slate-50 transition-colors"
            >
              <ChevronRight class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-2xl p-10 animate-in zoom-in slide-in-from-bottom duration-300">
        <div class="flex items-center justify-between mb-10">
          <h3 class="text-2xl font-black text-slate-900">{{ currentId ? $t('common.update') : $t('production.add_new') }}</h3>
          <button @click="showModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full"><X class="w-5 h-5" /></button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
            <UiSelect 
              v-model="form.teamId" 
              :label="$t('common.team')" 
              :options="teamOptions" 
              :placeholder="$t('common.select_team') || 'Chọn tổ sản xuất'"
              required
            />
            
            <UiInput v-model="form.productionDate" :label="$t('production.date')" type="date" required />
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <UiSelect 
              v-model="form.productId" 
              :label="$t('production.product')" 
              :options="filteredProductOptions" 
              :placeholder="$t('common.select_product') || 'Chọn sản phẩm'"
              :disabled="!form.teamId"
              required
            />

            <UiSelect 
              v-model="form.qualityId" 
              :label="$t('production.quality')" 
              :options="qualityOptions" 
              :placeholder="$t('common.select_quality') || 'Chọn chất lượng'"
              :disabled="!form.teamId"
              required
            />

            <UiInput v-model="form.quantity" :label="$t('production.quantity')" type="number" min="1" required />
          </div>

          <div class="flex gap-4 pt-6">
            <button type="button" @click="showModal = false" class="flex-1 py-3.5 rounded-2xl border border-slate-200 text-slate-500 font-black hover:bg-slate-50 transition-all">{{ $t('common.cancel') }}</button>
            <UiButton type="submit" class="flex-[2] h-14" :loading="saving">{{ $t('common.save') }}</UiButton>
          </div>
        </form>
      </div>
    </div>

    <!-- Bulk Entry Modal -->
    <div v-if="showBulkModal" class="fixed inset-0 z-[110] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-6xl p-8 animate-in zoom-in slide-in-from-bottom duration-300 max-h-[95vh] flex flex-col pl-4 pr-6">
        <div class="flex items-center justify-between mb-6 shrink-0 pl-4">
          <div>
            <h3 class="text-2xl font-black text-slate-900 tracking-tight">{{ $t('production.bulk_title') }}</h3>
            <p class="text-sm text-slate-500 mt-1">{{ $t('production.bulk_subtitle') }}</p>
          </div>
          <button @click="showBulkModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full transition-all"><X class="w-5 h-5" /></button>
        </div>

        <div class="overflow-y-auto flex-1 pl-4 pr-2 space-y-6">
          <div class="flex items-center gap-4 bg-slate-50 p-4 rounded-xl border border-slate-100 w-full max-w-sm">
            <label class="text-xs font-black text-slate-500 uppercase tracking-widest shrink-0">{{ $t('production.date') }}</label>
            <input v-model="bulkDate" type="date" class="input-field py-2 text-sm font-bold flex-1" />
          </div>

          <!-- Error Message -->
          <div v-if="bulkError" class="p-4 bg-red-50 border border-red-100 rounded-2xl flex items-center gap-3 text-red-600 animate-in fade-in slide-in-from-top-2">
            <AlertCircle class="w-5 h-5 shrink-0" />
            <p class="text-sm font-bold">{{ bulkError }}</p>
          </div>


          <div class="border border-slate-200 rounded-2xl overflow-hidden shadow-sm">
            <table class="w-full text-left bg-white">
              <thead class="bg-slate-50 border-b border-slate-200">
                <tr class="text-[10px] font-black uppercase text-slate-500 tracking-widest divide-x divide-slate-100">
                  <th class="px-4 py-3 w-12 text-center">#</th>
                  <th class="px-4 py-3 w-[25%]">{{ $t('common.team') }}</th>
                  <th class="px-4 py-3">{{ $t('production.product') }}</th>
                  <th class="px-4 py-3 w-[20%]">{{ $t('production.quality') }}</th>
                  <th class="px-4 py-3 w-32">{{ $t('production.quantity') }}</th>
                  <th class="px-3 py-3 w-12 text-center"></th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr v-for="(row, index) in bulkRecords" :key="index" class="hover:bg-slate-50/50 transition-colors divide-x divide-slate-50">
                  <td class="px-4 py-2 text-center text-xs font-black text-slate-300">{{ index + 1 }}</td>
                  <td class="px-4 py-2">
                    <select 
                      v-model="row.teamId" 
                      @change="onBulkTeamChange(row)"
                      class="w-full text-sm font-bold bg-transparent border-none focus:ring-0 cursor-pointer text-slate-700 outline-none"
                    >
                      <option disabled :value="null">-- {{ $t('common.select_team') }} --</option>
                      <option v-for="opt in teamOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                    </select>
                  </td>
                  <td class="px-4 py-2">
                    <select v-model="row.productId" class="w-full text-sm font-bold bg-transparent border-none focus:ring-0 cursor-pointer text-slate-700 outline-none">
                      <option disabled :value="null">-- {{ $t('common.select_product') }} --</option>
                      <option v-for="opt in getRowProductOptions(row)" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                    </select>
                  </td>
                  <td class="px-4 py-2">
                    <select v-model="row.qualityId" class="w-full text-sm font-bold bg-transparent border-none focus:ring-0 cursor-pointer text-slate-700 outline-none">
                      <option disabled :value="null">-- {{ $t('common.select_quality') }} --</option>
                      <option v-for="opt in qualityOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                    </select>
                  </td>
                  <td class="px-4 py-2">
                    <input v-model.number="row.quantity" type="number" min="1" class="w-full text-lg font-black bg-transparent border-none focus:ring-0 text-slate-900 outline-none text-right" placeholder="0" />
                  </td>
                  <td class="px-3 py-2 text-center">
                    <button @click="removeBulkRow(index)" class="p-2 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all" :disabled="bulkRecords.length === 1 && index === 0" :class="{'opacity-50 cursor-not-allowed': bulkRecords.length === 1 && index === 0}">
                      <Trash2 class="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div>
             <UiButton @click="addBulkRow" variant="outline" >
               <Plus class="w-4 h-4 mr-1.5" /> {{ $t('production.add_row') }}
             </UiButton>
          </div>
        </div>

        <div class="flex gap-4 mt-8 pt-6 border-t border-slate-100 shrink-0 pl-4">
          <button type="button" @click="showBulkModal = false" class="flex-1 py-3.5 rounded-2xl border border-slate-200 text-slate-500 font-black hover:bg-slate-50 transition-all">{{ $t('common.cancel') }}</button>
          <UiButton @click="handleBulkSubmit" class="flex-[2] h-14 text-lg shadow-xl shadow-primary-200" :loading="saving">{{ $t('production.save_bulk') }}</UiButton>
        </div>
      </div>
    </div>
    <!-- Cell Detail Modal -->
    <div v-if="showDetailModal" class="fixed inset-0 z-[120] flex items-center justify-center bg-slate-900/60 backdrop-blur-md p-4">
      <div class="card w-full max-w-3xl p-8 animate-in zoom-in slide-in-from-bottom duration-300">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-xl font-black text-slate-900 tracking-tight">{{ $t('production.detail_title', { date: detailContext.date }) }}</h3>
            <p class="text-sm text-slate-500 font-medium">{{ $t('common.team') }}: <strong class="text-slate-900">{{ detailContext.teamName }}</strong></p>
          </div>
          <button @click="showDetailModal = false" class="p-2.5 text-slate-400 hover:text-slate-600 bg-slate-50 rounded-full transition-all"><X class="w-5 h-5" /></button>
        </div>

        <div class="overflow-x-auto border border-slate-100 rounded-2xl">
          <table class="w-full text-left">
            <thead>
              <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
                <th class="px-6 py-4">Sản phẩm</th>
                <th class="px-6 py-4">Chất lượng</th>
                <th class="px-6 py-4">Số lượng</th>
                <th class="px-6 py-4 text-right" v-if="!isPastMonth"></th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="rec in detailContext.items" :key="rec.id" class="hover:bg-slate-50/50">
                <td class="px-6 py-4">
                  <div class="font-bold text-sm text-slate-900">{{ rec.product?.code }}</div>
                  <div class="text-[10px] text-slate-400">{{ rec.product?.thickness }}mm x {{ rec.product?.length }}m x {{ rec.product?.width }}m</div>
                </td>
                <td class="px-6 py-4">
                  <span class="px-2 py-0.5 bg-primary-100 rounded text-[10px] font-black text-primary-700 uppercase">{{ rec.quality?.code }}</span>
                </td>
                <td class="px-6 py-4 font-black text-slate-900">{{ rec.quantity?.toLocaleString() }}</td>
                <td class="px-6 py-4 text-right" v-if="!isPastMonth">
                  <div class="flex justify-end gap-2">
                    <button @click="editFromDetail(rec)" class="p-2 text-slate-400 hover:text-primary-600 transition-all"><PencilLine class="w-4 h-4" /></button>
                    <button @click="deleteFromDetail(rec.id)" class="p-2 text-slate-400 hover:text-red-500 transition-all"><Trash2 class="w-4 h-4" /></button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mt-8 flex justify-end gap-3">
          <UiButton v-if="!isPastMonth" @click="addFromDetail" class="bg-slate-900 text-white text-xs font-black uppercase tracking-widest px-6 h-11">{{ $t('production.add_new') }}</UiButton>
          <UiButton @click="showDetailModal = false" variant="outline" class="px-8 h-11">{{ $t('common.cancel') }}</UiButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { 
  History, Plus, ListPlus, Filter, Trash2, PencilLine, 
  X, PlusCircle, AlertCircle, ShieldAlert, Package, 
  Layers, Gavel, ChevronLeft, ChevronRight, User, ArrowRightLeft,
  ChevronDown, Check, LayoutList, Grid3X3
} from 'lucide-vue-next';

const { $api } = useNuxtApp();
const { t } = useI18n();
const records = ref([]);
const teams = ref([]);
const departments = ref([]);
const products = ref([]);
const qualities = ref([]);
const attendances = ref([]);

const teamOptions = computed(() => teams.value.map(t => ({ value: t.id, label: t.name })));
const productOptions = computed(() => products.value.map(p => ({ value: p.id, label: `${p.code} (${p.thickness}x${p.length}x${p.width})` })));
const qualityOptions = computed(() => qualities.value.map(q => ({ value: q.id, label: q.code })));

// Filtered products for single modal
const filteredProducts = ref([]);
const filteredProductOptions = computed(() => {
  const list = filteredProducts.value.length > 0 ? filteredProducts.value : products.value;
  return list.map(p => ({ value: p.id, label: `${p.code} (${p.thickness}x${p.length}x${p.width})` }));
});

// Cache for bulk rows products
const stepProductsCache = reactive({});

const loading = ref(true);
const saving = ref(false);
const exporting = ref(false);
const showModal = ref(false);

const now = new Date();
const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().substr(0, 10);
const lastDayOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).toISOString().substr(0, 10);

const filter = reactive({
  from: firstDayOfMonth,
  to: lastDayOfMonth,
  departmentIds: [],
  teamIds: []
});

// handleClickOutside removed as it is no longer needed with new components

const form = reactive({
  teamId: null,
  productId: null,
  qualityId: null,
  productionDate: new Date().toISOString().substr(0, 10),
  quantity: 0
});

const currentId = ref(null);

const showBulkModal = ref(false);
const bulkDate = ref(new Date().toISOString().substr(0, 10));
const bulkRecords = ref([]);
const bulkError = ref('');

// Calendar View State
const viewMode = ref('list'); // 'list' or 'matrix'
const viewMonth = ref(new Date().toISOString().substr(0, 7)); // YYYY-MM
const viewWeek = ref(null); 
const showDetailModal = ref(false);

// onMounted consolidatied at the end of the file

const currentPage = ref(1);
const itemsPerPage = 20;

const totalPages = computed(() => Math.ceil(filteredTeams.value.length / itemsPerPage) || 1);

const filteredTeams = computed(() => {
  if (filter.teamIds && filter.teamIds.length > 0) {
    return teams.value.filter(t => filter.teamIds.includes(t.id));
  }
  return teams.value;
});

const paginatedTeams = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage;
  const end = start + itemsPerPage;
  return filteredTeams.value.slice(start, end);
});

const detailContext = reactive({
  date: '',
  teamId: null,
  teamName: '',
  items: []
});

const hoverTooltip = reactive({
  show: false,
  x: 0,
  y: 0,
  teamName: '',
  date: '',
  totalQuantity: 0,
  records: [],
  employees: []
});

const isPastMonth = computed(() => {
  const nowStr = new Date().toISOString().substr(0, 7);
  return viewMonth.value < nowStr;
});

const daysInMonth = computed(() => {
  const [year, month] = viewMonth.value.split('-').map(Number);
  return new Date(year, month, 0).getDate();
});

const monthWeeks = computed(() => {
  const [year, month] = viewMonth.value.split('-').map(Number);
  const firstDay = new Date(year, month - 1, 1);
  const lastDay = new Date(year, month, 0);
  const weeks = [];
  
  let currentStart = new Date(firstDay);
  let weekNum = 1;
  
  while (currentStart <= lastDay) {
    const weekEnd = new Date(currentStart);
    weekEnd.setDate(currentStart.getDate() + (6 - currentStart.getDay()));
    
    const actualEnd = weekEnd > lastDay ? lastDay : weekEnd;
    weeks.push({
      id: weekNum,
      label: `Tuần ${weekNum} (${currentStart.getDate()}/${month} - ${actualEnd.getDate()}/${month})`,
      start: currentStart.getDate(),
      end: actualEnd.getDate()
    });
    
    currentStart = new Date(actualEnd);
    currentStart.setDate(actualEnd.getDate() + 1);
    weekNum++;
  }
  return weeks;
});

const findCurrentWeekId = (monthYearStr) => {
  const now = new Date();
  const nowMonthStr = now.toISOString().substr(0, 7);
  
  if (monthYearStr !== nowMonthStr) return null; // Nếu không phải tháng hiện tại thì mặc định xem cả tháng
  
  const today = now.getDate();
  const week = monthWeeks.value.find(w => today >= w.start && today <= w.end);
  return week ? week.id : null;
};

const displayedDays = computed(() => {
  if (!viewWeek.value) return Array.from({ length: daysInMonth.value }, (_, i) => i + 1);
  const week = monthWeeks.value.find(w => w.id === viewWeek.value);
  if (!week) return Array.from({ length: daysInMonth.value }, (_, i) => i + 1);
  
  const days = [];
  for (let d = week.start; d <= week.end; d++) days.push(d);
  return days;
});

const getDayOfWeek = (day) => {
  const [year, month] = viewMonth.value.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  const days = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
  return days[date.getDay()];
};

const isSunday = (day) => {
  const [year, month] = viewMonth.value.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  return date.getDay() === 0;
};

const handleMonthChange = () => {
  viewWeek.value = findCurrentWeekId(viewMonth.value);
  if (viewMode.value === 'matrix') {
    const [year, month] = viewMonth.value.split('-').map(Number);
    filter.from = `${viewMonth.value}-01`;
    const lastDay = new Date(year, month, 0).getDate();
    filter.to = `${viewMonth.value}-${lastDay}`;
    fetchData();
  }
};

const handleDepartmentChange = () => {
  filter.teamIds = [];
  fetchData();
};

const getDayHighlightClass = (day, type) => {
  const dayStr = day < 10 ? `0${day}` : `${day}`;
  const targetDate = `${viewMonth.value}-${dayStr}`;
  
  const now = new Date();
  const todayStr = now.toISOString().substr(0, 10);
  
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const yesterdayStr = yesterday.toISOString().substr(0, 10);
  
  const tomorrow = new Date(now);
  tomorrow.setDate(now.getDate() + 1);
  const tomorrowStr = tomorrow.toISOString().substr(0, 10);

  if (targetDate === todayStr) {
    return type === 'cell' ? 'bg-amber-50/80 hover:bg-amber-100/90' : 'bg-amber-100/30 text-amber-700';
  } else if (targetDate === yesterdayStr) {
    return type === 'cell' ? 'bg-slate-50/80 hover:bg-slate-100/90' : 'bg-slate-100/30';
  } else if (targetDate === tomorrowStr) {
    return type === 'cell' ? 'bg-blue-50/60 hover:bg-blue-100/80' : 'bg-blue-100/20 text-blue-700';
  }
  
  return type === 'cell' ? 'hover:bg-primary-50/50' : '';
};

watch(viewMode, (newVal) => {
  if (newVal === 'matrix') {
    handleMonthChange();
  }
});

const getTeamTotalQuantity = (teamId) => {
  const days = displayedDays.value;
  let total = 0;
  days.forEach(day => {
    total += getMatrixCellQuantity(teamId, day);
  });
  return total;
};

const getMatrixCell = (teamId, day) => {
  const dayStr = day < 10 ? `0${day}` : `${day}`;
  const targetDate = `${viewMonth.value}-${dayStr}`;
  return records.value.filter(r => r.team?.id === teamId && r.productionDate === targetDate);
};

const getMatrixCellQuantity = (teamId, day) => {
  return getMatrixCell(teamId, day).reduce((sum, r) => sum + (r.quantity || 0), 0);
};

const getMatrixCellProducts = (teamId, day) => {
  const items = getMatrixCell(teamId, day);
  const codes = [...new Set(items.map(i => i.product?.code))];
  return codes;
};

const setHoveredCell = (teamId, day, event) => {
  const dayStr = day < 10 ? `0${day}` : `${day}`;
  const targetDate = `${viewMonth.value}-${dayStr}`;
  const cellRecords = getMatrixCell(teamId, day);
  const team = teams.value.find(t => t.id === teamId);
  
  if (cellRecords.length === 0) return;

  // Lấy nhân viên tham gia (bao gồm nhân sự mượn)
  const teamEmployees = attendances.value
    .filter(a => a.actualTeam?.id === teamId && a.attendanceDate === targetDate)
    .map(a => ({
      id: a.employee?.id,
      name: a.employee?.fullName,
      isBorrowed: a.originalTeam?.id !== a.actualTeam?.id,
      fromTeam: a.originalTeam?.name
    }));

  hoverTooltip.teamName = team?.name || '';
  hoverTooltip.date = targetDate;
  hoverTooltip.totalQuantity = getMatrixCellQuantity(teamId, day);
  hoverTooltip.records = cellRecords;
  hoverTooltip.employees = teamEmployees;
  
  // Tính toán vị trí x, y (hiển thị sang phải hoặc trái tuỳ thuộc vào vị trí chuột)
  const tooltipWidth = 288; // w-72 matches Tooltip width
  const windowWidth = window.innerWidth;
  
  hoverTooltip.x = event.clientX + 20;
  if (hoverTooltip.x + tooltipWidth > windowWidth) {
    hoverTooltip.x = event.clientX - tooltipWidth - 20;
  }
  
  hoverTooltip.y = event.clientY - 50;
  hoverTooltip.show = true;
};

const clearHoveredCell = () => {
  hoverTooltip.show = false;
};

const showCellDetails = (teamId, day) => {
  const dayStr = day < 10 ? `0${day}` : `${day}`;
  const targetDate = `${viewMonth.value}-${dayStr}`;
  const items = getMatrixCell(teamId, day);
  const team = teams.value.find(t => t.id === teamId);
  
  detailContext.date = targetDate;
  detailContext.teamId = teamId;
  detailContext.teamName = team?.name || '';
  detailContext.items = items;
  showDetailModal.value = true;
};

const editFromDetail = (rec) => {
  showDetailModal.value = false;
  openModal(rec);
};

const deleteFromDetail = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa bản ghi này?')) return;
  try {
    await $api.delete(`/production-records/${id}`);
    showDetailModal.value = false;
    fetchData();
  } catch (err) {
    alert('Lỗi: ' + (err.response?.data?.message || err.message));
  }
};

const addFromDetail = () => {
  showDetailModal.value = false;
  openModal();
  form.productionDate = detailContext.date;
  form.teamId = detailContext.teamId;
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params = new URLSearchParams();
    if (filter.from) params.append('from', filter.from);
    if (filter.to) params.append('to', filter.to);
    
    if (filter.departmentIds && filter.departmentIds.length > 0) {
      params.append('departmentIds', filter.departmentIds.join(','));
    }
    
    if (filter.teamIds && filter.teamIds.length > 0) {
      params.append('teamIds', filter.teamIds.join(','));
    }

    const { data } = await $api.get(`/production-records?${params.toString()}`);
    records.value = data;
    
    // Nạp dữ liệu danh mục & Chấm công đồng bộ với dải ngày
    const attendanceParams = new URLSearchParams();
    if (filter.from) attendanceParams.append('fromDate', filter.from);
    if (filter.to) attendanceParams.append('toDate', filter.to);
    
    const [teamRes, deptRes, prodRes, qualRes, attRes] = await Promise.all([
      departments.value.length === 0 ? $api.get('/teams') : Promise.resolve({ data: teams.value }),
      departments.value.length === 0 ? $api.get('/departments') : Promise.resolve({ data: departments.value }),
      products.value.length === 0 ? $api.get('/products') : Promise.resolve({ data: products.value }),
      qualities.value.length === 0 ? $api.get('/product-qualities') : Promise.resolve({ data: qualities.value }),
      $api.get(`/attendances?${attendanceParams.toString()}`)
    ]);

    teams.value = teamRes.data;
    departments.value = deptRes.data;
    products.value = prodRes.data;
    qualities.value = qualRes.data;
    attendances.value = attRes.data;

  } catch (err) {
    console.error('Fetch error:', err);
  } finally {
    loading.value = false;
  }
};

const fetchRecords = () => fetchData();

const openModal = async (r = null) => {
  if (r) {
    currentId.value = r.id;
    form.teamId = r.team?.id;
    form.productId = r.product?.id;
    form.qualityId = r.quality?.id;
    form.productionDate = r.productionDate;
    form.quantity = r.quantity;
    
    // Load products for this team's step
    if (r.team?.productionStep?.id) {
       await fetchStepProducts(r.team.productionStep.id);
    }
  } else {
    currentId.value = null;
    form.teamId = null;
    form.productId = null;
    form.qualityId = null;
    form.productionDate = new Date().toISOString().substr(0, 10);
    form.quantity = 0;
    filteredProducts.value = [];
  }
  showModal.value = true;
};

const fetchStepProducts = async (stepId) => {
  if (!stepId) return [];
  if (stepProductsCache[stepId]) return stepProductsCache[stepId];
  
  try {
    const { data } = await $api.get(`/production-steps/${stepId}/products`);
    stepProductsCache[stepId] = data;
    return data;
  } catch (err) {
    console.error('Error fetching step products:', err);
    return [];
  }
};

watch(() => form.teamId, async (newTeamId) => {
  if (!newTeamId) {
    filteredProducts.value = [];
    return;
  }
  const team = teams.value.find(t => t.id === newTeamId);
  if (team?.productionStep?.id) {
    const products = await fetchStepProducts(team.productionStep.id);
    filteredProducts.value = products;
    
    // Logic: Default to most used product/quality for this team
    if (!currentId.value) { // Only for new records
        const { productId, qualityId } = findMostUsed(newTeamId);
        
        // Ensure the suggested product is valid for this step
        if (productId && products.find(p => p.id === productId)) {
            form.productId = productId;
        } else if (products.length > 0) {
            form.productId = products[0].id; // Fallback to first valid product
        }
        
        if (qualityId) {
            form.qualityId = qualityId;
        } else if (qualities.value.length > 0) {
            form.qualityId = qualities.value[0].id;
        }
    } else {
        // Edit mode: Ensure existing product is in the valid list
        if (form.productId && !products.find(p => p.id === form.productId)) {
            form.productId = null;
        }
    }
  } else {
    filteredProducts.value = [];
  }
}, { immediate: true });

const findMostUsed = (teamId) => {
  const teamRecords = records.value.filter(r => r.team?.id === teamId);
  if (teamRecords.length === 0) return { productId: null, qualityId: null };

  const productCounts = {};
  const qualityCounts = {};
  
  teamRecords.forEach(r => {
    if (r.product?.id) productCounts[r.product.id] = (productCounts[r.product.id] || 0) + 1;
    if (r.quality?.id) qualityCounts[r.quality.id] = (qualityCounts[r.quality.id] || 0) + 1;
  });

  const mostUsedProductId = Object.keys(productCounts).length > 0 
    ? parseInt(Object.keys(productCounts).reduce((a, b) => productCounts[a] > productCounts[b] ? a : b))
    : null;
    
  const mostUsedQualityId = Object.keys(qualityCounts).length > 0
    ? parseInt(Object.keys(qualityCounts).reduce((a, b) => qualityCounts[a] > qualityCounts[b] ? a : b))
    : null;

  return { productId: mostUsedProductId, qualityId: mostUsedQualityId };
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    // Backend expects numeric quantity
    const payload = {
      ...form,
      quantity: parseInt(form.quantity)
    };

    if (currentId.value) {
      await $api.put(`/production-records/${currentId.value}`, payload);
    } else {
      await $api.post('/production-records', payload);
    }
    showModal.value = false;
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Có lỗi xảy ra');
  } finally {
    saving.value = false;
  }
};

const onBulkTeamChange = async (row) => {
  if (!row.teamId) return;
  const team = teams.value.find(t => t.id === row.teamId);
  if (team?.productionStep?.id) {
    const validProducts = await fetchStepProducts(team.productionStep.id);
    
    // Smart Defaults for Bulk Row
    const { productId, qualityId } = findMostUsed(row.teamId);
    
    if (productId && validProducts.find(p => p.id === productId)) {
        row.productId = productId;
    } else if (validProducts.length > 0) {
        row.productId = validProducts[0].id;
    }
    
    if (qualityId) {
        row.qualityId = qualityId;
    } else if (qualities.value.length > 0) {
        row.qualityId = qualities.value[0].id;
    }
  }
};

const getRowProductOptions = (row) => {
  if (!row.teamId) return productOptions.value;
  const team = teams.value.find(t => t.id === row.teamId);
  const stepId = team?.productionStep?.id;
  if (stepId && stepProductsCache[stepId]) {
    return stepProductsCache[stepId].map(p => ({ value: p.id, label: `${p.code} (${p.thickness}x${p.length}x${p.width})` }));
  }
  return productOptions.value;
};

const handleExportList = () => handleExport('list');
const handleExportMatrix = () => handleExport('matrix');

const handleExport = async (format = 'list') => {
  exporting.value = true;
  try {
    const params = new URLSearchParams();
    if (format === 'matrix') {
      const [year, month] = viewMonth.value.split('-').map(Number);
      const start = `${viewMonth.value}-01`;
      const end = `${viewMonth.value}-${new Date(year, month, 0).getDate()}`;
      params.append('from', start);
      params.append('to', end);
    } else {
      if (filter.from) params.append('from', filter.from);
      if (filter.to) params.append('to', filter.to);
    }
    
    params.append('format', format);
    if (filter.departmentIds && filter.departmentIds.length > 0) {
      params.append('departmentIds', filter.departmentIds.join(','));
    }
    if (filter.teamIds && filter.teamIds.length > 0) {
      params.append('teamIds', filter.teamIds.join(','));
    }

    const response = await $api.get(`/production-records/export?${params.toString()}`, {
      responseType: 'blob'
    });
    
    const url = window.URL.createObjectURL(new Blob([response]));
    const link = document.createElement('a');
    link.href = url;
    const fileName = format === 'matrix' ? `SanLuong_MaTran_${viewMonth.value}.xlsx` : `SanLuong_DanhSach_${new Date().toISOString().substr(0,10)}.xlsx`;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (err) {
    alert('Lỗi xuất file: ' + (err.response?.data?.message || err.message));
  } finally {
    exporting.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa bản ghi này?')) return;
  try {
    await $api.delete(`/production-records/${id}`);
    fetchData();
  } catch (err) {
    alert(err.response?.data?.message || err.message || 'Lỗi');
  }
};

const openBulkModal = () => {
  bulkDate.value = new Date().toISOString().substr(0, 10);
  bulkRecords.value = [{
    teamId: null,
    productId: null,
    qualityId: null,
    quantity: 0
  }];
  bulkError.value = '';
  showBulkModal.value = true;
};

const addBulkRow = () => {
  const lastRow = bulkRecords.value[bulkRecords.value.length - 1];
  bulkRecords.value.push({
    teamId: null,
    productId: lastRow ? lastRow.productId : null,
    qualityId: lastRow ? lastRow.qualityId : null,
    quantity: 0
  });
};

const removeBulkRow = (index) => {
  if (bulkRecords.value.length > 1) {
    bulkRecords.value.splice(index, 1);
  }
};

const validateBulkRecords = () => {
  bulkError.value = '';
  const lines = bulkRecords.value;
  
  // 1. Kiểm tra dòng trống
  for (let i = 0; i < lines.length; i++) {
    const row = lines[i];
    if (!row.teamId || !row.productId || !row.qualityId || !row.quantity || row.quantity <= 0) {
      bulkError.value = t('production.error_empty_row', { row: i + 1 });
      return false;
    }
  }

  // 2. Kiểm tra trùng lặp trên Form hiện tại
  const seenMap = new Map();
  for (let i = 0; i < lines.length; i++) {
    const row = lines[i];
    const key = `${row.teamId}_${row.productId}_${row.qualityId}`;
    if (seenMap.has(key)) {
      const prevLine = seenMap.get(key);
      bulkError.value = t('production.error_duplicate_form', { prev: prevLine, curr: i + 1 });
      return false;
    }
    seenMap.set(key, i + 1);
  }

  // 3. Kiểm tra trùng lặp với Database đã hiển thị ở bảng (Tạm thời)
  for (let i = 0; i < lines.length; i++) {
    const row = lines[i];
    const existing = records.value.find(r => 
      r.productionDate === bulkDate.value &&
      r.team?.id === row.teamId &&
      r.product?.id === row.productId &&
      r.quality?.id === row.qualityId
    );
    if (existing) {
      const teamName = teamOptions.value.find(t => t.value === row.teamId)?.label || 'Tổ';
      const productName = productOptions.value.find(p => p.value === row.productId)?.label || 'SP';
      const qualityName = qualityOptions.value.find(q => q.value === row.qualityId)?.label || 'CL';
      
      bulkError.value = t('production.error_duplicate_db', { 
        team: teamName, 
        product: productName,
        quality: qualityName,
        date: bulkDate.value 
      });
      return false;
    }
  }

  return true;
};

const handleBulkSubmit = async () => {
  if (!validateBulkRecords()) return;

  saving.value = true;
  try {
    const payload = bulkRecords.value.map(row => ({
      teamId: row.teamId,
      productId: row.productId,
      qualityId: row.qualityId,
      quantity: parseInt(row.quantity),
      productionDate: bulkDate.value
    }));

    await $api.post('/production-records/batch', payload);
    showBulkModal.value = false;
    fetchData();
  } catch (err) {
    const msg = err.response?.data?.message || err.message || 'Error';
    bulkError.value = msg.includes("Duplicate") || msg.includes("constraint") 
      ? t('production.error_db_exists')
      : t('messages.system_error') + ': ' + msg;
  } finally {
    saving.value = false;
  }
};

watch(() => filter.departmentIds, () => {
  fetchData();
}, { deep: true });

watch(() => filter.teamIds, () => {
  fetchData();
}, { deep: true });

onMounted(async () => {
  viewWeek.value = findCurrentWeekId(viewMonth.value);
  await fetchData();
});

onUnmounted(() => {
});
</script>
