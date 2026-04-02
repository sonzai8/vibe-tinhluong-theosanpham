<template>
  <div class="flex flex-col gap-1.5 w-full">
    <label v-if="label" :for="id" class="text-[10px] font-black text-slate-400 uppercase tracking-widest px-1">
      {{ label }}
    </label>
    <div class="relative group">
      <div v-if="$slots.icon" class="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 group-focus-within:text-primary-500 transition-colors">
        <slot name="icon" />
      </div>
      <input
        :id="id"
        ref="inputRef"
        type="text"
        :value="displayValue"
        @input="handleInput"
        @blur="handleBlur"
        v-bind="$attrs"
        class="w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 outline-none focus:bg-white focus:border-slate-200 focus:ring-4 focus:ring-primary-500/5 transition-all shadow-sm"
        :class="{ 'pl-11': $slots.icon, 'pr-16': isCurrency }"
        placeholder="0"
      />
      <div v-if="isCurrency" class="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none">
        <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">VND</span>
      </div>
    </div>
    <p v-if="error" class="text-xs text-red-500 ml-1 mt-0.5 font-medium">{{ error }}</p>
  </div>
</template>

<script setup>
const props = defineProps({
  modelValue: [String, Number],
  label: String,
  id: String,
  error: String,
  isCurrency: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue']);
const inputRef = ref(null);

// Format number with dots as thousand separators and comma as decimal separator
const formatNumber = (val) => {
  if (val === null || val === undefined || val === '') return '';
  
  // Convert to string and replace any existing separators to get raw number parts
  const parts = val.toString().replace(/\./g, '').replace(',', '.').split('.');
  
  // Format integer part with dots
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  
  // Join with comma for decimal if exists
  return parts.join(',');
};

// Parse formatted string back to raw number
const parseNumber = (val) => {
  if (!val) return 0;
  // Remove dots (thousand separators) and replace comma with dot (decimal)
  const cleanVal = val.toString().replace(/\./g, '').replace(',', '.');
  const num = parseFloat(cleanVal);
  return isNaN(num) ? 0 : num;
};

const displayValue = ref(formatNumber(props.modelValue));

// Sync display value when modelValue changes externally
watch(() => props.modelValue, (newVal) => {
  const formatted = formatNumber(newVal);
  if (formatted !== displayValue.value) {
    displayValue.value = formatted;
  }
});

const handleInput = (e) => {
  const input = e.target;
  let cursorSlot = input.selectionStart;
  const oldVal = input.value;
  
  // Only allow digits, commas and dots (as we will reformat)
  let rawStr = oldVal.replace(/[^0-9,]/g, '');
  
  // Ensure only one comma for decimals
  const commaIndex = rawStr.indexOf(',');
  if (commaIndex !== -1) {
    rawStr = rawStr.slice(0, commaIndex + 1) + rawStr.slice(commaIndex + 1).replace(/,/g, '');
  }

  const rawNum = parseNumber(rawStr);
  const formatted = formatNumber(rawStr);
  
  displayValue.value = formatted;
  emit('update:modelValue', rawNum);

  // Restore cursor position logic
  nextTick(() => {
    if (inputRef.value) {
      // Calculate how many characters were added/removed (formatting characters)
      const diff = formatted.length - oldVal.length;
      inputRef.value.setSelectionRange(cursorSlot + diff, cursorSlot + diff);
    }
  });
};

const handleBlur = () => {
  // Ensure final state is cleaned up
  displayValue.value = formatNumber(props.modelValue);
};
</script>

<style scoped>
.input-field {
  @apply w-full bg-slate-50 border border-transparent rounded-xl px-4 py-2.5 text-sm font-bold text-slate-700 outline-none focus:bg-white focus:border-slate-200 transition-all shadow-sm;
}
</style>
