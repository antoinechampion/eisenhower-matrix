<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { AttachedDocuments, Notes } from '@/services/api'
import SyncStatus from '@/components/SyncStatus.vue'

// TinyMCE is loaded as a side effect; we use the global tinymce object via a script tag.
// This component is mounted in EditorView and renders a textarea that TinyMCE replaces.

const route = useRoute()
const noteId = route.params.noteId as string

const loading = ref(true)
const isGuest = ref(false)
const initialContent = ref('')
const sharingEnabled = ref(false)
// Drives the sync spinner: true while there are edits not yet persisted.
const pendingSync = ref(false)

let editor: any = null
let changedContent: string | null = null
let saving = false
let saveInterval: ReturnType<typeof setInterval> | null = null

declare const tinymce: any

// TinyMCE ships as a classic (non-module) script exposing a global `tinymce`.
// Load it on demand so the home/matrix pages don't pay for it.
function loadTinyMce(): Promise<void> {
  if (typeof tinymce !== 'undefined') return Promise.resolve()
  return new Promise((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[data-tinymce]')
    if (existing) {
      existing.addEventListener('load', () => resolve())
      existing.addEventListener('error', () => reject(new Error('Failed to load TinyMCE')))
      return
    }
    const script = document.createElement('script')
    script.src = '/tinymce/js/tinymce/tinymce.min.js'
    script.referrerPolicy = 'origin'
    script.dataset.tinymce = 'true'
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('Failed to load TinyMCE'))
    document.head.appendChild(script)
  })
}

async function loadDocument() {
  try {
    const noteResp = await Notes.get(noteId)
    if (noteResp.id) {
      document.title = noteResp.text || 'Editor'
    }
    let docResp = await AttachedDocuments.get(noteId)
    if (docResp.status === 404) {
      await AttachedDocuments.upsert(noteId, '<p>Write your content here...</p>')
      docResp = await AttachedDocuments.get(noteId)
    }
    isGuest.value = docResp.headers?.has('X-Document-Guest') ?? false
    const text = await docResp.text()
    initialContent.value = text
    if (isGuest.value) {
      document.title = text.substring(0, 50).replace(/<[^>]+>/g, '') + '...'
    }
  } catch {
    loading.value = false
  }
}

function initTinyMce() {
  tinymce.init({
    selector: '#tinymce-editor',
    menubar: false,
    height: '100vh',
    content_style: 'body { font-family:Helvetica,Arial,sans-serif; font-size:14px }',
    plugins: 'preview searchreplace autolink code visualblocks fullscreen image link media codesample table charmap pagebreak nonbreaking anchor insertdatetime advlist lists wordcount charmap quickbars emoticons',
    toolbar: 'undo redo | bold italic underline strikethrough | blocks | alignleft aligncenter alignright alignjustify | numlist bullist | forecolor backcolor removeformat | fullscreen | sharingButton',
    setup(ed: any) {
      editor = ed
      ed.ui.registry.addButton('sharingButton', {
        text: 'Share',
        onAction() {
          AttachedDocuments.share(noteId)
          ed.windowManager.open({
            title: 'Share your document',
            body: {
              type: 'panel',
              items: [{
                type: 'htmlpanel',
                html: `<h3>Copy your sharing link below:</h3><br><a href="/editor/${noteId}">/editor/${noteId}</a>`,
              }],
            },
            buttons: [],
          })
        },
      })
    },
    init_instance_callback(ed: any) {
      ed.setContent(initialContent.value)
      if (isGuest.value) {
        ed.getBody().setAttribute('contenteditable', 'false')
        ed.execCommand('ToggleToolbarDrawer')
      }
      ed.on('KeyUp', () => {
        changedContent = ed.getContent()
        pendingSync.value = true
      })
      loading.value = false
      saveInterval = setInterval(saveContentCron, 1000)
    },
  })
}

function saveContentCron() {
  if (changedContent && !saving) {
    const backup = changedContent
    saving = true
    changedContent = null
    AttachedDocuments.upsert(noteId, backup)
      .then(() => { saving = false })
      .catch(() => {
        if (!changedContent) changedContent = backup
        saving = false
      })
      .finally(() => { pendingSync.value = changedContent !== null })
  }
}

onMounted(async () => {
  // The editor tab uses a distinct favicon (matches the legacy editor.html).
  const favicon = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
  if (favicon) favicon.href = '/images/favicon-document.png'
  await loadTinyMce()
  await loadDocument()
  initTinyMce()
})

onUnmounted(() => {
  if (saveInterval) clearInterval(saveInterval)
  editor?.remove()
})
</script>

<template>
  <div class="editor-wrapper">
    <p v-if="loading" class="loading">Loading...</p>
    <textarea id="tinymce-editor" :class="{ hidden: loading }" />
    <div v-if="isGuest" class="sharing-footer">
      This document is being shared in read-only mode
    </div>
    <SyncStatus :active="pendingSync" />
  </div>
</template>

<style scoped>
.editor-wrapper {
  position: relative;
  width: 100%;
  height: 100vh;
}
.loading {
  padding: 16px;
}
.hidden {
  display: none;
}
.sharing-footer {
  background-color: LemonChiffon;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px;
  z-index: 9999;
  font-family: sans-serif;
}
</style>
