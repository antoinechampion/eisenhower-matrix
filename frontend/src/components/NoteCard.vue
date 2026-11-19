<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import type { ResizeEvent, DragEvent as InteractDragEvent } from '@interactjs/types'
import type { NoteFlag } from '@/stores/matrix'

export type { NoteFlag }

export interface NoteCardProps {
  noteId: string
  text: string
  urgency: number
  importance: number
  flag?: NoteFlag
  background: string
  border: string
  canvasWidth: number
  canvasHeight: number
}

const props = defineProps<NoteCardProps>()
const emit = defineEmits<{
  update: [patch: { text?: string; urgency?: number; importance?: number; flag?: NoteFlag }]
  focus: [noteId: string]
}>()

const el = ref<HTMLDivElement | null>(null)
let interactInstance: ReturnType<typeof import('interactjs')['default']> | null = null

const x = ref(props.urgency * props.canvasWidth)
const y = ref(props.importance * props.canvasHeight)

onMounted(async () => {
  const Interact = (await import('interactjs')).default
  if (!el.value) return

  interactInstance = Interact(el.value)
    .resizable({
      edges: { left: true, right: true, bottom: true, top: true },
      ignoreFrom: '.flag-badge',
      listeners: {
        move(event: ResizeEvent) {
          const target = event.target as HTMLElement
          let dx = parseFloat(target.dataset.x ?? '0')
          let dy = parseFloat(target.dataset.y ?? '0')
          target.style.width = event.rect.width + 'px'
          target.style.height = event.rect.height + 'px'
          dx += event.deltaRect!.left
          dy += event.deltaRect!.top
          target.style.transform = `translate(${dx}px, ${dy}px)`
          target.dataset.x = String(dx)
          target.dataset.y = String(dy)
          x.value = dx
          y.value = dy
          if (event.deltaRect!.left !== 0 || event.deltaRect!.top !== 0) {
            emit('update', {
              urgency: dx / props.canvasWidth,
              importance: dy / props.canvasHeight,
            })
          }
        },
      },
      modifiers: [
        Interact.modifiers.restrictEdges({ outer: 'parent' }),
        Interact.modifiers.restrictSize({ min: { width: 100, height: 50 } }),
      ],
      inertia: true,
    })
    .draggable({
      ignoreFrom: '.flag-badge',
      listeners: {
        move(event: InteractDragEvent) {
          const target = event.target as HTMLElement
          const nx = (parseFloat(target.dataset.x ?? '0') + event.dx)
          const ny = (parseFloat(target.dataset.y ?? '0') + event.dy)
          target.style.transform = `translate(${nx}px, ${ny}px)`
          target.dataset.x = String(nx)
          target.dataset.y = String(ny)
          x.value = nx
          y.value = ny
          emit('update', {
            urgency: nx / props.canvasWidth,
            importance: ny / props.canvasHeight,
          })
        },
      },
    })
})

onUnmounted(() => {
  interactInstance?.unset()
})

function onInput(event: Event) {
  emit('update', { text: (event.target as HTMLTextAreaElement).value })
}

function onFocus() {
  emit('focus', props.noteId)
}

function cycleFlag() {
  let nextFlag: NoteFlag = null
  if (!props.flag) {
    nextFlag = 'QUICK_WIN'
  } else if (props.flag === 'QUICK_WIN') {
    nextFlag = 'LONG_TERM'
  } else if (props.flag === 'LONG_TERM') {
    nextFlag = null
  }
  emit('update', { flag: nextFlag })
}

const flagAriaLabel = computed(() => {
  if (props.flag === 'QUICK_WIN') {
    return 'Flag: Quick Win (⚡). Activate to change to Long Term'
  }
  if (props.flag === 'LONG_TERM') {
    return 'Flag: Long Term (🐌). Activate to remove flag'
  }
  return 'Flag: None. Activate to set Quick Win'
})

const flagTitle = computed(() => {
  if (props.flag === 'QUICK_WIN') {
    return 'Quick Win (click to change to Long Term)'
  }
  if (props.flag === 'LONG_TERM') {
    return 'Long Term (click to remove flag)'
  }
  return 'Set Quick Win flag'
})
</script>

<template>
  <div
    ref="el"
    class="note-card"
    :data-x="x"
    :data-y="y"
    :data-id="noteId"
    :style="{
      transform: `translate(${x}px, ${y}px)`,
      backgroundColor: background,
      borderColor: border,
    }"
    @pointerdown="onFocus"
  >
    <textarea
      class="note-textarea"
      :value="text"
      @input="onInput"
      @focus="onFocus"
    />
    <button
      type="button"
      class="flag-badge"
      :class="{ 'has-flag': !!flag, 'is-empty': !flag }"
      :aria-label="flagAriaLabel"
      :title="flagTitle"
      @click.stop="cycleFlag"
      @pointerdown.stop
      @mousedown.stop
      @touchstart.stop
    >
      <span v-if="flag === 'QUICK_WIN'">⚡</span>
      <span v-else-if="flag === 'LONG_TERM'">🐌</span>
    </button>
  </div>
</template>

<style scoped>
.note-card {
  position: absolute;
  height: max(11%, 75px);
  width: max(15%, 150px);
  border-radius: 3px;
  border-width: 2px;
  border-style: solid;
  touch-action: none;
  box-sizing: border-box;
  display: flex;
}

.note-card:focus-within {
  outline: 2px solid rgba(0, 0, 0, 0.4);
  outline-offset: 1px;
}

.note-textarea {
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
  padding: 6px 28px 18px 12px;
  font-size: 16px;
  font-family: sans-serif;
  box-sizing: border-box;
  resize: none;
  outline: none;
  color: inherit;
}

.note-textarea:focus-visible {
  outline: 1px dashed rgba(0, 0, 0, 0.3);
}

.flag-badge {
  position: absolute;
  bottom: 4px;
  right: 6px;
  width: 24px;
  height: 24px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  line-height: 1;
  border-radius: 50%;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  z-index: 2;
  user-select: none;
  touch-action: manipulation;
  transition: opacity 0.2s, background-color 0.2s, border-color 0.2s;
}

.flag-badge.has-flag {
  opacity: 1;
  background: rgba(255, 255, 255, 0.7);
  border-color: rgba(0, 0, 0, 0.12);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.flag-badge.is-empty {
  opacity: 0.25;
  border: 1px dashed rgba(0, 0, 0, 0.2);
}

.note-card:hover .flag-badge.is-empty,
.note-card:focus-within .flag-badge.is-empty,
.flag-badge:focus,
.flag-badge:focus-visible {
  opacity: 0.85;
  background: rgba(255, 255, 255, 0.5);
  border-color: rgba(0, 0, 0, 0.35);
}

.flag-badge:hover {
  opacity: 1;
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(0, 0, 0, 0.4);
}

@media (hover: none), (pointer: coarse) {
  .flag-badge.is-empty {
    opacity: 0.5;
  }
}

@media (max-width: 600px) {
  .note-card {
    height: max(8%, 40px);
    width: max(15%, 90px);
  }

  .note-textarea {
    font-size: 11px;
    padding: 4px 20px 14px 4px;
  }

  .flag-badge {
    bottom: 2px;
    right: 2px;
    width: 18px;
    height: 18px;
    font-size: 10px;
  }

  .flag-badge.is-empty {
    opacity: 0.5;
  }
}
</style>
