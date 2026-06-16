/** 项目 */
export interface Project {
  id: string
  name: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

/** 标准字段 */
export interface Field {
  id: string
  projectId: string
  fieldName: string
  fieldType: string
  fieldLength?: number
  fieldScale?: number
  comment?: string
  nullable?: boolean
  defaultValue?: string
}

/** 数据域 */
export interface Domain {
  id: string
  projectId: string
  domainCode: string
  domainName: string
  description?: string
}

/** 枚举字典 */
export interface EnumDict {
  id: string
  projectId: string
  enumCode: string
  enumName: string
  description?: string
  values: EnumValue[]
}

/** 枚举值 */
export interface EnumValue {
  id: string
  enumId: string
  valueCode: string
  valueName: string
  sort?: number
}

/** 表模板 */
export interface Template {
  id: string
  projectId: string
  templateCode: string
  templateName: string
  description?: string
  fields: TemplateField[]
}

/** 模板字段 */
export interface TemplateField {
  id: string
  templateId: string
  fieldName: string
  fieldType: string
  fieldLength?: number
  fieldScale?: number
  comment?: string
  nullable?: boolean
  defaultValue?: string
  sort?: number
}

/** 规则配置 */
export interface RuleConfig {
  id: string
  projectId: string
  ruleCode: string
  ruleName: string
  ruleType: string
  ruleExpression: string
  description?: string
  enabled?: boolean
}

/** SQL 校验结果 */
export interface LintResult {
  totalIssues: number
  issues: LintIssue[]
}

/** SQL 校验问题条目 */
export interface LintIssue {
  line: number
  column: number
  severity: 'error' | 'warning' | 'info'
  ruleCode: string
  message: string
}

/** 通用分页响应 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 通用 API 响应包装 */
export interface R<T> {
  code: number
  message: string
  data: T
}
