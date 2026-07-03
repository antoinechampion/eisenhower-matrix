<script setup lang="ts">
import { ref } from 'vue'
import { useSettingsStore, PALETTE_NAMES, type PaletteName } from '@/stores/settings'
import { useAuthStore } from '@/stores/auth'

const settings = useSettingsStore()
const auth = useAuthStore()
const open = ref(false)

function toggleMenu() {
  open.value = !open.value
}

function selectPalette(name: PaletteName) {
  settings.setPalette(name)
}

defineExpose({ toggleMenu })
</script>

<template>
  <div class="settings-wrapper">
    <div v-if="open" class="settings-menu">
      <button class="menu-item logout" @click="auth.logout">Log Out</button>
      <hr />
      <div class="menu-label">Palette</div>
      <button
        v-for="name in PALETTE_NAMES"
        :key="name"
        class="menu-item"
        :class="{ selected: settings.palette === name }"
        @click="selectPalette(name)"
      >
        {{ name }}
      </button>
      <hr />
      <button class="menu-item" :class="{ selected: settings.flipMatrix }" @click="settings.toggleFlip">
        Flip Matrix
      </button>
    </div>
  </div>
</template>

<style scoped>
.settings-wrapper {
  position: relative;
  display: inline-block;
}
.settings-menu {
  position: absolute;
  bottom: 44px;
  right: 0;
  background: white;
  border: 1px solid #ccc;
  border-radius: 4px;
  min-width: 160px;
  padding: 4px 0;
  z-index: 2000;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.menu-item {
  display: block;
  width: 100%;
  padding: 8px 16px;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
}
.menu-item:hover {
  background: #f0f0f0;
}
.menu-item.selected {
  font-weight: bold;
}
.menu-item.logout {
  color: #a00;
}
.menu-label {
  padding: 4px 16px;
  font-size: 0.75rem;
  color: #888;
  text-transform: uppercase;
}
hr {
  border: none;
  border-top: 1px solid #eee;
  margin: 4px 0;
}
</style>
