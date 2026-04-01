<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-black text-slate-900">Quản lý Đơn giá Sản phẩm</h2>
        <p class="text-slate-500 font-medium">Thiết lập đơn giá theo từng công đoạn và chất lượng</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="relative group">
          <UiButton variant="outline">
            <Download class="w-4 h-4" />
            Xuất Excel
          </UiButton>
          <div class="absolute right-0 top-full mt-2 w-48 bg-white border border-slate-200 rounded-xl shadow-xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50 p-2">
            <button @click="exportExcel('list')" class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2">
              <Table2 class="w-4 h-4" />
              Dạng danh sách
            </button>
            <button @click="exportExcel('matrix')" class="w-full text-left px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-lg flex items-center gap-2 border-t border-slate-50 mt-1 pt-3">
              <LayoutGrid class="w-4 h-4" />
              Dạng ma trận
            </button>
          </div>
        </div>
        <UiButton @click="openModal()">
          <Plus class="w-4 h-4" />
          Thiết lập đơn giá
        </UiButton>
      </div>
    </div>

    <!-- View Mode Switcher -->
    <div class="card p-6 flex flex-col gap-6">
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div class="flex p-1 bg-slate-100 rounded-2xl w-fit shrink-0">
          <button 
            @click="viewMode = 'list'" 
            :class="['px-6 py-2 text-[11px] font-black uppercase tracking-widest rounded-xl transition-all flex items-center gap-2.5', viewMode === 'list' ? 'bg-white text-primary-700 shadow-xl shadow-slate-300 scale-[1.02]' : 'text-slate-500 hover:text-slate-700 hover:bg-white/50']"
          >
            <Table2 class="w-3 h-3" />
            Danh sách
          </button>
          <button 
            @click="viewMode = 'matrix'" 
            :class="['px-6 py-2 text-[11px] font-black uppercase tracking-widest rounded-xl transition-all flex items-center gap-2.5', viewMode === 'matrix' ? 'bg-white text-primary-700 shadow-xl shadow-slate-300 scale-[1.02]' : 'text-slate-500 hover:text-slate-700 hover:bg-white/50']"
          >
            <LayoutGrid class="w-3 h-3" />
            Ma trận
          </button>
        </div>

        <!-- Filters (Common or Specific) -->
        <div class="flex flex-wrap gap-4 items-end flex-1 justify-end">
          <template v-if="viewMode === 'list'">
            <div class="space-y-1.5 min-w-[200px]">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Lọc theo Sản phẩm</label>
              <select v-model="filters.productId" class="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all">
                <option :value="null">Tất cả sản phẩm</option>
                <option v-for="p in products" :key="p.id" :value="p.id">{{ p.code }} ({{ p.thickness }}x{{ p.length }}x{{ p.width }})</option>
              </select>
            </div>
            <div class="space-y-1.5 min-w-[200px]">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Lọc theo Công đoạn</label>
              <select v-model="filters.stepId" class="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all">
                <option :value="null">Tất cả công đoạn</option>
                <option v-for="s in steps" :key="s.id" :value="s.id">{{ s.name }}</option>
              </select>
            </div>
            <UiButton variant="outline" @click="resetFilters" class="mb-0.5">Làm mới</UiButton>
          </template>
          <template v-else>
            <div class="flex flex-col gap-1.5 min-w-[200px]">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Tìm kiếm Sản phẩm</label>
              <div class="relative">
                <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-300" />
                <input v-model="matrixSearch" type="text" placeholder="Mã sp..." class="input-field py-2 pl-9 text-sm w-full" />
              </div>
            </div>
            <div class="flex flex-col gap-1.5 min-w-[200px]">
                <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest">Tìm kiếm Công đoạn</label>
                <div class="relative">
                    <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-300" />
                    <input v-model="matrixStepSearch" type="text" placeholder="Tên cđ..." class="input-field py-2 pl-9 text-sm w-full" />
                </div>
            </div>
            <div class="flex items-center gap-2 mb-0.5 bg-slate-50 px-4 py-2 rounded-2xl border border-slate-100 h-[42px]">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest cursor-pointer select-none" for="showAllToggle">Show All</label>
              <input type="checkbox" id="showAllToggle" v-model="showAll" class="w-4 h-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500 cursor-pointer" />
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="card overflow-hidden min-h-[400px]">
      <div v-if="loading" class="p-20 flex flex-col items-center justify-center gap-4">
        <div class="w-12 h-12 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin"></div>
        <p class="text-slate-500 font-bold animate-pulse">Đang tải biểu phí...</p>
      </div>

      <!-- List View Table -->
      <div v-else-if="viewMode === 'list'" class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-slate-50 text-slate-500 text-[10px] font-black uppercase tracking-widest border-b border-slate-100">
              <th class="px-6 py-4">Sản phẩm</th>
              <th class="px-6 py-4">Công đoạn</th>
              <th class="px-6 py-4">Chất lượng</th>
              <th class="px-6 py-4 text-emerald-600 text-center">Giá High</th>
              <th class="px-6 py-4 text-center">Giá Low</th>
              <th class="px-6 py-4">Ngày hiệu lực</th>
              <th class="px-6 py-4 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="r in filteredRates" :key="r.id" class="hover:bg-slate-50/50 transition-colors group text-sm font-medium">
              <td class="px-6 py-4">
                <div class="flex flex-col text-xs">
                  <span class="font-black text-slate-900">{{ r.product.code }}</span>
                  <span class="text-[9px] text-slate-400 font-bold uppercase">{{ r.product.thickness }}x{{ r.product.length }}x{{ r.product.width }}</span>
                </div>
              </td>
              <td class="px-6 py-4">
                <span class="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-[9px] font-black uppercase">
                  {{ r.productionStep.name }}
                </span>
              </td>
              <td class="px-6 py-4">
                <span :class="['px-2 py-0.5 rounded text-[9px] font-black uppercase', 
                  r.quality.code === 'A' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700']">
                  {{ r.quality.code }}
                </span>
              </td>
              <td class="px-6 py-4 font-black text-emerald-600 text-xs text-center">
                {{ formatCurrency(r.priceHigh) }}
              </td>
              <td class="px-6 py-4 font-black text-slate-600 text-xs text-center">
                {{ formatCurrency(r.priceLow) }}
              </td>
              <td class="px-6 py-4 text-slate-500 text-xs">
                {{ formatDate(r.effectiveDate) }}
              </td>
              <td class="px-6 py-4 text-right">
                <div class="flex items-center justify-end gap-1">
                  <button @click="viewHistory(r)" title="Xem lịch sử giá" class="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition-all">
                    <History class="w-4 h-4" />
                  </button>
                  <button @click="openModal(r)" class="p-1.5 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all">
                    <PencilLine class="w-4 h-4" />
                  </button>
                  <button @click="handleDelete(r.id)" class="p-1.5 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all">
                    <Trash2 class="w-4 h-4" />
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredRates.length === 0">
              <td colspan="7" class="px-6 py-20 text-center space-y-4">
                <div class="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-300">
                  <FileX class="w-8 h-8" />
                </div>
                <p class="text-slate-500 font-bold">Không tìm thấy đơn giá nào phù hợp với bộ lọc.</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Matrix View Table -->
      <div v-else class="flex flex-col h-full bg-slate-50/50">
        <!-- Matrix Tabs -->
        <div class="flex px-6 border-b border-slate-100 bg-white">
            <button @click="matrixTab = 'LABOR'" :class="['px-6 py-4 text-[10px] font-black uppercase tracking-[0.2em] transition-all border-b-2 relative top-[1px]', matrixTab === 'LABOR' ? 'border-primary-600 text-primary-600' : 'border-transparent text-slate-400 hover:text-slate-600']">
                1. Công lao động (Size Only)
            </button>
            <button @click="matrixTab = 'QUALITY'" :class="['px-6 py-4 text-[10px] font-black uppercase tracking-[0.2em] transition-all border-b-2 relative top-[1px]', matrixTab === 'QUALITY' ? 'border-primary-600 text-primary-600' : 'border-transparent text-slate-400 hover:text-slate-600']">
                2. Lương Sản phẩm (Quality & Size)
            </button>
        </div>

        <div class="overflow-x-auto overflow-y-auto flex-1 p-6">
            <!-- SECTON 1: SIZE ONLY (Labor-only) -->
            <div v-if="matrixTab === 'LABOR'" class="space-y-4">
                <table class="w-full text-left border-collapse min-w-max border border-slate-200 bg-white rounded-xl shadow-sm overflow-hidden">
                    <thead class="sticky top-0 z-30">
                        <tr class="bg-slate-100 text-slate-600 text-[10px] font-black uppercase tracking-widest border-b border-slate-200">
                            <th rowspan="2" class="px-6 py-5 sticky left-0 z-40 bg-slate-100 border-r border-slate-200 min-w-[150px] shadow-[2px_0_5px_rgba(0,0,0,0.02)] text-center">Loại giá</th>
                            <th v-for="s in sizeOnlySteps" :key="s.id" :colspan="getStepProducts(s.id).length || 1" class="px-6 py-3 text-center border-r border-slate-200 bg-amber-50/50 text-amber-700">
                                {{ s.name }}
                            </th>
                        </tr>
                        <tr class="bg-slate-50 text-slate-500 text-[9px] font-black uppercase tracking-widest border-b border-slate-200">
                            <template v-for="s in sizeOnlySteps" :key="s.id">
                                <th v-for="p in getStepProducts(s.id)" :key="p.id" class="px-4 py-3 text-center border-r border-slate-200 min-w-[140px]">
                                    <div class="flex flex-col">
                                        <span>{{ p.code }}</span>
                                        <span class="text-[8px] opacity-60">{{ p.thickness }}x{{ p.length }}x{{ p.width }}</span>
                                    </div>
                                </th>
                                <th v-if="getStepProducts(s.id).length === 0" class="px-4 py-3 text-center border-r border-slate-200 min-w-[140px] italic opacity-40">N/A</th>
                            </template>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-slate-100">
                        <!-- High Price Row -->
                        <tr class="hover:bg-slate-50/30 transition-all border-b border-slate-100">
                            <td class="px-4 py-3 sticky left-0 z-20 bg-white border-r border-slate-200 font-bold text-[9px] uppercase tracking-tighter text-emerald-600 shadow-[2px_0_5_rgba(0,0,0,0.02)] text-center">Giá Cao</td>
                            <template v-for="s in sizeOnlySteps" :key="s.id">
                                <td v-for="p in getStepProducts(s.id)" :key="p.id" class="px-4 py-3 border-r border-slate-100 text-center">
                                    <div class="flex items-center justify-center gap-2">
                                        <span class="font-black text-slate-700 text-xs">{{ formatCurrency(getSizeOnlyRate(p.id, s.id)?.priceHigh) }}</span>
                                        <button @click="openModal(getSizeOnlyRate(p.id, s.id))" v-if="getSizeOnlyRate(p.id, s.id)" class="p-1 hover:bg-slate-100 rounded transition-colors group/edit">
                                            <PencilLine class="w-3 h-3 text-slate-300 group-hover/edit:text-primary-500" />
                                        </button>
                                        <button @click="openModalWith(p.id, s.id, defaultQualityId)" v-else class="p-1 hover:bg-slate-100 rounded transition-colors group/edit">
                                            <Plus class="w-3 h-3 text-slate-300 group-hover/edit:text-primary-500" />
                                        </button>
                                    </div>
                                </td>
                                <td v-if="getStepProducts(s.id).length === 0" class="px-4 py-3 border-r border-slate-100 bg-slate-50/30"></td>
                            </template>
                        </tr>
                        <!-- Low Price Row -->
                        <tr class="hover:bg-slate-50/30 transition-all border-b border-slate-200">
                            <td class="px-4 py-3 sticky left-0 z-20 bg-white border-r border-slate-200 font-bold text-[9px] uppercase tracking-tighter text-slate-400 shadow-[2px_0_5_rgba(0,0,0,0.02)] text-center">Giá Thấp</td>
                            <template v-for="s in sizeOnlySteps" :key="s.id">
                                <td v-for="p in getStepProducts(s.id)" :key="p.id" class="px-4 py-3 border-r border-slate-100 text-center">
                                    <span class="font-black text-slate-500 text-xs">{{ formatCurrency(getSizeOnlyRate(p.id, s.id)?.priceLow) }}</span>
                                </td>
                                <td v-if="getStepProducts(s.id).length === 0" class="px-4 py-3 border-r border-slate-100 bg-slate-50/30"></td>
                            </template>
                        </tr>
                    </tbody>
                </table>
                <div v-if="sizeOnlySteps.length === 0" class="py-20 text-center">
                    <SearchX class="w-12 h-12 text-slate-200 mx-auto mb-4" />
                    <p class="text-slate-400 font-bold">Không tìm thấy công đoạn lao động nào.</p>
                </div>
            </div>

            <!-- SECTION 2: SIZE AND QUALITY (Quality-based) -->
            <div v-else class="space-y-4">
                <table class="w-full text-left border-collapse min-w-max border border-slate-200 bg-white rounded-xl shadow-sm overflow-hidden">
                    <thead class="sticky top-0 z-30">
                        <tr class="bg-slate-100 text-slate-600 text-[10px] font-black uppercase tracking-widest border-b border-slate-200">
                            <th rowspan="2" class="px-6 py-5 sticky left-0 z-40 bg-slate-100 border-r border-slate-200 min-w-[150px] shadow-[2px_0_5px_rgba(0,0,0,0.02)]">Chất lượng</th>
                            <th rowspan="2" class="px-4 py-5 sticky left-[150px] z-40 bg-slate-100 border-r border-slate-200 min-w-[100px] shadow-[2px_0_5px_rgba(0,0,0,0.02)]">Loại giá</th>
                            <th v-for="s in qualitySteps" :key="s.id" :colspan="getStepProducts(s.id).length || 1" class="px-6 py-3 text-center border-r border-slate-200 bg-primary-50/50 text-primary-700">
                                {{ s.name }}
                            </th>
                        </tr>
                        <tr class="bg-slate-50 text-slate-500 text-[9px] font-black uppercase tracking-widest border-b border-slate-200">
                            <template v-for="s in qualitySteps" :key="s.id">
                                <th v-for="p in getStepProducts(s.id)" :key="p.id" class="px-4 py-3 text-center border-r border-slate-200 min-w-[140px]">
                                    <div class="flex flex-col">
                                        <span>{{ p.code }}</span>
                                        <span class="text-[8px] opacity-60">{{ p.thickness }}x{{ p.length }}x{{ p.width }}</span>
                                    </div>
                                </th>
                                <th v-if="getStepProducts(s.id).length === 0" class="px-4 py-3 text-center border-r border-slate-200 min-w-[140px] italic opacity-40">N/A</th>
                            </template>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-slate-100">
                        <template v-for="q in qualities" :key="q.id">
                            <!-- High Price Row -->
                            <tr class="hover:bg-slate-50/30 transition-all border-b border-slate-100">
                                <td rowspan="2" class="px-6 py-4 sticky left-0 z-20 bg-white border-r border-slate-200 font-extrabold text-slate-900 shadow-[2px_0_5px_rgba(0,0,0,0.02)] text-center">
                                    <span class="px-2 py-1 bg-primary-50 text-primary-700 rounded-lg">{{ q.code }}</span>
                                </td>
                                <td class="px-4 py-3 sticky left-[150px] z-20 bg-white border-r border-slate-200 font-bold text-[9px] uppercase tracking-tighter text-emerald-600 shadow-[2px_0_5px_rgba(0,0,0,0.02)] text-center">Giá Cao</td>
                                <template v-for="s in qualitySteps" :key="s.id">
                                    <td v-for="p in getStepProducts(s.id)" :key="p.id" class="px-4 py-3 border-r border-slate-100 text-center">
                                        <div class="flex items-center justify-center gap-2">
                                            <span class="font-black text-slate-700 text-xs">{{ formatCurrency(getRate(p.id, s.id, q.id)?.priceHigh) }}</span>
                                            <button @click="openModal(getRate(p.id, s.id, q.id))" v-if="getRate(p.id, s.id, q.id)" class="p-1 hover:bg-slate-100 rounded transition-colors group/edit">
                                                <PencilLine class="w-3 h-3 text-slate-300 group-hover/edit:text-primary-500" />
                                            </button>
                                            <button @click="openModalWith(p.id, s.id, q.id)" v-else class="p-1 hover:bg-slate-100 rounded transition-colors group/edit">
                                                <Plus class="w-3 h-3 text-slate-300 group-hover/edit:text-primary-500" />
                                            </button>
                                        </div>
                                    </td>
                                    <td v-if="getStepProducts(s.id).length === 0" class="px-4 py-3 border-r border-slate-100 bg-slate-50/30"></td>
                                </template>
                            </tr>
                            <!-- Low Price Row -->
                            <tr class="hover:bg-slate-50/30 transition-all border-b border-slate-200">
                                <td class="px-4 py-3 sticky left-[150px] z-20 bg-white border-r border-slate-200 font-bold text-[9px] uppercase tracking-tighter text-slate-400 shadow-[2px_0_5px_rgba(0,0,0,0.02)] text-center">Giá Thấp</td>
                                <template v-for="s in qualitySteps" :key="s.id">
                                    <td v-for="p in getStepProducts(s.id)" :key="p.id" class="px-4 py-3 border-r border-slate-100 text-center">
                                        <span class="font-black text-slate-500 text-xs">{{ formatCurrency(getRate(p.id, s.id, q.id)?.priceLow) }}</span>
                                    </td>
                                    <td v-if="getStepProducts(s.id).length === 0" class="px-4 py-3 border-r border-slate-100 bg-slate-50/30"></td>
                                </template>
                            </tr>
                        </template>
                    </tbody>
                </table>
                <div v-if="qualitySteps.length === 0" class="py-20 text-center">
                    <SearchX class="w-12 h-12 text-slate-200 mx-auto mb-4" />
                    <p class="text-slate-400 font-bold">Không tìm thấy công đoạn sản phẩm nào.</p>
                </div>
            </div>
        </div>
      </div>
    </div>

    <!-- Modals (Add/Edit, History) -->
    <div v-if="showModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-lg p-8 animate-in zoom-in duration-200 shadow-2xl">
        <div class="flex items-center justify-between mb-8 text-white-900">
          <h3 class="text-xl font-black">{{ currentRate.id ? 'Cập nhật' : 'Thiết lập' }} đơn giá</h3>
          <button @click="showModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-5">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="space-y-1.5">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Sản phẩm</label>
              <select v-model="form.productId" class="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all" required>
                <option value="" disabled>Chọn sản phẩm</option>
                <option v-for="p in products" :key="p.id" :value="p.id">{{ p.code }} ({{ p.thickness }}x{{ p.length }}x{{ p.width }})</option>
              </select>
            </div>
            <div class="space-y-1.5">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Công đoạn</label>
              <select v-model="form.productionStepId" class="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all" required>
                <option value="" disabled>Chọn công đoạn</option>
                <option v-for="s in steps" :key="s.id" :value="s.id">{{ s.name }}</option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="space-y-1.5">
              <label class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">Chất lượng</label>
              <select v-model="form.qualityId" class="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 focus:ring-2 focus:ring-primary-500 outline-none transition-all" required>
                <option value="" disabled>Chọn chất lượng</option>
                <option v-for="q in qualities" :key="q.id" :value="q.id">{{ q.code }} - {{ q.name }}</option>
              </select>
            </div>
            <UiInput v-model="form.effectiveDate" type="date" label="Ngày hiệu lực" required />
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <UiInput v-model="form.priceHigh" type="number" step="0.01" label="Giá Cao (Khi đủ chuyên cần)" placeholder="0.00" required />
            <UiInput v-model="form.priceLow" type="number" step="0.01" label="Giá Thấp (Mặc định)" placeholder="0.00" required />
          </div>
          
          <div class="flex gap-3 pt-4">
            <button type="button" @click="showModal = false" class="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-600 font-bold hover:bg-slate-50 transition-all">Hủy</button>
            <UiButton type="submit" class="flex-1" :loading="saving">Lưu thiết lập</UiButton>
          </div>
        </form>
      </div>
    </div>

    <!-- History Modal -->
    <div v-if="showHistoryModal" class="fixed inset-0 z-[100] flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
      <div class="card w-full max-w-2xl p-8 animate-in zoom-in duration-200 shadow-2xl">
        <div class="flex items-center justify-between mb-8">
          <div>
            <h3 class="text-xl font-black text-slate-900">Lịch sử biến động giá</h3>
            <p class="text-xs text-slate-500 font-medium mt-1">
              Sản phẩm: <span class="font-bold text-slate-900">{{ selectedForHistory?.product?.code }}</span> | 
              Công đoạn: <span class="font-bold text-slate-900">{{ selectedForHistory?.productionStep?.name }}</span> | 
              Chất lượng: <span class="font-bold text-slate-900">{{ selectedForHistory?.quality?.code }}</span>
            </p>
          </div>
          <button @click="showHistoryModal = false" class="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition-all">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="max-h-[400px] overflow-y-auto rounded-xl border border-slate-100">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-slate-50 text-slate-500 text-[9px] font-black uppercase tracking-widest border-b border-slate-100">
                <th class="px-4 py-3">Ngày hiệu lực</th>
                <th class="px-4 py-3 text-emerald-600">Giá High</th>
                <th class="px-4 py-3">Giá Low</th>
                <th class="px-4 py-3">Ngày tạo</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="h in historyRates" :key="h.id" class="text-xs hover:bg-slate-50/50 transition-colors">
                <td class="px-4 py-3 font-bold text-primary-600">{{ formatDate(h.effectiveDate) }}</td>
                <td class="px-4 py-3 font-black text-emerald-600">{{ formatCurrency(h.priceHigh) }}</td>
                <td class="px-4 py-3 font-black text-slate-600">{{ formatCurrency(h.priceLow) }}</td>
                <td class="px-4 py-3 text-slate-400">{{ formatDate(h.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <div class="mt-6 flex justify-end">
          <UiButton variant="outline" @click="showHistoryModal = false">Đóng</UiButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { 
    Plus, 
    PencilLine, 
    Trash2, 
    X, 
    Table2, 
    History, 
    Download, 
    LayoutGrid, 
    Search, 
    SearchX, 
    FileX 
} from 'lucide-vue-next';

const { $api } = useNuxtApp();
const { exportExcel: dlExcel } = useExcel();

const viewMode = ref('list'); // 'list' or 'matrix'
const loading = ref(true);
const saving = ref(false);
const exporting = ref(false);

const rates = ref([]);
const products = ref([]);
const steps = ref([]);
const qualities = ref([]);

// Filters for List View
const filters = reactive({
  productId: null,
  stepId: null
});

// Filters for Matrix View
const matrixSearch = ref('');
const matrixStepSearch = ref('');
const matrixTab = ref('QUALITY'); // 'LABOR' or 'QUALITY'
const showAll = ref(false);

const currentRate = ref({});
const form = reactive({
  productId: '',
  productionStepId: '',
  qualityId: '',
  priceHigh: 0,
  priceLow: 0,
  effectiveDate: new Date().toISOString().split('T')[0]
});

// History Modal State
const showHistoryModal = ref(false);
const historyRates = ref([]);
const selectedForHistory = ref(null);
const showModal = ref(false);

const sizeOnlySteps = computed(() => filteredSteps.value.filter(s => s.priceCalculationType === 'SIZE_ONLY'));
const qualitySteps = computed(() => filteredSteps.value.filter(s => s.priceCalculationType === 'SIZE_AND_QUALITY'));
const defaultQualityId = computed(() => qualities.value[0]?.id || null);

// Computed for List View
const filteredRates = computed(() => {
  if (!Array.isArray(rates.value)) return [];
  return rates.value.filter(r => {
    const matchProduct = !filters.productId || r.product?.id === filters.productId;
    const matchStep = !filters.stepId || r.productionStep?.id === filters.stepId;
    return matchProduct && matchStep;
  });
});

// Computed for Matrix View (Products)
const filteredProducts = computed(() => {
  let list = products.value;
  
  // Lọc theo "Hiển thị tất cả" - mặc định chỉ hiện sản phẩm có đơn giá
  if (!showAll.value) {
    const activeProductIds = new Set(rates.value.map(r => r.product?.id));
    list = list.filter(p => activeProductIds.has(p.id));
  }

  if (!matrixSearch.value) return list;
  const q = matrixSearch.value.toLowerCase();
  return list.filter(p => 
    p.name.toLowerCase().includes(q) || 
    p.code.toLowerCase().includes(q)
  );
});

// Computed for Matrix View (Steps)
const filteredSteps = computed(() => {
  let list = steps.value;

  // Lọc theo "Hiển thị tất cả" - mặc định chỉ hiện công đoạn có đơn giá
  if (!showAll.value) {
    const activeStepIds = new Set(rates.value.map(r => r.productionStep?.id));
    list = list.filter(s => activeStepIds.has(s.id));
  }

  if (!matrixStepSearch.value) return list;
  const q = matrixStepSearch.value.toLowerCase();
  return list.filter(s => 
    s.name.toLowerCase().includes(q)
  );
});

const fetchData = async () => {
  loading.value = true;
  try {
    const [ratesRes, productsRes, stepsRes, qualitiesRes] = await Promise.all([
      $api.get('/product-step-rates'),
      $api.get('/products'),
      $api.get('/production-steps'),
      $api.get('/product-qualities')
    ]);
    
    rates.value = ratesRes.data || [];
    products.value = productsRes.data || [];
    steps.value = stepsRes.data || [];
    qualities.value = qualitiesRes.data || [];
  } catch (err) {
    console.error('Error fetching rate data:', err);
  } finally {
    loading.value = false;
  }
};

const getRate = (productId, stepId, qualityId) => {
  if (!productId || !stepId || !qualityId) return null;
  return rates.value.find(r => 
    r.product?.id === productId && 
    r.productionStep?.id === stepId && 
    r.quality?.id === qualityId
  );
};

// Lấy đơn giá cho công đoạn SIZE_ONLY (dùng chất lượng đầu tiên làm mặc định)
const getSizeOnlyRate = (productId, stepId) => {
    if (!productId || !stepId) return null;
    // Dù là SIZE_ONLY, trong DB vẫn cần 1 qualityId. Ta lấy bản ghi bất kỳ hoặc mặc định.
    return rates.value.find(r => 
        r.product?.id === productId && 
        r.productionStep?.id === stepId
    );
};

// Lấy danh sách sản phẩm thuộc 1 công đoạn (dựa trên các đơn giá đã cấu hình)
const getStepProducts = (stepId) => {
  // Lấy các sản phẩm có ít nhất 1 đơn giá ở công đoạn này
  const activeProductIds = new Set(
    rates.value
      .filter(r => r.productionStep?.id === stepId)
      .map(r => r.product?.id)
  );
  
  let list = products.value.filter(p => activeProductIds.has(p.id));

  // Nếu bật showAll, ta có thể muốn logic khác, nhưng hiện tại user muốn theo cấu hình
  // nên ta vẫn giữ logic filter theo search nếu có
  if (matrixSearch.value) {
    const q = matrixSearch.value.toLowerCase();
    list = list.filter(p => 
      p.name.toLowerCase().includes(q) || 
      p.code.toLowerCase().includes(q)
    );
  }
  
  if (showAll.value && list.length === 0) {
      // Nếu showAll bật và list trống, có thể hiện toàn bộ sản phẩm để user setup mới
      // Tuy nhiên theo prompt user, mỗi công đoạn chỉ có 1 số sản phẩm nhất định.
      // Nếu không có rate nào, ta có thể hiện toàn bộ sản phẩm nếu showAll bật.
      return products.value;
  }

  return list;
};

const resetFilters = () => {
  filters.productId = null;
  filters.stepId = null;
};

const openModal = (r = null) => {
  if (r) {
    currentRate.value = { ...r };
    form.productId = r.product?.id;
    form.productionStepId = r.productionStep?.id;
    form.qualityId = r.quality?.id;
    form.priceHigh = r.priceHigh;
    form.priceLow = r.priceLow;
    form.effectiveDate = r.effectiveDate;
  } else {
    currentRate.value = {};
    form.productId = products.value[0]?.id || '';
    form.productionStepId = steps.value[0]?.id || '';
    form.qualityId = qualities.value[0]?.id || '';
    form.priceHigh = 0;
    form.priceLow = 0;
    
    const today = new Date().toISOString().split('T')[0];
    form.effectiveDate = today;
  }
  showModal.value = true;
};

const openModalWith = (productId, stepId, qualityId) => {
    currentRate.value = {};
    form.productId = productId;
    form.productionStepId = stepId;
    form.qualityId = qualityId;
    form.priceHigh = 0;
    form.priceLow = 0;
    form.effectiveDate = new Date().toISOString().split('T')[0];
    showModal.value = true;
};

const viewHistory = (r) => {
  selectedForHistory.value = r;
  historyRates.value = rates.value
    .filter(item => 
      item.product?.id === r.product?.id && 
      item.productionStep?.id === r.productionStep?.id && 
      item.quality?.id === r.quality?.id
    )
    .sort((a, b) => new Date(b.effectiveDate) - new Date(a.effectiveDate));
  showHistoryModal.value = true;
};

const handleSubmit = async () => {
  saving.value = true;
  try {
    const payload = {
      ...form,
      priceHigh: parseFloat(form.priceHigh),
      priceLow: parseFloat(form.priceLow)
    };
    
    if (currentRate.value.id) {
      await $api.put(`/product-step-rates/${currentRate.value.id}`, payload);
    } else {
      await $api.post('/product-step-rates', payload);
    }
    showModal.value = false;
    await fetchData();
  } catch (err) {
    alert(err.message || 'Lỗi khi lưu dữ liệu');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa đơn giá này?')) return;
  try {
    await $api.delete(`/product-step-rates/${id}`);
    await fetchData();
  } catch (err) {
    alert('Lỗi khi xóa dữ liệu');
  }
};

const exportExcel = async (type) => {
  exporting.value = true;
  try {
    const fileName = type === 'list' ? 'don_gia_danh_sach.xlsx' : 'don_gia_ma_tran.xlsx';
    await dlExcel(`/product-step-rates/export?type=${type}`, fileName);
  } catch (err) {
    console.error('Export failed:', err);
  } finally {
    exporting.value = false;
  }
};

const formatCurrency = (val) => {
  if (val === undefined || val === null) return '0 đ';
  return new Intl.NumberFormat('vi-VN', { 
    style: 'currency', 
    currency: 'VND', 
    maximumFractionDigits: 0 
  }).format(val);
};

const formatDate = (dateStr) => {
  if (!dateStr) return '---';
  const d = new Date(dateStr);
  return d.toLocaleDateString('vi-VN');
};

onMounted(fetchData);
</script>

<style scoped>
.card {
  @apply bg-white rounded-3xl border border-slate-100 shadow-sm relative overflow-hidden;
}

.input-field {
  @apply bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-primary-500/20 transition-all font-bold text-slate-700;
}

.shadow-2xl {
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

/* Custom scrollbar */
.overflow-x-auto::-webkit-scrollbar {
  height: 8px;
}
.overflow-x-auto::-webkit-scrollbar-track {
  @apply bg-slate-50 rounded-full;
}
.overflow-x-auto::-webkit-scrollbar-thumb {
  @apply bg-slate-200 rounded-full hover:bg-slate-300 transition-all;
}
</style>
