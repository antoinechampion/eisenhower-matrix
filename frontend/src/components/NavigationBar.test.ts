import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import NavigationBar from './NavigationBar.vue'

describe('NavigationBar', () => {
  it('renders the app title', () => {
    const wrapper = mount(NavigationBar)
    expect(wrapper.find('.nav__brand strong').text()).toBe('Eisenhower Matrix')
  })

  it('renders Home link', () => {
    const wrapper = mount(NavigationBar)
    const links = wrapper.findAll('a')
    expect(links.some((a) => a.text() === 'Home')).toBe(true)
  })

  it('renders FAQ link', () => {
    const wrapper = mount(NavigationBar)
    const links = wrapper.findAll('a')
    expect(links.some((a) => a.text() === 'FAQ')).toBe(true)
  })
})
