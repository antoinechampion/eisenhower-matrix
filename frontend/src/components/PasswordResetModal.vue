<script setup lang="ts">
import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  open: boolean
  mode: 'request' | 'reset'
  token?: string
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
}>()

const auth = useAuthStore()
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const mismatch = ref(false)

// Clear transient state whenever the modal is (re)opened.
watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      auth.resetError = null
      auth.resetSuccess = null
      email.value = ''
      password.value = ''
      confirmPassword.value = ''
      mismatch.value = false
    }
  }
)

function close() {
  emit('update:open', false)
}

async function submit() {
  loading.value = true
  mismatch.value = false
  if (props.mode === 'request') {
    await auth.requestPasswordReset(email.value)
  } else {
    if (password.value !== confirmPassword.value) {
      mismatch.value = true
      loading.value = false
      return
    }
    await auth.resetPassword(props.token ?? '', password.value)
  }
  loading.value = false
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="backdrop" @click.self="close">
      <div class="card" role="dialog" aria-modal="true">
        <button class="close" aria-label="Close" @click="close">&times;</button>

        <h3>{{ mode === 'request' ? 'Reset your password' : 'Choose a new password' }}</h3>

        <div v-if="auth.resetError" class="error-alert" role="alert">{{ auth.resetError }}</div>
        <div v-if="mismatch" class="error-alert" role="alert">The passwords do not match</div>
        <div v-if="auth.resetSuccess" class="success-alert" role="status">{{ auth.resetSuccess }}</div>

        <form v-if="!auth.resetSuccess" @submit.prevent="submit">
          <label v-if="mode === 'request'">
            <span class="label-text">Your email address <span class="required">*</span></span>
            <input v-model="email" type="email" required />
          </label>

          <template v-else>
            <label>
              <span class="label-text">New password <span class="required">*</span></span>
              <input v-model="password" type="password" required />
            </label>
            <label>
              <span class="label-text">Confirm new password <span class="required">*</span></span>
              <input v-model="confirmPassword" type="password" required />
            </label>
          </template>

          <button type="submit" :disabled="loading">
            {{ mode === 'request' ? 'Send reset link' : 'Reset password' }}
          </button>
        </form>

        <button v-else class="link-button" @click="close">Close</button>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.card {
  position: relative;
  width: 320px;
  padding: 24px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
}
.close {
  position: absolute;
  top: 8px;
  right: 12px;
  border: none;
  background: none;
  font-size: 1.4rem;
  line-height: 1;
  color: #888;
  cursor: pointer;
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
.success-alert {
  background: #efe;
  border: 1px solid #8c8;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 12px;
  color: #262;
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
.link-button {
  border: none;
  background: none;
  color: #1c6ca1;
  cursor: pointer;
  font-size: 0.9rem;
  padding: 0;
}
</style>
