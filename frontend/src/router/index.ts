import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import MatrixView from '@/views/MatrixView.vue'
import EditorView from '@/views/EditorView.vue'
import FaqView from '@/views/FaqView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/matrix', component: MatrixView },
    { path: '/editor/:noteId', component: EditorView },
    { path: '/faq', component: FaqView },
  ],
})
