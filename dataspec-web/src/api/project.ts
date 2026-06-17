import request from '@/api/request'
import type { CreateProjectReq, Project } from '@/types'

export function listProjects() {
  return request.get<unknown, Project[]>('/projects')
}

export function getProject(id: number) {
  return request.get<unknown, Project>(`/projects/${id}`)
}

export function createProject(data: CreateProjectReq) {
  return request.post<unknown, Project>('/projects', data)
}

export function updateProject(id: number, data: CreateProjectReq) {
  return request.put<unknown, Project>(`/projects/${id}`, data)
}

export function deleteProject(id: number) {
  return request.delete<unknown, void>(`/projects/${id}`)
}
