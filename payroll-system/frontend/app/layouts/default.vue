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
          <h1 class="font-black text-2xl text-slate-900 leading-tight tracking-tighter italic">DucLam</h1>
          <div class="flex items-center gap-1.5">
            <div class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></div>
            <p class="text-[10px] text-slate-400 font-black uppercase tracking-widest">Payroll System</p>
          </div>
        </div>
      </div>

      <!-- Navigation Menu -->
      <nav class="flex-1 px-4 py-4 space-y-1.5 overflow-y-auto overflow-x-hidden custom-scrollbar">
        <div v-for="group in menuGroups" :key="group.title" class="pb-4">
          <div 
            v-if="!isSidebarCollapsed" 
            class="flex items-center justify-between px-4 py-2 cursor-pointer hover:bg-slate-50 rounded-xl transition-all group/header"
            @click="toggleGroup(group.title)"
          >
            <p class="text-[10px] font-black text-slate-400 uppercase tracking-[0.2em] animate-in fade-in duration-300">{{ $t(group.title) }}</p>
            <ChevronDown 
              :class="['w-3.5 h-3.5 text-slate-300 transition-transform duration-300', isGroupCollapsed(group.title) ? '-rotate-90' : '']"
            />
          </div>
          <div v-else class="h-px bg-slate-100 my-4 mx-2"></div>
          
          <div v-show="!isGroupCollapsed(group.title) || isSidebarCollapsed" class="space-y-1 mt-1 animate-in slide-in-from-top-2 duration-300">
            <template v-for="item in group.items" :key="item.to">
              <NuxtLink
                v-if="item.to"
                :to="localePath(item.to)"
                class="nav-link-v2 group relative"
                active-class="nav-link-v2-active"
              >
                <div class="w-10 h-10 rounded-xl flex items-center justify-center transition-colors shadow-sm shrink-0"
                  :class="route.path === item.to ? 'bg-primary-600 text-white' : 'bg-slate-50 text-slate-400 group-hover:bg-primary-50 group-hover:text-primary-600'">
                  <component :is="item.icon" class="w-5 h-5" />
                </div>
                <span v-if="!isSidebarCollapsed" class="flex-1 font-bold text-[13.5px] tracking-tight whitespace-nowrap animate-in slide-in-from-left-2 duration-300">{{ $t(item.label) }}</span>
                <ChevronRight v-if="!isSidebarCollapsed" class="w-4 h-4 opacity-0 group-hover:opacity-40 transition-opacity" />
                
                <!-- Tooltip for collapsed mode -->
                <div v-if="isSidebarCollapsed" class="absolute left-full ml-4 px-3 py-2 bg-slate-900 text-white text-xs font-bold rounded-lg opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-[60] whitespace-nowrap shadow-xl">
                  {{ $t(item.label) }}
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

      <!-- Sidebar Footer -->
      <div class="p-4 border-t border-slate-100 space-y-2">
        <button 
          @click="logout" 
          class="w-full h-12 flex items-center gap-3 px-4 text-slate-500 hover:text-red-600 hover:bg-red-50 rounded-2xl transition-all duration-300 font-bold text-sm group"
        >
          <div class="w-10 h-10 rounded-xl bg-slate-50 flex items-center justify-center group-hover:bg-red-100 shrink-0">
            <LogOut class="w-5 h-5 transition-transform group-hover:scale-110" />
          </div>
          <span v-if="!isSidebarCollapsed">{{ $t('common.logout') }}</span>
        </button>

        <button 
          @click="isSidebarCollapsed = !isSidebarCollapsed"
          class="w-full h-12 hidden md:flex items-center gap-3 px-4 text-slate-400 hover:text-primary-600 hover:bg-primary-50 rounded-2xl transition-all duration-300 font-bold"
        >
          <div class="w-10 h-10 rounded-xl bg-slate-50 flex items-center justify-center shrink-0">
            <component :is="isSidebarCollapsed ? ChevronRight : ChevronLeft" class="w-5 h-5" />
          </div>
          <span v-if="!isSidebarCollapsed" class="text-xs uppercase tracking-widest">{{ isSidebarCollapsed ? $t('common.expand') : $t('common.collapse') }}</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <main class="flex-1 flex flex-col min-w-0 bg-[#F8FAFC]">
      <!-- CMS Header -->
      <header class="h-24 bg-white/80 backdrop-blur-md border-b border-slate-100 px-8 flex items-center justify-between z-40 sticky top-0 shrink-0">
        <div class="flex items-center gap-8">
          <button @click="isSidebarCollapsed = !isSidebarCollapsed" class="p-3 text-slate-400 hover:bg-slate-50 rounded-2xl">
            <Menu class="w-6 h-6" />
          </button>
          
          <div class="flex flex-col">
            <h2 class="text-xl font-black text-slate-900 tracking-tight">{{ $t(pageTitle) }}</h2>
            <p class="text-[10px] text-slate-400 font-black uppercase tracking-widest">{{ new Date().toLocaleDateString('vi-VN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }) }}</p>
          </div>
        </div>

        <div class="flex items-center gap-6">
          <!-- Multi-language Switcher -->
          <div class="relative" id="lang-switcher">
            <button 
              @click="showLangDropdown = !showLangDropdown"
              class="flex items-center gap-2 px-4 py-2 bg-slate-50 hover:bg-slate-100 rounded-xl transition-all border border-slate-100 group"
            >
              <Languages class="w-4 h-4 text-slate-400 group-hover:text-primary-600" />
              <span class="text-xs font-black text-slate-600 uppercase tracking-widest">{{ locale.toUpperCase() }}</span>
            </button>
            <div v-if="showLangDropdown" class="absolute right-0 mt-2 w-40 bg-white rounded-2xl shadow-2xl border border-slate-100 p-2 z-[100] animate-in zoom-in slide-in-from-top-2 duration-200">
              <button 
                v-for="l in locales" 
                :key="l.code"
                @click="changeLanguage(l.code)"
                class="w-full flex items-center justify-between px-4 py-2.5 rounded-xl text-xs font-bold transition-all"
                :class="locale === l.code ? 'bg-primary-50 text-primary-600' : 'text-slate-500 hover:bg-slate-50'"
              >
                {{ l.name }}
                <Check v-if="locale === l.code" class="w-4 h-4" />
              </button>
            </div>
          </div>

          <div class="h-10 w-px bg-slate-100"></div>

          <div class="flex items-center gap-4">
            <div class="flex flex-col items-end">
              <span class="text-sm font-black text-slate-900 leading-none">{{ user?.fullName || 'Administrator' }}</span>
              <span class="text-[10px] font-black text-emerald-500 uppercase tracking-widest mt-1">{{ user?.roles?.[0] || 'System Manager' }}</span>
            </div>
            <div class="relative group">
              <div class="w-12 h-12 rounded-2xl bg-gradient-to-br from-primary-600 to-emerald-600 p-0.5 shadow-lg shadow-emerald-100 transition-transform group-hover:scale-105 duration-300">
                <div class="w-full h-full bg-white rounded-[14px] flex items-center justify-center font-black text-primary-700">
                  {{ userInitials }}
                </div>
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
  ShieldCheck, Tags, ShieldAlert, ChevronLeft, Languages, Check, CalendarCheck2, ChevronDown
} from 'lucide-vue-next';

const { locale, locales, setLocale } = useI18n();
const localePath = useLocalePath();
const showLangDropdown = ref(false);

const currentLocaleName = computed(() => {
  return locales.value.find(l => l.code === locale.value)?.name || 'Language';
});

const changeLanguage = (code) => {
  setLocale(code);
  showLangDropdown.value = false;
};

// Click outside logic for language switcher
const handleLangClickOutside = (event) => {
  const langSwitcher = document.getElementById('lang-switcher');
  if (langSwitcher && !langSwitcher.contains(event.target)) {
    showLangDropdown.value = false;
  }
};

const { user, logout } = useAuth();
const route = useRoute();
const isSidebarCollapsed = ref(false);
const collapsedGroups = ref(new Set());

const toggleGroup = (title) => {
  if (collapsedGroups.value.has(title)) {
    collapsedGroups.value.delete(title);
  } else {
    collapsedGroups.value.add(title);
  }
  saveCollapsedState();
};

const isGroupCollapsed = (title) => collapsedGroups.value.has(title);

const saveCollapsedState = () => {
  if (process.client) {
    localStorage.setItem('sidebar_collapsed_groups', JSON.stringify(Array.from(collapsedGroups.value)));
  }
};

const loadCollapsedState = () => {
  if (process.client) {
    const saved = localStorage.getItem('sidebar_collapsed_groups');
    if (saved) {
      try {
        const arr = JSON.parse(saved);
        collapsedGroups.value = new Set(arr);
      } catch (e) {
        console.error('Error loading collapsed state:', e);
      }
    }
  }
};

const menuGroups = [
  {
    title: 'menu.overview_reports',
    items: [
      { to: '/', label: 'menu.dashboard', icon: LayoutDashboard },
      { to: '/payrolls', label: 'menu.payroll', icon: Wallet },
    ]
  },
  {
    title: 'menu.hr_mgmt',
    items: [
      { to: '/employees', label: 'menu.employees', icon: Users },
      { to: '/salary-processes', label: 'menu.salary_config_process', icon: History },
      { to: '/teams', label: 'menu.teams', icon: Users2 },
      { to: '/departments', label: 'menu.departments', icon: Briefcase },
      { to: '/roles', label: 'menu.positions', icon: ShieldCheck },
    ]
  },
  {
    title: 'menu.attendance_rewards',
    items: [
      { to: '/attendances', label: 'menu.attendance', icon: ClipboardCheck },
      { to: '/penalty-bonus', label: 'menu.penalty_bonus', icon: Gavel },
      { to: '/attendance-definitions', label: 'menu.attendance_definitions', icon: Settings2 },
      { to: '/penalty-bonus-types', label: 'menu.penalty_bonus_types', icon: Settings2 },
    ]
  },
  {
    title: 'menu.production_ops',
    items: [
      { to: '/production-records', label: 'menu.production_records', icon: Layers },
      { to: '/individual-productions', label: 'menu.individual_production', icon: Users },
    ]
  },
  {
    title: 'menu.system_catalog',
    items: [
      { to: '/products', label: 'menu.products', icon: Package },
      { to: '/product-units', label: 'menu.product_units', icon: LayoutGrid },
      { to: '/film-coating-options', label: 'menu.coating_options', icon: ShieldCheck },
      { to: '/quality-layers', label: 'menu.quality_layers', icon: Tags },
      { to: '/product-qualities', label: 'menu.product_qualities', icon: ShieldAlert },
      { to: '/production-steps', label: 'menu.production_steps', icon: Layers },
      { to: '/unit-prices', label: 'menu.unit_prices', icon: Tags },
      { to: '/payroll-config', label: 'menu.payroll_config', icon: Settings2 },
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
  loadCollapsedState();
  document.addEventListener('click', handleLangClickOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleLangClickOutside);
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
