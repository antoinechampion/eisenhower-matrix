<script setup lang="ts">
import { onMounted, watch, ref } from 'vue'
import { useSettingsStore } from '@/stores/settings'

const settings = useSettingsStore()
const canvas = ref<HTMLCanvasElement | null>(null)

function draw() {
  const c = canvas.value
  if (!c) return
  c.width = window.innerWidth
  c.height = window.innerHeight
  const ctx = c.getContext('2d')!
  const palette = settings.activePalette
  const w = c.width
  const h = c.height

  // Quadrants
  const colors = [palette.quadrantColors.q1, palette.quadrantColors.q2, palette.quadrantColors.q3, palette.quadrantColors.q4]
  const positions = [
    { x: 0, y: 0 },
    { x: w / 2, y: 0 },
    { x: 0, y: h / 2 },
    { x: w / 2, y: h / 2 },
  ]
  for (let i = 0; i < 4; i++) {
    ctx.fillStyle = colors[i]!
    ctx.fillRect(positions[i]!.x, positions[i]!.y, w / 2, h / 2)
  }

  // Arrows
  const stroke = 2
  ctx.strokeStyle = palette.arrowsColor
  if (settings.flipMatrix) {
    drawArrow(ctx, 0, w - stroke, h / 2, h / 2, stroke)
  } else {
    drawArrow(ctx, w - stroke, 0, h / 2, h / 2, stroke)
  }
  drawArrow(ctx, w / 2, w / 2, h, stroke, stroke)

  // Text
  const fontSize = Math.max(12, Math.min(24, Math.floor(w / 50)))
  const lineH = fontSize + 8
  ctx.font = `${fontSize}px 'Arial Black'`
  ctx.fillStyle = palette.textColor
  const quadrantTexts = settings.flipMatrix
    ? [['IMPORTANT', 'NOT URGENT'], ['IMPORTANT', 'URGENT'], ['NOT IMPORTANT', 'NOT URGENT'], ['NOT IMPORTANT', 'URGENT']]
    : [['IMPORTANT', 'URGENT'], ['IMPORTANT', 'NOT URGENT'], ['NOT IMPORTANT', 'URGENT'], ['NOT IMPORTANT', 'NOT URGENT']]
  // Left column labels are left-aligned; right column labels are right-aligned so
  // longer labels ("NOT IMPORTANT") stay inside the canvas edge.
  const margin = 20
  const textAligns: CanvasTextAlign[] = ['left', 'right', 'left', 'right']
  const textPositions = [
    { x: margin, y: fontSize + margin },
    { x: w - margin, y: fontSize + margin },
    { x: margin, y: h - lineH - margin },
    { x: w - margin, y: h - lineH - margin },
  ]
  for (let i = 0; i < 4; i++) {
    ctx.textAlign = textAligns[i]!
    ctx.fillText(quadrantTexts[i]![0]!, textPositions[i]!.x, textPositions[i]!.y)
    ctx.fillText(quadrantTexts[i]![1]!, textPositions[i]!.x, textPositions[i]!.y + lineH)
  }
  ctx.textAlign = 'left'
}

function drawArrow(ctx: CanvasRenderingContext2D, fromx: number, tox: number, fromy: number, toy: number, arrowWidth: number) {
  const headlen = 10
  const angle = Math.atan2(toy - fromy, tox - fromx)
  ctx.beginPath()
  ctx.moveTo(fromx, fromy)
  ctx.lineTo(tox, toy)
  ctx.lineWidth = arrowWidth
  ctx.stroke()
  ctx.beginPath()
  ctx.moveTo(tox, toy)
  ctx.lineTo(tox - headlen * Math.cos(angle - Math.PI / 7), toy - headlen * Math.sin(angle - Math.PI / 7))
  ctx.lineTo(tox - headlen * Math.cos(angle + Math.PI / 7), toy - headlen * Math.sin(angle + Math.PI / 7))
  ctx.lineTo(tox, toy)
  ctx.stroke()
  ctx.restore()
}

onMounted(() => {
  draw()
  window.addEventListener('resize', draw)
})

watch(() => [settings.palette, settings.flipMatrix], draw)

defineExpose({ canvas })
</script>

<template>
  <canvas ref="canvas" class="background-canvas" />
</template>

<style scoped>
.background-canvas {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 0;
}
</style>
