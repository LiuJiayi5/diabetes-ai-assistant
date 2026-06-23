import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    title: '糖尿病预治智能助手'
  })
})
