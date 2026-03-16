<template>
  <div class="h-screen flex flex-col md:flex-row bg-[#F8FAFC]">
    <!-- CMS Sidebar -->
    <aside class="w-full md:w-80 bg-white border-r border-[#E2E8F0] flex flex-col z-50 shadow-[4px_0_24px_rgba(0,0,0,0.02)]">
      <!-- Logo Section -->
      <div class="p-8 flex items-center gap-4">
        <div class="w-12 h-12 bg-primary-600 rounded-2xl flex items-center justify-center text-white shadow-xl shadow-primary-200 rotate-3 group-hover:rotate-0 transition-transform duration-500">
          <LayoutGrid class="w-7 h-7" />
        </div>
        <div>
          <h1 class="font-black text-2xl text-slate-900 leading-tight tracking-tighter italic">PLYWOOD</h1>
          <div class="flex items-center gap-1.5">
            <div class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></div>
            <p class="text-[10px] text-slate-400 font-black uppercase tracking-widest">CMS Dashboard</p>
          </div>
        </div>
      </div>

      <!-- Navigation Menu -->
      <nav class="flex-1 px-6 py-4 space-y-1.5 overflow-y-auto">
        <div v-for="group in menuGroups" :key="group.title" class="pb-6">
          <p class="px-4 py-2 text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] mb-2">{{ group.title }}</p>
          <div class="space-y-1">
            <Component
              v-for="item in group.items"
              :key="item.to"
              :is="item.to ? 'NuxtLink' : 'div'"
              :to="item.to"
              class="nav-link-v2"
              active-class="nav-link-v2-active"
            >
              <div class="w-10 h-10 rounded-xl flex items-center justify-center transition-colors shadow-sm"
                :class="route.path === item.to ? 'bg-primary-600 text-white' : 'bg-slate-50 text-slate-400 group-hover:bg-primary-50 group-hover:text-primary-600'">
                <component :is="item.icon" class="w-5 h-5" />
              </div>
              <span class="flex-1 font-bold text-[13.5px] tracking-tight">{{ item.label }}</span>
              <ChevronRight v-if="item.to" class="w-4 h-4 opacity-0 group-hover:opacity-40 transition-opacity" />
            </Component>
          </div>
        </div>
      </nav>

      <!-- User Profile (Bottom) -->
      <div class="p-6 border-t border-slate-100 bg-slate-50/30">
        <div class="flex items-center gap-4 p-3 bg-white border border-slate-200 rounded-2xl shadow-sm hover:shadow-md transition-all cursor-pointer group">
          <div class="w-10 h-10 rounded-xl bg-primary-900 flex items-center justify-center text-white font-black shadow-lg">
            {{ userInitials }}
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-[13px] font-black text-slate-900 truncate tracking-tight">{{ user?.fullName || 'Admin' }}</p>
            <p class="text-[9px] text-primary-600 font-bold uppercase tracking-widest">Administrator</p>
          </div>
          <button @click="logout" class="p-2 text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all">
            <LogOut class="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>

    <!-- Main Content Area -->
    <main class="flex-1 flex flex-col h-full overflow-hidden">
      <!-- Floating Header -->
      <header class="h-24 bg-white/70 backdrop-blur-xl border-b border-slate-100 px-10 flex items-center justify-between sticky top-0 z-40 transition-all duration-300">
        <div class="flex items-center gap-4">
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
  ClipboardCheck, History, Gavel, Wallet, LogOut, Bell, ChevronRight, Menu, Search, Settings2
} from 'lucide-vue-next';

const { user, logout } = useAuth();
const route = useRoute();

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
  return user.value.fullName.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
});

onMounted(() => {
  if (!useCookie('auth_token').value) navigateTo('/login');
});
</script>

<style>
/* Custom CMS Styles */
.nav-link-v2 {
  @apply flex items-center gap-4 px-4 py-2.5 text-slate-500 hover:text-primary-600 rounded-2xl transition-all duration-300 group;
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
