<template>
  <div class="h-screen flex flex-col md:flex-row bg-[#F8FAFC] overflow-hidden">
    <!-- CMS Sidebar -->
    <aside 
      :class="[
        'bg-white border-r border-[#E2E8F0] flex flex-col z-50 shadow-[4px_0_24px_rgba(0,0,0,0.02)] transition-all duration-300 ease-in-out h-full overflow-hidden',
        isSidebarCollapsed ? 'w-24' : 'w-full md:w-80'
      ]"
    >
      <!-- Logo Section -->
      <div :class="['p-8 flex items-center gap-4 transition-all duration-300', isSidebarCollapsed ? 'justify-center' : '']">
        <div class="w-12 h-12 bg-primary-600 rounded-2xl flex items-center justify-center text-white shadow-xl shadow-primary-200 rotate-3 group-hover:rotate-0 transition-transform duration-500 shrink-0">
          <LayoutGrid class="w-7 h-7" />
        </div>
        <div v-if="!isSidebarCollapsed" class="animate-in fade-in duration-500">
          <h1 class="font-black text-2xl text-slate-900 leading-tight tracking-tighter italic">PLYWOOD</h1>
          <div class="flex items-center gap-1.5">
            <div class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></div>
            <p class="text-[10px] text-slate-400 font-black uppercase tracking-widest">CMS Dashboard</p>
          </div>
        </div>
      </div>

      <!-- Navigation Menu -->
      <nav class="flex-1 px-4 py-4 space-y-1.5 overflow-y-auto overflow-x-hidden custom-scrollbar">
        <div v-for="group in menuGroups" :key="group.title" class="pb-6">
          <p v-if="!isSidebarCollapsed" class="px-4 py-2 text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] mb-2 animate-in fade-in duration-300">{{ group.title }}</p>
          <div v-else class="h-px bg-slate-100 my-4 mx-2"></div>
          
          <div class="space-y-1">
            <template v-for="item in group.items" :key="item.to">
              <NuxtLink
                v-if="item.to"
                :to="item.to"
                class="nav-link-v2 group relative"
                active-class="nav-link-v2-active"
              >
                <div class="w-10 h-10 rounded-xl flex items-center justify-center transition-colors shadow-sm shrink-0"
                  :class="route.path === item.to ? 'bg-primary-600 text-white' : 'bg-slate-50 text-slate-400 group-hover:bg-primary-50 group-hover:text-primary-600'">
                  <component :is="item.icon" class="w-5 h-5" />
                </div>
                <span v-if="!isSidebarCollapsed" class="flex-1 font-bold text-[13.5px] tracking-tight whitespace-nowrap animate-in slide-in-from-left-2 duration-300">{{ item.label }}</span>
                <ChevronRight v-if="!isSidebarCollapsed" class="w-4 h-4 opacity-0 group-hover:opacity-40 transition-opacity" />
                
                <!-- Tooltip for collapsed mode -->
                <div v-if="isSidebarCollapsed" class="absolute left-full ml-4 px-3 py-2 bg-slate-900 text-white text-xs font-bold rounded-lg opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-[60] whitespace-nowrap shadow-xl">
                  {{ item.label }}
                </div>
              </NuxtLink>
              <div v-else class="nav-link-v2 opacity-50 cursor-not-allowed">
                <div class="w-10 h-10 rounded-xl bg-slate-50 text-slate-400 flex items-center justify-center shadow-sm shrink-0">
                  <component :is="item.icon" class="w-5 h-5" />
                </div>
                <span v-if="!isSidebarCollapsed" class="flex-1 font-bold text-[13.5px] tracking-tight">{{ item.label }}</span>
              </div>
            </template>
          </div>
        </div>
      </nav>

      <!-- User Profile (Bottom) -->
      <div class="p-6 border-t border-slate-100 bg-slate-50/30">
        <div :class="['flex items-center gap-4 transition-all duration-300', isSidebarCollapsed ? 'p-0 bg-transparent border-none' : 'p-3 bg-white border border-slate-200 rounded-2xl shadow-sm hover:shadow-md cursor-pointer group']">
          <div :class="['w-10 h-10 rounded-xl bg-primary-900 flex items-center justify-center text-white font-black shadow-lg shrink-0', isSidebarCollapsed ? 'mx-auto' : '']">
            {{ userInitials }}
          </div>
          <div v-if="!isSidebarCollapsed" class="flex-1 min-w-0 animate-in fade-in duration-300">
            <p class="text-[13px] font-black text-slate-900 truncate tracking-tight">{{ user?.fullName || 'Admin' }}</p>
            <p class="text-[9px] text-primary-600 font-bold uppercase tracking-widest">Administrator</p>
          </div>
          <button v-if="!isSidebarCollapsed" @click="logout" class="p-2 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all animate-in fade-in duration-300">
            <LogOut class="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>

    <!-- Main Content Area -->
    <main class="flex-1 flex flex-col h-full overflow-hidden transition-all duration-300">
      <!-- Floating Header -->
      <header class="h-24 bg-white/70 backdrop-blur-xl border-b border-slate-100 px-10 flex items-center justify-between sticky top-0 z-40 transition-all duration-300">
        <div class="flex items-center gap-6">
          <!-- Toggle Button -->
          <button 
            @click="isSidebarCollapsed = !isSidebarCollapsed"
            class="hidden md:flex w-10 h-10 items-center justify-center bg-slate-50 hover:bg-white hover:shadow-md border border-slate-100 rounded-xl text-slate-400 hover:text-primary-600 transition-all group"
          >
            <Menu v-if="isSidebarCollapsed" class="w-5 h-5 group-hover:scale-110 transition-transform" />
            <ChevronLeft v-else class="w-5 h-5 group-hover:-translate-x-0.5 transition-transform" />
          </button>
          
          <button class="md:hidden p-2 text-slate-500"><Menu /></button>
          
          <div class="flex flex-col">
            <h2 class="text-2xl font-black text-slate-900 tracking-tighter">{{ pageTitle }}</h2>
            <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ route.path === '/' ? 'Home Center' : 'Management' }}</p>
          </div>
        </div>
        
        <div class="flex items-center gap-6">
          <!-- Search UI -->
          <div class="hidden lg:flex items-center bg-slate-100/80 rounded-2xl px-4 py-2.5 w-72 group focus-within:bg-white focus-within:ring-2 focus-within:ring-primary-500/20 transition-all">
            <Search class="w-4 h-4 text-slate-400 group-focus-within:text-primary-600" />
            <input type="text" placeholder="Tìm kiếm nhanh..." class="bg-transparent border-none focus:ring-0 text-sm font-medium w-full ml-3" />
          </div>

          <div class="flex items-center gap-4">
            <button class="w-12 h-12 rounded-2xl border border-slate-200 flex items-center justify-center text-slate-500 hover:bg-white hover:shadow-lg transition-all relative">
              <Bell class="w-5 h-5 text-slate-400" />
              <div class="absolute top-3 right-3 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></div>
            </button>
            <div class="h-10 w-[1px] bg-slate-200 mx-2"></div>
            <div class="flex items-center gap-4 px-1.5 py-1.5 bg-slate-100 rounded-2xl hover:bg-slate-200 transition-all cursor-pointer group">
              <div class="w-9 h-9 rounded-xl bg-white flex items-center justify-center shadow-sm">
                <Settings2 class="w-5 h-5 text-slate-400 group-hover:rotate-90 transition-transform duration-500" />
              </div>
            </div>
          </div>
        </div>
      </header>

      <!-- Dynamic Page Slot -->
      <div class="flex-1 overflow-y-auto p-10 bg-[#F8FAFC]">
        <div class="max-w-[1600px] mx-auto">
          <slot />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { 
  LayoutGrid, LayoutDashboard, Users, Briefcase, Users2, Package, Layers, 
  ClipboardCheck, History, Gavel, Wallet, LogOut, Bell, ChevronRight, Menu, Search, Settings2,
  ShieldCheck, Tags, ShieldAlert, ChevronLeft
} from 'lucide-vue-next';

const { user, logout } = useAuth();
const route = useRoute();
const isSidebarCollapsed = ref(false);

const menuGroups = [
  {
    title: 'Hệ thống',
    items: [
      { to: '/', label: 'Tổng quan CMS', icon: LayoutDashboard },
      { to: '/attendances', label: 'Quản lý Chấm công', icon: ClipboardCheck },
      { to: '/production-records', label: 'Nhật ký Sản lượng', icon: History },
      { to: '/payrolls', label: 'Bảng lương Tổng', icon: Wallet },
    ]
  },
  {
    title: 'Nhân sự',
    items: [
      { to: '/employees', label: 'Danh sách Nhân viên', icon: Users },
      { to: '/departments', label: 'Phòng ban công ty', icon: Briefcase },
      { to: '/roles', label: 'Xây dựng Chức vụ', icon: Users2 },
      { to: '/teams', label: 'Tổ đội sản xuất', icon: Users2 },
    ]
  },
  {
    title: 'Sản xuất',
    items: [
      { to: '/products', label: 'Danh mục Sản phẩm', icon: Package },
      { to: '/quality-layers', label: 'Định Nghĩa Lớp Gỗ', icon: Tags },
      { to: '/product-qualities', label: 'Cấu Hình Chất lượng', icon: ShieldAlert },
      { to: '/production-steps', label: 'Công đoạn vận hành', icon: Layers },
      { to: '/penalty-bonus', label: 'Khen thưởng / Kỷ luật', icon: Gavel },
    ]
  }
];

const pageTitle = computed(() => {
  for (const group of menuGroups) {
    const item = group.items.find(i => i.to === route.path);
    if (item) return item.label;
  }
  return 'Quản trị hệ thống';
});

const userInitials = computed(() => {
  if (!user.value?.fullName) return 'AD';
  const names = user.value.fullName.split(' ');
  if (names.length === 1) return names[0].substring(0, 2).toUpperCase();
  return (names[0][0] + names[names.length - 1][0]).toUpperCase();
});

onMounted(() => {
  if (!useCookie('auth_token').value) navigateTo('/login');
});
</script>

<style>
/* Custom CMS Styles */
.nav-link-v2 {
  @apply flex items-center gap-4 px-4 py-2.5 text-slate-500 hover:text-primary-600 rounded-2xl transition-all duration-300;
}

.nav-link-v2-active {
  @apply bg-primary-50/50 text-primary-600 shadow-[0_8px_16px_rgba(16,185,129,0.08)];
}

.overflow-y-auto::-webkit-scrollbar {
  width: 4px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: transparent;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #E2E8F0;
  border-radius: 10px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #CBD5E1;
}

/* Base CMS spacing and aesthetic */
body {
  @apply bg-[#F8FAFC];
}
</style>
