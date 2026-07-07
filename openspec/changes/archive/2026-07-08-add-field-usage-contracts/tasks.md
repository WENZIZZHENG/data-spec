## 1. 数据模型与后端契约

- [x] 1.1 新增字段使用契约迁移脚本，补充列说明和默认空兼容策略。
- [x] 1.2 扩展 Field entity、FieldReq、OpenAPI schema 和字段保存/更新映射，字段级注释说明业务语义和敏感边界。
- [x] 1.3 复用或补充敏感内容校验，拒绝 usage contract 中的 password、token、Authorization、完整 JDBC URL、DSN 和私钥。
- [x] 1.4 补充字段 CRUD、迁移兼容和敏感内容拒绝的后端测试。

## 2. AI 读取与低置信链路

- [x] 2.1 字段检索结果输出 usageContractSummary，并在命中 avoidWhen 或 misuseExamples 时追加需要确认的 nextActions。
- [x] 2.2 标准问答读取 usage contract，针对统计、join、过滤、写入、替代和禁用场景输出证据、缺失事实或低置信提示。
- [x] 2.3 字段推荐或相关 AI 入口避免把废弃、停用、草稿、禁用场景字段作为直接可采纳结果。

## 3. AI Context 与 DDL/Prompt

- [x] 3.1 AI Context `field-catalog.json` 和 schema 输出 additive `usageContract` 对象。
- [x] 3.2 `DATABASE_RULES.md`、README/AGENTS fragment 或 prompt guidance 输出高风险字段使用边界，不生成空噪声段落。
- [x] 3.3 DDL/Prompt 上下文读取 usage contract 作为只读指导，不自动改写 SQL 或执行写入。
- [x] 3.4 补充 AI Context、DDL/Prompt golden 或目标测试，覆盖有契约、无契约和禁用场景。

## 4. 前端体验

- [x] 4.1 字段库创建/编辑表单新增紧凑“使用契约”区域，支持推荐使用、禁用场景、join、默认过滤、聚合、替代指导和误用样例。
- [x] 4.2 字段详情、字段检索或标准问答页面展示契约证据和低置信原因，空契约不显示噪声。
- [x] 4.3 补充前端源码级测试，覆盖表单字段、展示文案和标准问答低置信处理。

## 5. 文档、验证与评审

- [x] 5.1 同步 TODO P6-187 状态、实现范围、验证证据和剩余边界。
- [x] 5.2 运行 `openspec validate add-field-usage-contracts --strict`，并记录 Verification Evidence。
- [x] 5.3 运行匹配风险的后端、前端、CLI/AI Context 和通用验证命令。
- [x] 5.4 启动独立子 agent 只读代码评审，处理 findings 并关闭子 agent。
- [x] 5.5 完成提交前 staged diff、敏感项扫描、`git diff --check` / `git diff --cached --check`，创建本地 commit，不 push。

## Verification Evidence

- TDD 红灯：`mvn test "-Dtest=StandardReusePackServiceImplTest,ProjectBackupServiceImplTest"` 首次失败 3 项，证明复用包 payload/apply 和项目备份 overwrite 会丢失 usage contract。
- 目标回归：`mvn test "-Dtest=StandardReusePackServiceImplTest,ProjectBackupServiceImplTest"` 14 tests / 0 failures，确认复用包与备份恢复传播已补齐。
- 评审修复红灯：`mvn test "-Dtest=FieldServiceImplTest"` 首次新增失败 3 项，覆盖 usage contract 字段清空需要 `FieldStrategy.ALWAYS`、单个中文 bigram 误判禁用场景、suggest 降级缺少字段 evidence；`pnpm test -- standardQuestionDisplay` 首次新增失败 1 项，覆盖前端标准问答同类误降级。
- 评审修复回归：`mvn test "-Dtest=FieldServiceImplTest"` 52 tests / 0 failures；`pnpm test -- standardQuestionDisplay` 162 tests / 0 failures。
- OpenSpec：`openspec validate add-field-usage-contracts --strict` 通过，change valid。
- 后端：`mvn test` 553 tests / 0 failures / 0 errors；保留本机 Maven `javax.annotation-api` transitive POM warning、ByteBuddy 动态 agent warning 和既有性能 baseline 慢操作日志。
- 前端：`pnpm test` 162 tests / 0 failures；`pnpm build` 通过，保留既有 `@vueuse/core` pure annotation、chunk size 和 plugin timings warning。
- OpenAPI：临时启动后端连接用户授权的一次性 PostgreSQL `ai_test`，运行 `pnpm exec openapi-typescript http://localhost:18092/api-docs -o src/api/schema.ts` 重新生成 schema；随后 `node scripts/check-openapi-schema.mjs --source http://localhost:18092/api-docs` 输出 `OpenAPI schema.ts 已是最新`。该脚本保留 Node `DEP0190` 参数传递 warning。
- 真实 PostgreSQL：临时启动后端/前端连接 `ai_test`，`node tools/dataspec-local-smoke.mjs --server http://localhost:18091 --web http://localhost:15174 --json --skip-demo --timeout-ms 180000` 的 web/api-docs 检查通过；随后通过 API 新建一次性项目 `projectId=2` 和字段 `fieldId=11`，成功写入并读回 `preferredUseCases`、`avoidWhen`、`aggregationHints` 等 usage contract 字段。完整 demo smoke 在复用旧 `ai_test` 演示项目时命中既有 `ds_rule_baseline.applied_at` TIMESTAMPTZ 到 `LocalDateTime` 映射问题，已记录为历史数据遗留，不归因本变更。
- 真实 PostgreSQL 清空回归（评审修复）：临时启动后端连接用户授权的一次性 PostgreSQL `ai_test`，通过 API 新建一次性项目 `projectId=5` 和字段 `fieldId=14`，先写入七个 usage contract 字段，再用更新接口省略这些字段并读回，确认七个字段已清空；同一项目调用 `/api/fields/suggest` 查询“统计订单金额”返回 `existing=true`、`recommendedName=amount_clear_1783442785981`，证明不会只因“金额”单个中文 bigram 命中禁用场景。
- 代码评审：独立只读子 agent `019f3d66-fa51-74d2-91b0-e63149641419`（用途：P6-187 SDD full 代码评审）未发现 Critical；提出 2 个 Important。已修复：1）七个 usage contract 字段设置 `updateStrategy = FieldStrategy.ALWAYS` 并补清空测试/真实库验证；2）usage contract 中文 bigram 阈值提升为 2，suggest 先确认字段命中再降级，并在降级结果保留字段 evidence，前端标准问答同步阈值。该 agent 已关闭。
- 提交前检查：`git diff --check` 与 `git diff --cached --check` 均无空白错误；`git diff --cached --stat` 和 staged 文件列表已核对；staged 文件名与 staged diff 已按 `password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn` 扫描，命中均为字段说明、OpenAPI/generated schema 字段名、脱敏/拒绝测试样例或 OpenSpec 安全边界描述，未发现真实密码、token、Authorization、JDBC URL、DSN 或私钥。
- 归档预检：已将 delta specs 同步到主规格 `field-usage-contracts`、`field-model`、`field-standard-search`、`ai-context-package` 和 `ddl-generator-tool`；`openspec validate add-field-usage-contracts --strict` valid；`openspec validate --all` 121 passed / 0 failed；`git diff --check` 通过，仅 LF/CRLF warning。
