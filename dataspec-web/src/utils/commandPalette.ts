import type { LocationQueryRaw } from 'vue-router'
import type { AiJobRecordListItem, ReverseImportDecision, SqlCheckRecord } from '@/types'
import { sanitizeQuery } from './urlState.ts'

const MAX_RECENT_COMMANDS = 8
const STORAGE_KEY = 'dataspec.commandPalette.recent.v1'

type CommandGroup = 'navigation' | 'action' | 'recent' | 'project'

export interface CommandRouteTarget {
  /** 前端路由路径，只保存 DataSpec 内部 route，不保存外部 URL。 */
  path: string
  /** 路由 query，写入前会移除 token、SQL、payload 等敏感字段。 */
  query?: LocationQueryRaw
}

export interface CommandPaletteItem {
  /** 命令稳定 ID，用于搜索结果去重和最近操作覆盖。 */
  id: string
  /** 用户可见命令标题。 */
  title: string
  /** 用户可见辅助说明，不包含 SQL 原文、token 或 payload。 */
  description?: string
  /** 命令分组，用于面板中保持信息密度和可扫描性。 */
  group: CommandGroup
  /** 业务关键词，帮助用户按别名搜索入口。 */
  keywords?: string[]
  /** 命令跳转目标。 */
  route: CommandRouteTarget
  /** 是否依赖当前项目。无项目时该类命令会禁用并提示先选项目。 */
  projectRequired?: boolean
  /** 是否禁用当前命令。 */
  disabled?: boolean
  /** 禁用原因，用于 UI 展示恢复路径。 */
  disabledReason?: string
  /** 最近操作发生时间，用于排序。 */
  usedAt?: string
}

export interface RecentCommandEntry {
  /** 最近操作稳定 ID，通常由业务类型和记录 ID 组成。 */
  id: string
  /** 最近操作标题。 */
  title: string
  /** 最近操作跳转目标。 */
  route: CommandRouteTarget
  /** 最近使用时间，ISO 字符串。 */
  usedAt: string
}

export interface CommandPaletteContext {
  /** 当前项目 ID；为空时只显示非项目入口和项目选择建议。 */
  projectId?: number | null
  /** 最近 SQL 检查记录。 */
  lintRecords?: SqlCheckRecord[]
  /** 最近反向导入决策或批次线索。 */
  reverseDecisions?: ReverseImportDecision[]
  /** 最近 AI 作业。 */
  aiJobs?: AiJobRecordListItem[]
  /** 本地记录的最近命令。 */
  localRecentEntries?: RecentCommandEntry[]
}

interface CommandStorage {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
}

export function commandPaletteStorageKey() {
  return STORAGE_KEY
}

export function buildCommandPaletteItems(context: CommandPaletteContext = {}): CommandPaletteItem[] {
  const projectId = normalizePositiveInt(context.projectId)
  const baseItems = [
    ...projectSuggestions(projectId),
    ...navigationCommands(projectId),
    ...recentLintCommands(projectId, context.lintRecords ?? []),
    ...recentReverseImportCommands(projectId, context.reverseDecisions ?? []),
    ...recentAiJobCommands(projectId, context.aiJobs ?? []),
    ...localRecentCommands(context.localRecentEntries ?? [])
  ]
  return dedupeCommands(baseItems)
}

export function commandMatchesKeyword(item: CommandPaletteItem, keyword: string) {
  const normalizedKeyword = normalizeSearchText(keyword)
  if (!normalizedKeyword) {
    return true
  }
  return [
    item.title,
    item.description,
    groupLabel(item.group),
    ...(item.keywords ?? [])
  ]
    .map(normalizeSearchText)
    .some((text) => text.includes(normalizedKeyword))
}

export function filterCommandPaletteItems(items: CommandPaletteItem[], keyword: string) {
  return items.filter((item) => commandMatchesKeyword(item, keyword))
}

export function groupLabel(group: CommandGroup) {
  const labels: Record<CommandGroup, string> = {
    navigation: '页面入口',
    action: '常用动作',
    recent: '最近操作',
    project: '项目建议'
  }
  return labels[group]
}

export function readRecentCommandEntries(storage: CommandStorage | Storage | null | undefined = getDefaultStorage()): RecentCommandEntry[] {
  if (!storage) {
    return []
  }
  try {
    return normalizeRecentCommandEntries(JSON.parse(storage.getItem(STORAGE_KEY) || '[]'))
  } catch {
    return []
  }
}

export function writeRecentCommandEntry(
  storage: CommandStorage | Storage | null | undefined = getDefaultStorage(),
  entry: RecentCommandEntry
) {
  const entries = normalizeRecentCommandEntries([entry, ...readRecentCommandEntries(storage)])
  try {
    storage?.setItem(STORAGE_KEY, JSON.stringify(entries))
  } catch {
    // 浏览器隐私模式、禁用存储或配额异常时，命令跳转仍应继续工作。
  }
  return entries
}

export function normalizeRecentCommandEntries(value: unknown): RecentCommandEntry[] {
  if (!Array.isArray(value)) {
    return []
  }
  const deduped = new Map<string, RecentCommandEntry>()
  for (const item of value) {
    const normalized = normalizeRecentCommandEntry(item)
    if (normalized && !deduped.has(normalized.id)) {
      deduped.set(normalized.id, normalized)
    }
  }
  return Array.from(deduped.values())
    .sort((left, right) => right.usedAt.localeCompare(left.usedAt))
    .slice(0, MAX_RECENT_COMMANDS)
}

export function commandToRecentEntry(item: CommandPaletteItem, usedAt = new Date().toISOString()): RecentCommandEntry {
  return {
    id: item.id,
    title: item.title,
    route: normalizeRoute(item.route),
    usedAt
  }
}

export function commandToLocalRecentEntry(item: CommandPaletteItem, usedAt = new Date().toISOString()): RecentCommandEntry {
  const entry = commandToRecentEntry(item, usedAt)
  return {
    ...entry,
    id: entry.id.startsWith('local.') ? entry.id : `local.${entry.id}`
  }
}

function projectSuggestions(projectId: number | null): CommandPaletteItem[] {
  if (projectId) {
    return []
  }
  return [
    {
      id: 'project.select',
      title: '选择项目',
      description: '先进入项目列表，再继续项目内操作',
      group: 'project',
      keywords: ['project', '项目'],
      route: { path: '/projects' }
    },
    {
      id: 'project.create-demo',
      title: '创建演示项目',
      description: '打开项目列表并使用演示项目入口',
      group: 'project',
      keywords: ['demo', '演示', '初始化'],
      route: { path: '/projects', query: { action: 'demo' } }
    }
  ]
}

function navigationCommands(projectId: number | null): CommandPaletteItem[] {
  const projectQuery = projectId ? { projectId } : undefined
  const requiresProject = (item: Omit<CommandPaletteItem, 'projectRequired' | 'disabled' | 'disabledReason'>): CommandPaletteItem => ({
    ...item,
    projectRequired: true,
    disabled: !projectId,
    disabledReason: projectId ? undefined : '请先选择或创建项目'
  })
  return [
    { id: 'page.dashboard', title: '工作台', description: '查看项目概览和任务入口', group: 'navigation', keywords: ['dashboard'], route: { path: '/dashboard', query: projectQuery } },
    requiresProject({ id: 'page.sql-lint', title: 'SQL 校验', description: '校验 SQL 并查看历史检查记录', group: 'navigation', keywords: ['lint', 'sql', '校验'], route: { path: '/sql-lint', query: projectQuery } }),
    requiresProject({ id: 'page.reverse-import', title: '反向导入', description: '从 SQL 或数据库 metadata 生成标准候选', group: 'navigation', keywords: ['reverse', 'database', 'metadata', '导入'], route: { path: '/reverse-import', query: projectQuery } }),
    requiresProject({ id: 'page.field-quality', title: '字段质量', description: '查看低质量字段和修复建议', group: 'navigation', keywords: ['quality', '字段质量'], route: { path: '/field-quality', query: projectQuery } }),
    requiresProject({ id: 'page.field-coverage', title: '覆盖率报告', description: '恢复字段覆盖率报告入口', group: 'navigation', keywords: ['coverage', '覆盖率'], route: { path: '/field-coverage', query: projectQuery } }),
    requiresProject({ id: 'page.ai-export', title: 'AI Context', description: '导出当前项目 AI 上下文', group: 'action', keywords: ['context', 'ai', '导出'], route: { path: '/ai-export', query: projectQuery } }),
    requiresProject({ id: 'page.ai-replay', title: 'AI 回放', description: '查看 AI 作业和重放上下文', group: 'navigation', keywords: ['replay', '回放'], route: { path: '/ai-replay', query: projectQuery } }),
    requiresProject({ id: 'page.ai-handoff', title: 'AI 交接证据', description: '聚合任务交接记录和证据包', group: 'navigation', keywords: ['handoff', 'evidence', '交接', '证据'], route: { path: '/ai-handoff', query: projectQuery } }),
    { id: 'page.tokens', title: 'API Token', description: '管理本地 API Token', group: 'navigation', keywords: ['token', 'auth'], route: { path: '/tokens' } }
  ]
}

function recentLintCommands(projectId: number | null, records: SqlCheckRecord[]): CommandPaletteItem[] {
  if (!projectId) {
    return []
  }
  return records
    .filter((record) => normalizePositiveInt(record.id) && matchesProjectId(record.projectId, projectId))
    .slice(0, 5)
    .map((record) => ({
      id: `recent.sql.${record.id}`,
      title: `继续 SQL 检查 #${record.id}`,
      description: formatRecentDescription('SQL 校验记录', record.createdAt),
      group: 'recent',
      keywords: ['sql', 'lint', 'record', '续跑'],
      route: { path: '/sql-lint', query: { projectId, recordId: record.id } },
      usedAt: record.createdAt
    }))
}

function recentReverseImportCommands(projectId: number | null, decisions: ReverseImportDecision[]): CommandPaletteItem[] {
  if (!projectId) {
    return []
  }
  const batchIds = uniquePositiveInts(decisions
    .filter((item) => matchesProjectId(item.projectId, projectId))
    .map((item) => item.batchId))
  return batchIds.slice(0, 5).map((batchId) => ({
    id: `recent.reverse.${batchId}`,
    title: `继续反向导入批次 #${batchId}`,
    description: '恢复批次筛选和字段映射决策',
    group: 'recent',
    keywords: ['reverse', 'import', 'batch', '导入批次'],
    route: { path: '/reverse-import', query: { projectId, sourceBatchId: batchId } }
  }))
}

function recentAiJobCommands(projectId: number | null, jobs: AiJobRecordListItem[]): CommandPaletteItem[] {
  if (!projectId) {
    return []
  }
  return jobs
    .filter((job) => normalizePositiveInt(job.id) && matchesProjectId(job.projectId, projectId))
    .slice(0, 5)
    .map((job) => ({
      id: `recent.ai.${job.id}`,
      title: `继续 AI 作业 #${job.id}`,
      description: formatRecentDescription(formatJobType(job.jobType), job.createdAt),
      group: 'recent',
      keywords: ['ai', 'replay', 'job', job.jobType ?? ''],
      route: { path: '/ai-replay', query: { projectId, aiJobId: job.id, jobType: job.jobType || undefined } },
      usedAt: job.createdAt
    }))
}

function localRecentCommands(entries: RecentCommandEntry[]): CommandPaletteItem[] {
  return normalizeRecentCommandEntries(entries).map((entry) => ({
    id: entry.id,
    title: entry.title,
    description: formatRecentDescription('本地最近操作', entry.usedAt),
    group: 'recent',
    keywords: ['recent', 'history', '最近'],
    route: entry.route,
    usedAt: entry.usedAt
  }))
}

function normalizeRecentCommandEntry(value: unknown): RecentCommandEntry | null {
  if (!value || typeof value !== 'object') {
    return null
  }
  const candidate = value as Partial<RecentCommandEntry>
  const id = normalizeCommandText(candidate.id)
  const title = normalizeCommandText(candidate.title)
  const route = normalizeRoute(candidate.route)
  const usedAt = normalizeCommandText(candidate.usedAt) ?? new Date(0).toISOString()
  if (!id || !title || !route.path) {
    return null
  }
  return { id, title, route, usedAt }
}

function getDefaultStorage(): Storage | null {
  try {
    return globalThis.localStorage ?? null
  } catch {
    return null
  }
}

function normalizeRoute(route?: Partial<CommandRouteTarget>): CommandRouteTarget {
  const path = normalizePath(route?.path)
  const query = sanitizeQuery(route?.query ?? {})
  return {
    path,
    query: Object.keys(query).length > 0 ? query : undefined
  }
}

function normalizePath(path?: string) {
  return typeof path === 'string' && path.startsWith('/') ? path : '/dashboard'
}

function dedupeCommands(items: CommandPaletteItem[]) {
  const seen = new Set<string>()
  return items.filter((item) => {
    if (seen.has(item.id)) {
      return false
    }
    seen.add(item.id)
    return true
  })
}

function uniquePositiveInts(values: Array<number | null | undefined>) {
  const seen = new Set<number>()
  for (const value of values) {
    const normalized = normalizePositiveInt(value)
    if (normalized) {
      seen.add(normalized)
    }
  }
  return Array.from(seen)
}

function normalizePositiveInt(value: unknown): number | null {
  return typeof value === 'number' && Number.isSafeInteger(value) && value > 0 ? value : null
}

function matchesProjectId(value: unknown, projectId: number) {
  const normalized = normalizePositiveInt(value)
  return normalized === null || normalized === projectId
}

function normalizeSearchText(value?: string) {
  return (value ?? '').trim().toLowerCase()
}

function normalizeCommandText(value?: string) {
  const text = value?.trim()
  return text ? text.slice(0, 80) : undefined
}

function formatRecentDescription(prefix: string, createdAt?: string) {
  return createdAt ? `${prefix} · ${createdAt}` : prefix
}

function formatJobType(jobType?: string) {
  if (!jobType) {
    return 'AI 作业'
  }
  return jobType.replace(/_/g, ' ')
}
