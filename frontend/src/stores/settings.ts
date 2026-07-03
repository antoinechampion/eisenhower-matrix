import { defineStore } from 'pinia'

export type PaletteName = 'Post-It' | 'Shade' | 'High Contrast'

export interface NoteColors {
  background: string[]
  border: string[]
}

export interface QuadrantColors {
  q1: string
  q2: string
  q3: string
  q4: string
}

export interface Palette {
  noteColors: NoteColors
  quadrantColors: QuadrantColors
  arrowsColor: string
  textColor: string
}

function makeNoteColors(pairs: [string, string][]): NoteColors {
  return { background: pairs.map((p) => p[0]), border: pairs.map((p) => p[1]) }
}

const PALETTES: Record<PaletteName, Palette> = {
  'Post-It': {
    noteColors: makeNoteColors([
      ['hsla(0, 90%, 89%, 0.7)', 'hsl(0, 70%, 69%)'],
      ['hsla(33, 90%, 87%, 0.7)', 'hsl(33, 70%, 67%)'],
      ['hsla(62, 90%, 91%, 0.7)', 'hsl(62, 70%, 71%)'],
      ['hsla(110, 90%, 92%, 0.7)', 'hsl(110, 70%, 72%)'],
      ['hsla(185, 90%, 90%, 0.7)', 'hsl(185, 70%, 65%)'],
      ['hsla(217, 90%, 86%, 0.7)', 'hsl(217, 70%, 66%)'],
      ['hsla(249, 90%, 91%, 0.7)', 'hsl(249, 70%, 70%)'],
      ['hsla(300, 90%, 94%, 0.7)', 'hsl(300, 70%, 74%)'],
    ]),
    quadrantColors: {
      q1: 'hsl(8, 67%, 95%)',
      q2: 'hsl(92, 52%, 95%)',
      q3: 'hsl(274, 57%, 95%)',
      q4: 'hsl(46, 100%, 95%)',
    },
    arrowsColor: 'hsl(0, 0%, 10%)',
    textColor: 'hsl(0, 0%, 80%)',
  },
  Shade: {
    noteColors: makeNoteColors([
      ['hsla(300, 100%, 92%, 0.7)', 'hsl(300, 100%, 92%)'],
      ['hsla(275, 100%, 89%, 0.7)', 'hsl(275, 100%, 89%)'],
      ['hsla(255, 100%, 86%, 0.7)', 'hsl(255, 100%, 86%)'],
      ['hsla(233, 100%, 86%, 0.7)', 'hsl(233, 100%, 86%)'],
      ['hsla(221, 100%, 87%, 0.7)', 'hsl(221, 100%, 87%)'],
    ]),
    quadrantColors: {
      q1: 'hsl(260, 33%, 94%)',
      q2: 'hsl(260, 33%, 96%)',
      q3: 'hsl(260, 33%, 92%)',
      q4: 'hsl(260, 33%, 90%)',
    },
    arrowsColor: 'hsl(251, 20%, 67%)',
    textColor: 'hsl(251, 20%, 67%)',
  },
  'High Contrast': {
    noteColors: makeNoteColors([['hsl(360, 100%, 100%)', 'hsl(360, 100%, 0%)']]),
    quadrantColors: {
      q1: 'hsl(360, 100%, 100%)',
      q2: 'hsl(360, 100%, 100%)',
      q3: 'hsl(360, 100%, 100%)',
      q4: 'hsl(360, 100%, 100%)',
    },
    arrowsColor: 'hsl(360, 100%, 0%)',
    textColor: 'hsl(360, 100%, 0%)',
  },
}

export const PALETTE_NAMES = Object.keys(PALETTES) as PaletteName[]

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    palette: (localStorage.getItem('palette') ?? 'Post-It') as PaletteName,
    flipMatrix: localStorage.getItem('flipMatrix') === 'true',
    noteColorIndex: 0,
  }),
  getters: {
    activePalette(state): Palette {
      return PALETTES[state.palette] ?? PALETTES['Post-It']
    },
  },
  actions: {
    setPalette(name: PaletteName) {
      this.palette = name
      this.noteColorIndex = 0
      localStorage.setItem('palette', name)
    },
    toggleFlip() {
      this.flipMatrix = !this.flipMatrix
      localStorage.setItem('flipMatrix', String(this.flipMatrix))
    },
    nextNoteColors() {
      const colors = this.activePalette.noteColors
      const idx = this.noteColorIndex % colors.background.length
      this.noteColorIndex++
      return { background: colors.background[idx], border: colors.border[idx] }
    },
  },
})
