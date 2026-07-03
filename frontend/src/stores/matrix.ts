import { defineStore } from 'pinia'
import { Boards, Notes } from '@/services/api'

export interface Note {
  id: string
  text: string
  urgency: number
  importance: number
  boardId: string
  background?: string
  border?: string
}

export const useMatrixStore = defineStore('matrix', {
  state: () => ({
    activeBoardId: null as string | null,
    notes: [] as Note[],
    dirtyNoteIds: new Set<string>(),
    saving: false,
  }),
  actions: {
    async loadNotes() {
      const boards = await Boards.list()
      if (!boards || boards.length === 0) return
      this.activeBoardId = boards[0].id
      const notes = await Notes.list(this.activeBoardId!)
      this.notes = notes
    },
    async addNote() {
      if (!this.activeBoardId) return
      const note = await Notes.create({
        text: '',
        urgency: 0.1,
        importance: 0.1,
        boardId: this.activeBoardId,
      })
      this.notes.push(note)
    },
    async deleteNote(noteId: string) {
      await Notes.delete(noteId)
      this.notes = this.notes.filter((n) => n.id !== noteId)
      this.dirtyNoteIds.delete(noteId)
    },
    updateNote(noteId: string, patch: Partial<Note>) {
      const note = this.notes.find((n) => n.id === noteId)
      if (note) {
        Object.assign(note, patch)
        this.dirtyNoteIds.add(noteId)
      }
    },
    async batchSave() {
      if (this.dirtyNoteIds.size === 0 || this.saving) return
      this.saving = true
      const ids = [...this.dirtyNoteIds]
      const batch = this.notes
        .filter((n) => ids.includes(n.id))
        .map((n) => ({ id: n.id, text: n.text, urgency: n.urgency, importance: n.importance }))
      this.dirtyNoteIds.clear()
      await Notes.batchUpdate(batch).catch(() => {
        ids.forEach((id) => this.dirtyNoteIds.add(id))
      })
      this.saving = false
    },
  },
})
