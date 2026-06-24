import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '工作台' }
  },
  {
    path: '/projects',
    name: 'ProjectList',
    component: () => import('@/views/ProjectList.vue'),
    meta: { title: '项目列表' }
  },
  {
    path: '/fields',
    name: 'FieldLibrary',
    component: () => import('@/views/FieldLibrary.vue'),
    meta: { title: '标准字段库' }
  },
  {
    path: '/domains',
    name: 'DomainManage',
    component: () => import('@/views/DomainManage.vue'),
    meta: { title: '数据域管理' }
  },
  {
    path: '/enums',
    name: 'EnumDict',
    component: () => import('@/views/EnumDict.vue'),
    meta: { title: '枚举字典' }
  },
  {
    path: '/templates',
    name: 'TemplateManage',
    component: () => import('@/views/TemplateManage.vue'),
    meta: { title: '表模板' }
  },
  {
    path: '/rules',
    name: 'RuleConfig',
    component: () => import('@/views/RuleConfig.vue'),
    meta: { title: '规则配置' }
  },
  {
    path: '/sql-lint',
    name: 'SqlLint',
    component: () => import('@/views/SqlLint.vue'),
    meta: { title: 'SQL 校验' }
  },
  {
    path: '/generator',
    name: 'Generator',
    component: () => import('@/views/Generator.vue'),
    meta: { title: '生成器' }
  },
  {
    path: '/ai-export',
    name: 'AiExport',
    component: () => import('@/views/AiExport.vue'),
    meta: { title: 'AI 规则导出' }
  },
  {
    path: '/import-export',
    name: 'ImportExport',
    component: () => import('@/views/ImportExport.vue'),
    meta: { title: '导入导出' }
  },
  {
    path: '/reverse-import',
    name: 'ReverseImport',
    component: () => import('@/views/ReverseImport.vue'),
    meta: { title: '反向导入' }
  },
  {
    path: '/tokens',
    name: 'TokenManage',
    component: () => import('@/views/TokenManage.vue'),
    meta: { title: 'API Token 管理' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
