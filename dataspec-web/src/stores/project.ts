import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { createDemoProject as createDemoProjectApi, listProjects } from '@/api/project'
import type { DemoProjectResult, Project } from '@/types'

/** 当前选中项目的状态 */
export const useProjectStore = defineStore('project', () => {
  /** 可选项目列表 */
  const projects = ref<Project[]>([])
  /** 当前项目 ID */
  const currentProjectId = ref<number | null>(null)
  /** 当前项目名称（用于顶部下拉框显示） */
  const currentProjectName = ref('')
  /** 项目列表加载状态 */
  const loading = ref(false)

  const currentProject = computed(() =>
    projects.value.find((project) => project.id === currentProjectId.value) ?? null
  )

  /** 加载项目列表，并在未选中时默认选中第一项 */
  async function loadProjects() {
    loading.value = true
    try {
      projects.value = await listProjects()
      if (currentProjectId.value) {
        const selected = projects.value.find((project) => project.id === currentProjectId.value)
        if (selected) {
          setCurrentProject(selected)
          return
        }
      }
      if (projects.value.length > 0) {
        setCurrentProject(projects.value[0])
      } else {
        clearCurrentProject()
      }
    } finally {
      loading.value = false
    }
  }

  /** 创建或复用演示项目，并把它设为当前项目 */
  async function createDemoProjectAndSelect(): Promise<DemoProjectResult> {
    loading.value = true
    try {
      const result = await createDemoProjectApi()
      projects.value = await listProjects()
      const project = projects.value.find((item) => item.id === result.project?.id) ?? result.project ?? null
      setCurrentProject(project)
      return result
    } finally {
      loading.value = false
    }
  }

  /** 切换当前项目 */
  function setCurrentProject(project: Project | null) {
    currentProjectId.value = project?.id ?? null
    currentProjectName.value = project?.name ?? ''
  }

  function setCurrentProjectById(projectId: number | null) {
    if (!projectId) {
      clearCurrentProject()
      return
    }
    setCurrentProject(projects.value.find((project) => project.id === projectId) ?? null)
  }

  function clearCurrentProject() {
    currentProjectId.value = null
    currentProjectName.value = ''
  }

  return {
    projects,
    currentProjectId,
    currentProjectName,
    currentProject,
    loading,
    loadProjects,
    createDemoProjectAndSelect,
    setCurrentProject,
    setCurrentProjectById,
    clearCurrentProject
  }
})
