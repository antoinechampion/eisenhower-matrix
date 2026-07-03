<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { useMatrixStore } from '@/stores/matrix'
import { useSettingsStore } from '@/stores/settings'
import { useRouter } from 'vue-router'
import NoteCard from './NoteCard.vue'
import MatrixSettings from './MatrixSettings.vue'
import BackgroundCanvas from './BackgroundCanvas.vue'
import SyncStatus from './SyncStatus.vue'

const matrix = useMatrixStore()
const settings = useSettingsStore()
const router = useRouter()
const syncing = computed(() => matrix.dirtyNoteIds.size > 0 || matrix.saving)
const settingsRef = ref<InstanceType<typeof MatrixSettings> | null>(null)
const lastSelectedNoteId = ref<string | null>(null)
let saveInterval: ReturnType<typeof setInterval>

const canvasWidth = ref(window.innerWidth)
const canvasHeight = ref(window.innerHeight)

function onResize() {
  canvasWidth.value = window.innerWidth
  canvasHeight.value = window.innerHeight
}

onMounted(async () => {
  await matrix.loadNotes()
  // Assign palette colors to notes
  for (const note of matrix.notes) {
    if (!note.background) {
      const colors = settings.nextNoteColors()
      note.background = colors.background
      note.border = colors.border
    }
  }
  saveInterval = setInterval(() => matrix.batchSave(), 1000)
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  clearInterval(saveInterval)
  window.removeEventListener('resize', onResize)
})

async function addNote() {
  await matrix.addNote()
  const newNote = matrix.notes[matrix.notes.length - 1]
  if (newNote && !newNote.background) {
    const colors = settings.nextNoteColors()
    newNote.background = colors.background
    newNote.border = colors.border
  }
}

async function removeNote() {
  if (!lastSelectedNoteId.value) return
  const note = matrix.notes.find((n) => n.id === lastSelectedNoteId.value)
  if (!note) return
  if (!confirm(`Remove note with content '${note.text}'?`)) return
  await matrix.deleteNote(lastSelectedNoteId.value)
  lastSelectedNoteId.value = null
}

function openEditor() {
  if (!lastSelectedNoteId.value) return
  window.open(`/editor/${lastSelectedNoteId.value}`, '_blank')
}

function onNoteUpdate(noteId: string, patch: { text?: string; urgency?: number; importance?: number }) {
  matrix.updateNote(noteId, patch)
}
</script>

<template>
  <div class="matrix-wrapper" @click.self="lastSelectedNoteId = null">
    <BackgroundCanvas />

    <div class="matrix-container">
      <NoteCard
        v-for="note in matrix.notes"
        :key="note.id"
        :noteId="note.id"
        :text="note.text"
        :urgency="note.urgency"
        :importance="note.importance"
        :background="note.background ?? '#fff'"
        :border="note.border ?? '#ccc'"
        :canvasWidth="canvasWidth"
        :canvasHeight="canvasHeight"
        @update="(patch) => onNoteUpdate(note.id, patch)"
        @focus="(id) => (lastSelectedNoteId = id)"
      />
    </div>

    <div class="buttons-bar">
      <MatrixSettings ref="settingsRef" />
      <svg @click="addNote" xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 16 16" class="icon">
        <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM8.5 4.5a.5.5 0 0 0-1 0v3h-3a.5.5 0 0 0 0 1h3v3a.5.5 0 0 0 1 0v-3h3a.5.5 0 0 0 0-1h-3v-3z" />
      </svg>
      <svg @click="removeNote" xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 16 16" class="icon">
        <path d="M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM4.5 7.5a.5.5 0 0 0 0 1h7a.5.5 0 0 0 0-1h-7z" />
      </svg>
      <svg @click="openEditor" xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 16 16" class="icon">
        <path d="M4 0h5.293A1 1 0 0 1 10 .293L13.707 4a1 1 0 0 1 .293.707V14a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V2a2 2 0 0 1 2-2zm5.5 1.5v2a1 1 0 0 0 1 1h2l-3-3z" />
      </svg>
      <svg @click="settingsRef?.toggleMenu()" xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24" class="icon">
        <path d="m9.25 22l-.4-3.2q-.325-.125-.612-.3q-.288-.175-.563-.375L4.7 19.375l-2.75-4.75l2.575-1.95Q4.5 12.5 4.5 12.337v-.675q0-.162.025-.337L1.95 9.375l2.75-4.75l2.975 1.25q.275-.2.575-.375q.3-.175.6-.3l.4-3.2h5.5l.4 3.2q.325.125.613.3q.287.175.562.375l2.975-1.25l2.75 4.75l-2.575 1.95q.025.175.025.337v.675q0 .163-.05.338l2.575 1.95l-2.75 4.75l-2.95-1.25q-.275.2-.575.375q-.3.175-.6.3l-.4 3.2Zm2.8-6.5q1.45 0 2.475-1.025Q15.55 13.45 15.55 12q0-1.45-1.025-2.475Q13.5 8.5 12.05 8.5q-1.475 0-2.488 1.025Q8.55 10.55 8.55 12q0 1.45 1.012 2.475Q10.575 15.5 12.05 15.5Z" />
      </svg>
    </div>

    <SyncStatus :active="syncing" />
  </div>
</template>

<style scoped>
.matrix-wrapper {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}
.matrix-container {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}
.buttons-bar {
  position: absolute;
  z-index: 1000;
  bottom: 20px;
  right: 50%;
  transform: translate(50%, 0);
  display: flex;
  align-items: center;
  gap: 4px;
}
.icon {
  fill: #1c6ca144;
  transition: all 0.3s;
  cursor: pointer;
  margin: 4px;
}
.icon:hover {
  fill: #1c6ca1;
  transform: scale(1.3);
}
</style>
