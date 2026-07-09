export const WORKFLOW_RECIPES = [
  {
    id: 'create-table',
    title: '新增建表 SQL',
    goal: '为一个新业务表先读取 DataSpec 标准，再生成或整理符合规则的 CREATE TABLE SQL。',
    requiredInputs: [
      { name: 'projectId', description: 'DataSpec 项目 ID。', required: true },
      { name: 'businessDescription', description: '业务表或数据对象描述。', required: true },
      { name: 'tableName', description: '目标 snake_case 表名，未知时先由 AI 给出候选。', required: false },
      { name: 'templateId', description: '可选表模板 ID。', required: false }
    ],
    prechecks: [
      {
        title: '确认 DataSpec 服务、项目和 token 可用',
        command: 'node tools/dataspec-cli.mjs doctor --project <projectId> --format json',
        expected: '所有关键检查为 pass；warn 项需要在交付说明中记录。'
      }
    ],
    steps: [
      {
        order: 1,
        title: '导出当前任务的最小 AI Context',
        command: 'node tools/dataspec-cli.mjs export-context --project <projectId> --scope field --query "<businessDescription>" --output dataspec-ai-context.zip',
        purpose: '让 AI 先读取字段目录、规则、prompt 和示例，减少无关字段占用。',
        output: 'dataspec-ai-context.zip'
      },
      {
        order: 2,
        title: '按业务描述推荐标准字段',
        command: 'node tools/dataspec-cli.mjs suggest-field "<businessDescription>" --project <projectId> --format json',
        purpose: '优先复用已有标准字段，识别可能缺失的候选字段。',
        output: '字段推荐 JSON，包含分数、原因和 existing 标记。'
      },
      {
        order: 3,
        title: '基于表模板生成候选 DDL',
        command: 'node tools/dataspec-cli.mjs generate-ddl --project <projectId> --template <templateId> --table <tableName> --format json',
        purpose: '复用模板字段和规则生成第一版 SQL；无模板时由 AI 根据 Context 手写 SQL。',
        output: 'DDL 预览和 lintResult。'
      },
      {
        order: 4,
        title: '校验最终 SQL',
        command: 'node tools/dataspec-cli.mjs lint <sql-file|-> --project <projectId> --format json',
        purpose: '交付前确认命名、注释、必含列和类型规则。',
        output: 'LintResult；ERROR 需要修复后再交付。'
      }
    ],
    expectedArtifacts: [
      '符合 DataSpec 规则的 CREATE TABLE SQL',
      'lint JSON 结果或 SQL 校验记录',
      '使用的标准版本、字段推荐摘要和未采纳原因'
    ],
    failureHandling: [
      {
        condition: 'doctor 返回 server/auth/project 失败',
        nextAction: '先修复服务地址、DATASPEC_TOKEN 或 projectId，再重新开始 recipe。'
      },
      {
        condition: '字段推荐没有命中标准字段',
        nextAction: '标记 missingCandidates，先走标准候选或人工确认，不要直接创造不可追溯字段。'
      },
      {
        condition: 'lint 存在 ERROR',
        nextAction: '按 LintIssue.suggestion/replacement 修复 SQL，再重新运行 lint。'
      }
    ],
    nextActions: [
      '把最终 SQL 和 lint 结果写入任务交付说明。',
      '若新增了候选字段，进入标准候选或反向导入确认流程。',
      '需要复现时保存 dataspec-ai-context.zip 或记录 standard.specVersion/specHash。'
    ],
    sideEffectPolicy: 'plan-only'
  },
  {
    id: 'review-pr-sql',
    title: 'PR SQL Review',
    goal: '在 Pull Request 中扫描 SQL/DDL 变更，输出 DataSpec 校验结果和可恢复建议。',
    requiredInputs: [
      { name: 'projectId', description: 'DataSpec 项目 ID。', required: true },
      { name: 'paths', description: 'SQL 文件或目录，未提供时可使用 .dataspec/config.json 的 defaultPaths。', required: false },
      { name: 'repo', description: 'GitHub 仓库 owner/name。', required: true },
      { name: 'pr', description: 'Pull Request 编号。', required: true },
      { name: 'GITHUB_TOKEN', description: '具有 PR 评论权限的 GitHub token。', required: true }
    ],
    prechecks: [
      {
        title: '确认 DataSpec 与业务仓库配置可用',
        command: 'node tools/dataspec-cli.mjs doctor --project <projectId> --format json',
        expected: 'server/project/auth/defaultPaths 检查通过或有明确 warn。'
      },
      {
        title: '确认 GitHub token 已在环境变量中提供',
        command: 'node -e "process.exit(process.env.GITHUB_TOKEN ? 0 : 1)"',
        expected: '命令只用退出码确认变量存在，不把 token 写入日志。'
      }
    ],
    steps: [
      {
        order: 1,
        title: '批量 lint SQL 文件',
        command: 'node tools/dataspec-cli.mjs lint-files <paths...> --project <projectId> --format json',
        purpose: '先得到机器可读的文件级问题列表。',
        output: 'summary 和 files[] lint 结果。'
      },
      {
        order: 2,
        title: '发布或更新 PR 汇总评论',
        command: 'node tools/dataspec-cli.mjs review-pr <paths...> --project <projectId> --repo <owner/name> --pr <number> --token "$GITHUB_TOKEN"',
        purpose: '把 SQL Review 结果反馈到 PR，并用 marker 防止重复刷屏。',
        output: 'GitHub PR 评论和 CLI 退出码。'
      },
      {
        order: 3,
        title: '修复并复跑',
        command: 'node tools/dataspec-cli.mjs lint-files <paths...> --project <projectId> --format json',
        purpose: '修复后确认 ERROR 清零，WARNING/SUGGESTION 在交付说明中处理或解释。',
        output: '最终 lint JSON。'
      }
    ],
    expectedArtifacts: [
      'PR DataSpec SQL Review 评论',
      '最终 lint-files JSON 摘要',
      '未修复 WARNING/SUGGESTION 的说明'
    ],
    failureHandling: [
      {
        condition: 'review-pr 返回 GitHub 401/403',
        nextAction: '检查 GITHUB_TOKEN 权限、repo/pr 参数和 GitHub API 地址。'
      },
      {
        condition: '没有扫描到 SQL 文件',
        nextAction: '检查 paths 或 .dataspec/config.json 的 defaultPaths。'
      },
      {
        condition: '问题定位不到 diff 行',
        nextAction: '第一版保留汇总评论；inline comment 留给后续 P6-13。'
      }
    ],
    nextActions: [
      '把 lint-files 摘要放入 PR 或任务交付记录。',
      'ERROR 未清零时不要宣称 SQL Review 已通过。',
      '若规则误报，记录候选 rule exemption 并说明原因。'
    ],
    sideEffectPolicy: 'plan-only'
  },
  {
    id: 'reverse-import-standards',
    title: '数据库反向导入补标准',
    goal: '从已有数据库只读抽取 metadata，生成字段标准候选并由用户确认导入。',
    requiredInputs: [
      { name: 'projectId', description: 'DataSpec 项目 ID。', required: true },
      { name: 'databaseType', description: 'postgresql 或 mysql。', required: true },
      { name: 'host/port/databaseName/schemaName', description: '数据库连接元数据。', required: true },
      { name: 'username/password', description: '当次连接凭据，不应保存到 recipe 或提交文件。', required: true },
      { name: 'tableNames', description: '需要扫描的表名列表。', required: false }
    ],
    prechecks: [
      {
        title: '确认 DataSpec 服务和项目可用',
        command: 'node tools/dataspec-cli.mjs doctor --project <projectId> --format json',
        expected: 'DataSpec 服务和项目检查通过。'
      },
      {
        title: '确认使用只读数据库账号',
        command: '在数据库侧确认账号仅有 metadata 读取权限',
        expected: '账号不具备 DDL/DML 写入权限。'
      }
    ],
    steps: [
      {
        order: 1,
        title: '测试数据库连接',
        command: 'POST /api/reverse-import/database/test',
        purpose: '验证连接、schema 和基础权限，凭据只在请求体中临时使用。',
        output: 'DatabaseConnectionResult。'
      },
      {
        order: 2,
        title: '加载表并选择扫描范围',
        command: 'POST /api/reverse-import/database/tables',
        purpose: '先缩小表范围，避免一次性处理全库。',
        output: '可选表列表。'
      },
      {
        order: 3,
        title: '生成 metadata 预览和标准候选',
        command: 'POST /api/reverse-import/database/preview',
        purpose: '生成解析表、字段候选、缺注释和非标准字段差异。',
        output: 'ReverseImportPreview。'
      },
      {
        order: 4,
        title: '确认导入选中的候选字段',
        command: 'POST /api/reverse-import/database/import',
        purpose: '只写入 DataSpec 标准库和来源批次，不修改源数据库。',
        output: '导入批次、成功/跳过数量和字段来源摘要。'
      }
    ],
    expectedArtifacts: [
      '反向导入预览结果',
      '确认导入的标准字段候选',
      '导入批次和字段来源记录'
    ],
    failureHandling: [
      {
        condition: '连接失败或权限不足',
        nextAction: '使用连接诊断信息修正 host/schema/账号权限，不要保存密码。'
      },
      {
        condition: '候选字段过多',
        nextAction: '按 schema/table 分批处理，优先核心业务表。'
      },
      {
        condition: '字段与现有标准冲突',
        nextAction: '先查看字段冲突报告或人工确认别名，不要直接导入重复标准。'
      }
    ],
    nextActions: [
      '导入后运行覆盖率报告查看未纳管字段变化。',
      '把导入批次和未采纳字段写入任务交接说明。',
      '需要复用连接时只保存非敏感连接预设。'
    ],
    sideEffectPolicy: 'plan-only'
  },
  {
    id: 'export-min-context',
    title: '导出最小 AI Context',
    goal: '为当前建表、修 SQL 或字段设计任务导出尽量小但可复现的 DataSpec 上下文包。',
    requiredInputs: [
      { name: 'projectId', description: 'DataSpec 项目 ID。', required: true },
      { name: 'scope', description: 'all、field、domain、tag、table 或 changed。', required: true },
      { name: 'query', description: '当前任务关键词，例如模块名、表名、字段名或业务描述。', required: false },
      { name: 'limit', description: '返回字段上限。', required: false }
    ],
    prechecks: [
      {
        title: '确认 DataSpec 服务、项目和 OpenAPI 状态',
        command: 'node tools/dataspec-cli.mjs doctor --project <projectId> --format json',
        expected: 'server/project 检查通过；OpenAPI 漂移至少有明确诊断。'
      }
    ],
    steps: [
      {
        order: 1,
        title: '按任务范围导出 Context zip',
        command: 'node tools/dataspec-cli.mjs export-context --project <projectId> --scope <scope> --query "<query>" --limit <limit> --output dataspec-ai-context.zip',
        purpose: '生成当前任务所需字段、规则、prompt、示例和 workflow 文件。',
        output: 'dataspec-ai-context.zip'
      },
      {
        order: 2,
        title: '读取 manifest 和 README',
        command: 'unzip -p dataspec-ai-context.zip .dataspec/manifest.json && unzip -p dataspec-ai-context.zip .dataspec/README.md',
        purpose: '确认标准版本、裁剪条件、文件清单和推荐命令。',
        output: 'manifest JSON 和 .dataspec/README.md。'
      },
      {
        order: 3,
        title: '读取 workflow 和字段目录',
        command: 'unzip -p dataspec-ai-context.zip .dataspec/workflows.md && unzip -p dataspec-ai-context.zip .dataspec/field-catalog.json',
        purpose: '让 AI 先选择合适 recipe，再读取字段目录。',
        output: 'workflow guidance 和字段目录 JSON。'
      }
    ],
    expectedArtifacts: [
      'dataspec-ai-context.zip',
      'manifest 中的 contextScope 摘要',
      'AI 交付说明中的 standard.specVersion/specHash'
    ],
    failureHandling: [
      {
        condition: '导出包字段过少',
        nextAction: '扩大 scope 或放宽 query/limit 后重新导出。'
      },
      {
        condition: '导出包过大',
        nextAction: '收窄 scope/query/status，并优先读取 workflows.md 指定的必要文件。'
      },
      {
        condition: '标准版本为 unversioned',
        nextAction: '可继续使用，但交付时说明尚未创建标准快照。'
      }
    ],
    nextActions: [
      '把 contextScope 和标准版本写入任务说明。',
      '后续 SQL 变更完成后运行 lint 或 lint-files。',
      '若字段目录缺少关键字段，先补标准候选再生成 DDL。'
    ],
    sideEffectPolicy: 'plan-only'
  },
  {
    id: 'standard-evidence-review',
    title: '字段标准证据复核',
    goal: '在回答字段标准依据、可信度、最近使用情况或准备调整标准前，先读取跨来源标准证据并形成可复现结论。',
    requiredInputs: [
      { name: 'projectId', description: 'DataSpec 项目 ID。', required: true },
      { name: 'subjectType', description: '证据主体类型，第一版通常为 FIELD。', required: true },
      { name: 'subjectId', description: '证据主体 ID，例如字段标准 ID。', required: true }
    ],
    prechecks: [
      {
        title: '确认 DataSpec 服务、项目和本地配置可用',
        command: 'node tools/dataspec-cli.mjs doctor --project <projectId> --format json',
        expected: 'server/project/auth 检查通过；warn 项需要在证据结论中记录。'
      },
      {
        title: '确认 standard-evidence capability 是只读 API-only',
        command: 'node tools/dataspec-cli.mjs capability show standard-evidence --project <projectId> --format json',
        expected: 'writeRisk 为 READ_ONLY，apiEndpoints 包含 GET /api/standard-evidence，cliCommands/mcpResources/mcpTools 为空。'
      }
    ],
    steps: [
      {
        order: 1,
        title: '读取跨来源标准证据',
        command: 'GET /api/standard-evidence?projectId=<projectId>&subjectType=<subjectType>&subjectId=<subjectId>',
        purpose: '获取字段标准来源、可信度、最近使用、覆盖率、示例和相关任务证据的聚合视图。',
        output: 'cross-source-standard-evidence-view JSON。'
      },
      {
        order: 2,
        title: '判断证据是否足以支撑回答或变更',
        command: '检查 evidenceSummary、confidence、recentUsage、coverage 和 gaps',
        purpose: '避免只凭字段名或单条示例修改标准；证据不足时先停止并补充上下文。',
        output: '证据充分性判断和缺口列表。'
      },
      {
        order: 3,
        title: '记录证据结论和下一步',
        command: '在任务说明中记录证据摘要、采信原因、未采信来源和建议动作',
        purpose: '让字段标准问答、候选变更或人工复核可追溯。',
        output: '字段标准证据摘要和后续动作。'
      }
    ],
    expectedArtifacts: [
      '字段标准证据摘要',
      '可信度、最近使用和覆盖率判断',
      '证据缺口、未采信来源和后续动作说明'
    ],
    failureHandling: [
      {
        condition: 'capability show 未返回 standard-evidence 或 surfaces 不一致',
        nextAction: '先更新服务端 capability catalog 或本地 CLI 版本，不要假设存在独立 CLI/MCP 工具。'
      },
      {
        condition: 'standard evidence 为空或可信度不足',
        nextAction: '标记证据不足，先导出最小 AI Context、查看字段目录或补充使用示例，不要直接修改标准。'
      },
      {
        condition: '不同来源证据冲突',
        nextAction: '保留冲突来源和采信规则，创建任务卡或交给人工确认后再进入标准候选/变更流程。'
      }
    ],
    nextActions: [
      '把证据摘要和标准版本写入回答或交付说明。',
      '如果需要修改标准，先创建候选或变更预览，并附上证据结论。',
      '如果只是问答，明确说明证据来源、可信度和未覆盖风险。'
    ],
    sideEffectPolicy: 'plan-only'
  },
  {
    id: 'standard-maintenance',
    title: '标准维护 Inbox 工作流',
    goal: '把候选、字段质量、覆盖率缺口或 AI 失败反馈转成只读 dry-run 计划，再由用户显式确认执行维护动作。',
    requiredInputs: [
      { name: 'projectId', description: 'DataSpec 项目 ID。', required: true },
      { name: 'sourceType', description: '维护来源类型：STANDARD_CANDIDATE、FIELD_QUALITY、FIELD_COVERAGE 或 AI_TASK_FAILURE。', required: true },
      { name: 'sourceIds', description: '可选来源 ID 列表，例如候选 ID、字段 ID 或任务运行 ID。', required: false },
      { name: 'issueCodes', description: '可选字段质量问题代码，例如 comment_missing、code_set_missing。', required: false },
      { name: 'coverageStatuses', description: '可选覆盖率状态，例如 UNMANAGED、POSSIBLE_DUPLICATE、MISSING_COMMENT。', required: false }
    ],
    prechecks: [
      {
        title: '确认 DataSpec 服务、项目和 token 可用',
        command: 'node tools/dataspec-cli.mjs doctor --project <projectId> --format json',
        expected: 'server/project/auth 检查通过；warn 项需要在维护记录中说明。'
      },
      {
        title: '确认维护来源仍有待处理项',
        command: 'GET /api/standard-maintenance/workflows/plan { "projectId": <projectId>, "sourceType": "<sourceType>" }',
        expected: 'workflowPlan.executionState.status 为 DRY_RUN 或明确 BLOCKED 原因。'
      }
    ],
    steps: [
      {
        order: 1,
        title: '生成维护 workflow dry-run 计划',
        command: 'POST /api/standard-maintenance/workflows/plan { "projectId": <projectId>, "sourceType": "<sourceType>", "sourceIds": [<ids>] }',
        purpose: '把候选、质量或覆盖率信号转成 inboxAction、recipeBinding、dryRunSteps、executionState、evidenceLinks 和 nextActions。',
        output: 'workflowPlan JSON。'
      },
      {
        order: 2,
        title: '复核 workflowPlan 证据和边界',
        command: '检查 workflowPlan.evidenceLinks、workflowPlan.executionState、workflowPlan.undoHint 和 workflowPlan.nextActions',
        purpose: '确认没有 raw evidence、SQL、AI payload、JDBC URL、DSN、token、password 或 Authorization，并记录 partial/blocked 边界。',
        output: '可复制的维护计划摘要。'
      },
      {
        order: 3,
        title: '人工确认后执行单步维护动作',
        command: '按 workflowPlan.dryRunSteps[].recommendedAction 打开页面或调用既有候选/字段 API',
        purpose: '只执行用户确认过的候选采纳/合并/忽略、字段 metadata 修复或覆盖率后续动作。',
        output: '候选状态、字段更新结果或维护页面操作记录。'
      },
      {
        order: 4,
        title: '验证维护结果',
        command: '按 workflowPlan.dryRunSteps 中 verify 阶段的命令重新运行候选列表、字段质量报告或覆盖率报告',
        purpose: '确认待处理项降低，且未扫描、失败或未确认项没有被视为完成。',
        output: '验证命令、报告摘要和剩余风险。'
      },
      {
        order: 5,
        title: '归档维护证据',
        command: '记录 workflowPlan、执行动作、验证结果、completionCheck 和未处理项',
        purpose: '让用户、AI 或后续 OpenSpec 任务能恢复上下文。',
        output: '标准维护交付摘要。'
      }
    ],
    expectedArtifacts: [
      'standard maintenance workflowPlan JSON',
      '人工确认过的维护动作记录',
      '候选/字段质量/覆盖率验证结果',
      '剩余 blocked 或 partial 边界说明'
    ],
    failureHandling: [
      {
        condition: 'workflowPlan.executionState.status 为 BLOCKED',
        nextAction: '先按 nextActions 补充来源报告、候选选择或项目信息，再重新生成计划。'
      },
      {
        condition: 'execute 阶段需要写入候选或字段',
        nextAction: '必须人工确认具体候选、目标字段和理由；不要让 AI 自动批量采纳、合并、忽略或编辑字段。'
      },
      {
        condition: 'coverage 来源为 PARTIAL/CANCELLED/FAILED',
        nextAction: '只处理成功统计的字段，并在维护记录中保留未扫描或失败边界。'
      },
      {
        condition: '计划或错误信息包含 token/password/JDBC URL/DSN/Authorization',
        nextAction: '停止复制该内容，先重新生成脱敏计划或修复脱敏边界。'
      }
    ],
    nextActions: [
      '把 workflowPlan.recipeBinding 写入 AI task card 或维护说明。',
      '执行前逐条确认 requiresConfirmation=true 的步骤。',
      '完成后重新运行对应报告，并记录 completionCheck。'
    ],
    sideEffectPolicy: 'plan-only'
  }
]

export function listWorkflowRecipes() {
  return WORKFLOW_RECIPES.map(({ id, title, goal, requiredInputs }) => ({
    id,
    title,
    goal,
    requiredInputs
  }))
}

export function getWorkflowRecipe(id) {
  return WORKFLOW_RECIPES.find((recipe) => recipe.id === id) ?? null
}

export function supportedWorkflowRecipeIds() {
  return WORKFLOW_RECIPES.map((recipe) => recipe.id)
}

export function workflowCatalogPayload() {
  return {
    kind: 'dataspec-workflow-recipes',
    schemaVersion: 1,
    recipes: listWorkflowRecipes()
  }
}

export function workflowRecipesResourcePayload(projectId) {
  return {
    kind: 'dataspec-workflow-recipes',
    schemaVersion: 1,
    projectId,
    recipes: WORKFLOW_RECIPES
  }
}

export function formatWorkflowListText(recipes = listWorkflowRecipes()) {
  return [
    'DataSpec workflow recipes',
    '',
    ...recipes.map((recipe) => `- ${recipe.id}: ${recipe.title}。${recipe.goal}`),
    '',
    '查看详情：node tools/dataspec-cli.mjs workflow show <id> --format json'
  ].join('\n')
}

export function formatWorkflowRecipeText(recipe) {
  return [
    `# ${recipe.title} (${recipe.id})`,
    '',
    recipe.goal,
    '',
    '## Required Inputs',
    ...recipe.requiredInputs.map((input) => `- ${input.required ? '[required]' : '[optional]'} ${input.name}: ${input.description}`),
    '',
    '## Prechecks',
    ...recipe.prechecks.map((check) => `- ${check.title}: ${check.command}`),
    '',
    '## Steps',
    ...recipe.steps.map((step) => `${step.order}. ${step.title}: ${step.command}`),
    '',
    '## Expected Artifacts',
    ...recipe.expectedArtifacts.map((artifact) => `- ${artifact}`),
    '',
    '## Failure Handling',
    ...recipe.failureHandling.map((item) => `- ${item.condition}: ${item.nextAction}`),
    '',
    '## Next Actions',
    ...recipe.nextActions.map((action) => `- ${action}`),
    ''
  ].join('\n')
}

export function workflowRecipesMarkdown(projectId = '<projectId>') {
  return [
    '# DataSpec Workflow Recipes',
    '',
    '这些 recipe 是给 AI agent 和开发者读取的任务计划，不会自动执行步骤，也不会调用外部 LLM。',
    '',
    ...WORKFLOW_RECIPES.flatMap((recipe) => [
      `## ${recipe.id}: ${recipe.title}`,
      '',
      recipe.goal,
      '',
      `项目参数示例：projectId=${projectId}`,
      '',
      '### 输入',
      ...recipe.requiredInputs.map((input) => `- ${input.required ? '必填' : '可选'} ${input.name}: ${input.description}`),
      '',
      '### 前置检查',
      ...recipe.prechecks.map((check) => `- ${check.title}: \`${check.command}\``),
      '',
      '### 步骤',
      ...recipe.steps.map((step) => `${step.order}. ${step.title}: \`${step.command}\``),
      '',
      '### 产物',
      ...recipe.expectedArtifacts.map((artifact) => `- ${artifact}`),
      '',
      '### 失败恢复',
      ...recipe.failureHandling.map((item) => `- ${item.condition}: ${item.nextAction}`),
      '',
      '### 下一步',
      ...recipe.nextActions.map((action) => `- ${action}`),
      ''
    ])
  ].join('\n')
}
