import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 当前选中项目的状态 */
export const useProjectStore = defineStore('project', () => {
  /** 当前项目 ID */
  const currentProjectId = ref<string>('')
  /** 当前项目名称（用于顶部下拉框显示） */
  const currentProjectName = ref<string>('')

  /** 切换当前项目 */
  function setCurrentProject(id: string, name: string) {
    currentProjectId.value = id
    currentProjectName.value = name
  }

  return {
    currentProjectId,
    currentProjectName,
    setCurrentProject
  }
})
