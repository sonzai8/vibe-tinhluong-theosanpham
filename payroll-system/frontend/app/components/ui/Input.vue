<template>
  <div class="flex flex-col gap-1.5 w-full">
    <label v-if="label" :for="id" class="text-sm font-semibold text-slate-700 ml-1">
      {{ label }}
    </label>
    <div class="relative">
      <div v-if="$slots.icon" class="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
        <slot name="icon" />
      </div>
      <input
        :id="id"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        v-bind="$attrs"
        class="input-field"
        :class="{ 'pl-11': $slots.icon }"
      />
    </div>
    <p v-if="error" class="text-xs text-red-500 ml-1 mt-0.5">{{ error }}</p>
  </div>
</template>

<script setup>
defineProps({
  modelValue: [String, Number],
  label: String,
  id: String,
  error: String
});
defineEmits(['update:modelValue']);
</script>
