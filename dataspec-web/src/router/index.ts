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
    path: '/standard-candidates',
    name: 'StandardCandidate',
    component: () => import('@/views/StandardCandidate.vue'),
    meta: { title: '标准候选' }
  },
  {
    path: '/field-quality',
    name: 'FieldQuality',
    component: () => import('@/views/FieldQuality.vue'),
    meta: { title: '字段质量' }
  },
  {
    path: '/field-conflicts',
    name: 'FieldConflicts',
    component: () => import('@/views/FieldConflicts.vue'),
    meta: { title: '字段冲突' }
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
    path: '/rule-exemptions',
    name: 'RuleExemptions',
    component: () => import('@/views/RuleExemptions.vue'),
    meta: { title: '规则例外' }
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
    path: '/ai-replay',
    name: 'AiReplay',
    component: () => import('@/views/AiReplay.vue'),
    meta: { title: 'AI 回放' }
  },
  {
    path: '/ai-batches',
    name: 'AiBatch',
    component: () => import('@/views/AiBatch.vue'),
    meta: { title: 'AI 批量任务' }
  },
  {
    path: '/ai-feedback',
    name: 'AiFeedback',
    component: () => import('@/views/AiFeedback.vue'),
    meta: { title: 'AI 反馈' }
  },
  {
    path: '/import-export',
    name: 'ImportExport',
    component: () => import('@/views/ImportExport.vue'),
    meta: { title: '导入导出' }
  },
  {
    path: '/project-backup',
    name: 'ProjectBackup',
    component: () => import('@/views/ProjectBackup.vue'),
    meta: { title: '项目备份' }
  },
  {
    path: '/reverse-import',
    name: 'ReverseImport',
    component: () => import('@/views/ReverseImport.vue'),
    meta: { title: '反向导入' }
  },
  {
    path: '/field-coverage',
    name: 'FieldCoverage',
    component: () => import('@/views/FieldCoverage.vue'),
    meta: { title: '覆盖率报告' }
  },
  {
    path: '/tokens',
    name: 'TokenManage',
    component: () => import('@/views/TokenManage.vue'),
    meta: { title: 'API Token 管理' }
  },
  {
    path: '/standard-snapshots',
    name: 'StandardSnapshot',
    component: () => import('@/views/StandardSnapshot.vue'),
    meta: { title: '标准快照' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
