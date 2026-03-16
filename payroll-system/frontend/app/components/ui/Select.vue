<template>
  <div class="flex flex-col gap-1.5 w-full">
    <label v-if="label" :for="id" class="text-sm font-semibold text-slate-700 ml-1">
      {{ label }}
    </label>
    <div class="relative w-full">
      <div v-if="$slots.icon" class="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
        <slot name="icon" />
      </div>
      <select
        :id="id"
        :value="modelValue"
        @change="$emit('update:modelValue', $event.target.value)"
        v-bind="$attrs"
        class="input-field appearance-none w-full"
        :class="{ 'pl-11': $slots.icon }"
      >
        <option :value="null" disabled selected>{{ placeholder || 'Chọn một tùy chọn' }}</option>
        <option v-for="option in options" :key="option[valueKey]" :value="option[valueKey]">
          {{ option[labelKey] }}
        </option>
      </select>
      <div class="absolute right-3.5 top-1/2 -translate-y-1/2 pointer-events-none text-slate-400">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-chevron-down"><path d="m6 9 6 6 6-6"/></svg>
      </div>
    </div>
    <p v-if="error" class="text-xs text-red-500 ml-1 mt-0.5">{{ error }}</p>
  </div>
</template>

<script setup>
defineProps({
  modelValue: [String, Number],
  label: String,
  id: String,
  error: String,
  placeholder: String,
  options: {
    type: Array,
    default: () => []
  },
  labelKey: {
    type: String,
    default: 'label'
  },
  valueKey: {
    type: String,
    default: 'value'
  }
});
defineEmits(['update:modelValue']);
</script>
