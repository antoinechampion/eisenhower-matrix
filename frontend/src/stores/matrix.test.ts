import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useMatrixStore } from './matrix'
import { Boards, Notes } from '@/services/api'

vi.mock('@/services/api', () => ({
  Boards: {
    list: vi.fn(),
  },
  Notes: {
    list: vi.fn(),
    create: vi.fn(),
    delete: vi.fn(),
    batchUpdate: vi.fn(),
  },
}))

describe('Matrix Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads notes for the active board', async () => {
    const store = useMatrixStore()
    vi.mocked(Boards.list).mockResolvedValue([{ id: 'board-1', title: 'Main' }])
    vi.mocked(Notes.list).mockResolvedValue([
      { id: '1', text: 'Task 1', urgency: 0.1, importance: 0.2, boardId: 'board-1', flag: 'QUICK_WIN' },
    ])

    await store.loadNotes()
    expect(store.activeBoardId).toBe('board-1')
    expect(store.notes).toHaveLength(1)
    expect(store.notes[0]!.flag).toBe('QUICK_WIN')
  })

  it('adds a note with null flag', async () => {
    const store = useMatrixStore()
    store.activeBoardId = 'board-1'
    const createdNote = { id: 'note-new', text: '', urgency: 0.1, importance: 0.1, boardId: 'board-1', flag: null }
    vi.mocked(Notes.create).mockResolvedValue(createdNote)

    await store.addNote()
    expect(Notes.create).toHaveBeenCalledWith({
      text: '',
      urgency: 0.1,
      importance: 0.1,
      boardId: 'board-1',
      flag: null,
    })
    expect(store.notes).toContainEqual(createdNote)
  })

  it('updates note properties and marks note as dirty', () => {
    const store = useMatrixStore()
    store.notes = [
      { id: 'note-1', text: 'Original', urgency: 0.1, importance: 0.1, boardId: 'board-1', flag: null },
    ]

    store.updateNote('note-1', { flag: 'QUICK_WIN' })
    expect(store.notes[0]!.flag).toBe('QUICK_WIN')
    expect(store.dirtyNoteIds.has('note-1')).toBe(true)
  })

  it('batchSave sends flag in payload and clears dirty note IDs', async () => {
    const store = useMatrixStore()
    store.notes = [
      { id: 'note-1', text: 'Task 1', urgency: 0.3, importance: 0.4, boardId: 'board-1', flag: 'QUICK_WIN' },
      { id: 'note-2', text: 'Task 2', urgency: 0.5, importance: 0.6, boardId: 'board-1', flag: 'LONG_TERM' },
      { id: 'note-3', text: 'Task 3', urgency: 0.7, importance: 0.8, boardId: 'board-1', flag: null },
      { id: 'note-4', text: 'Task 4', urgency: 0.9, importance: 0.9, boardId: 'board-1' },
    ]
    store.dirtyNoteIds.add('note-1')
    store.dirtyNoteIds.add('note-2')
    store.dirtyNoteIds.add('note-3')
    store.dirtyNoteIds.add('note-4')

    vi.mocked(Notes.batchUpdate).mockResolvedValue({ status: 200 })

    await store.batchSave()

    expect(Notes.batchUpdate).toHaveBeenCalledWith([
      { id: 'note-1', text: 'Task 1', urgency: 0.3, importance: 0.4, flag: 'QUICK_WIN' },
      { id: 'note-2', text: 'Task 2', urgency: 0.5, importance: 0.6, flag: 'LONG_TERM' },
      { id: 'note-3', text: 'Task 3', urgency: 0.7, importance: 0.8, flag: null },
      { id: 'note-4', text: 'Task 4', urgency: 0.9, importance: 0.9, flag: null },
    ])
    expect(store.dirtyNoteIds.size).toBe(0)
  })

  it('batchSave restores dirty IDs if batch update fails', async () => {
    const store = useMatrixStore()
    store.notes = [
      { id: 'note-1', text: 'Task 1', urgency: 0.3, importance: 0.4, boardId: 'board-1', flag: 'QUICK_WIN' },
    ]
    store.dirtyNoteIds.add('note-1')

    vi.mocked(Notes.batchUpdate).mockRejectedValue(new Error('Network error'))

    await store.batchSave()

    expect(store.dirtyNoteIds.has('note-1')).toBe(true)
    expect(store.saving).toBe(false)
  })
})
