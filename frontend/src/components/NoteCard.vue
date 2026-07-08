<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import type { ResizeEvent, DragEvent as InteractDragEvent } from '@interactjs/types'

export interface NoteCardProps {
  noteId: string
  text: string
  urgency: number
  importance: number
  background: string
  border: string
  canvasWidth: number
  canvasHeight: number
}

const props = defineProps<NoteCardProps>()
const emit = defineEmits<{
  update: [patch: { text?: string; urgency?: number; importance?: number }]
  focus: [noteId: string]
}>()

const el = ref<HTMLTextAreaElement | null>(null)
let interactInstance: ReturnType<typeof import('interactjs')['default']> | null = null

const x = ref(props.urgency * props.canvasWidth)
const y = ref(props.importance * props.canvasHeight)

onMounted(async () => {
  const Interact = (await import('interactjs')).default
  if (!el.value) return

  interactInstance = Interact(el.value)
    .resizable({
      edges: { left: true, right: true, bottom: true, top: true },
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
        },
      },
      modifiers: [
        Interact.modifiers.restrictEdges({ outer: 'parent' }),
        Interact.modifiers.restrictSize({ min: { width: 100, height: 50 } }),
      ],
      inertia: true,
    })
    .draggable({
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
</script>

<template>
  <textarea
    ref="el"
    class="note"
    :value="text"
    :data-x="x"
    :data-y="y"
    :data-id="noteId"
    :style="{
      transform: `translate(${x}px, ${y}px)`,
      backgroundColor: background,
      borderColor: border,
    }"
    @input="onInput"
    @focus="onFocus"
    @pointerdown="onFocus"
  />
</template>

<style scoped>
.note {
  position: absolute;
  height: max(11%, 75px);
  width: max(15%, 150px);
  border-radius: 3px;
  border-width: 2px;
  border-style: solid;
  padding: 6px 12px;
  font-size: 16px;
  font-family: sans-serif;
  touch-action: none;
  box-sizing: border-box;
  resize: none;
}

@media (max-width: 600px) {
  .note {
    height: max(8%, 40px);
    width: max(15%, 90px);
    font-size: 11px;
    padding: 4px 4px;
  }
}
</style>
