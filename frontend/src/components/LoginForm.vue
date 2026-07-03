<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const tab = ref<'register' | 'login'>('register')
const email = ref('')
const password = ref('')
const loading = ref(false)

async function submit() {
  loading.value = true
  if (tab.value === 'login') {
    await auth.login(email.value, password.value)
  } else {
    await auth.register(email.value, password.value)
  }
  loading.value = false
}
</script>

<template>
  <div class="card">
    <div class="tabs">
      <button :class="{ active: tab === 'register' }" @click="tab = 'register'">Register</button>
      <button :class="{ active: tab === 'login' }" @click="tab = 'login'">Log In</button>
    </div>

    <div v-if="auth.error" class="error-alert" role="alert">{{ auth.error }}</div>

    <h3>{{ tab === 'register' ? 'Create your matrix' : 'Resume your matrix' }}</h3>

    <form @submit.prevent="submit">
      <label>
        <span class="label-text">Your email address <span class="required">*</span></span>
        <input v-model="email" type="email" required />
      </label>
      <label>
        <span class="label-text">Your password <span class="required">*</span></span>
        <input v-model="password" type="password" required />
      </label>
      <button type="submit" :disabled="loading">
        {{ tab === 'register' ? 'Register' : 'Log In' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.card {
  width: 320px;
  padding: 24px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
}
.tabs {
  display: flex;
  border-bottom: 1px solid #ddd;
  margin-bottom: 20px;
}
.tabs button {
  padding: 8px 16px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 0.95rem;
  color: #555;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.tabs button.active {
  color: #1c6ca1;
  border-bottom-color: #1c6ca1;
  font-weight: 500;
}
.error-alert {
  background: #fee;
  border: 1px solid #f88;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 12px;
  color: #a00;
  font-size: 0.9rem;
}
h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 16px;
  color: #2a2a2a;
}
form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.9rem;
  color: #333;
}
.label-text {
  display: inline;
}
.required {
  color: #c00;
}
input {
  padding: 8px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.95rem;
}
input:focus {
  outline: 2px solid #1c6ca1;
  border-color: transparent;
}
button[type='submit'] {
  padding: 10px;
  background: #1c6ca1;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 500;
  margin-top: 4px;
}
button[type='submit']:disabled {
  opacity: 0.6;
}
</style>
