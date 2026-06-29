## Why

字段别名散落在单个字段上后，AI 很难稳定理解“用户/账号/会员”“手机号/电话/mobile”“金额/费用/price”等项目级术语关系。P6-48 要新增轻量业务术语表与同义词词根库，让字段推荐、字段检索和 AI Context 能共享同一层语义映射。

本 change 按 SDD `full` 级处理：它会新增持久化模型、后端 API、前端维护页，并影响 AI 可消费输出和字段推荐/检索结果。

## What Changes

- 新增项目级业务术语表模型，支持术语、同义词、英文词根、拼音缩写、禁用词、推荐 canonical 字段、适用范围、示例字段和状态。
- 新增术语表 CRUD、列表、冲突检测 API，并在安全模式下继续按 projectId 做访问边界校验。
- 字段推荐和字段检索读取启用的术语表条目，把术语命中原因写入结果，让“会员手机号”“订单费用”等自然语言更稳定映射到标准字段集合。
- AI Context 导出精简 glossary，让 AI 在离线包中读取项目术语、禁用词和 canonical 字段提示。
- 前端新增“基础数据 / 业务术语表”页面，支持维护术语、查看冲突、关联字段和复制 AI 可读摘要。

## Capabilities

### New Capabilities
- `business-glossary-synonym-roots`: 项目级业务术语表、同义词、词根、缩写、禁用词和 canonical 字段映射能力。

### Modified Capabilities
- `field-suggestion`: 字段推荐可使用业务术语表增强匹配分数和命中原因。
- `field-standard-search`: 字段检索可使用业务术语表扩展 query 命中范围和 AI 下一步建议。
- `ai-context-package`: AI Context 包可导出精简 glossary。

## Impact

- 后端：新增 Flyway 迁移、entity/mapper/repository/service/controller/model/test；扩展字段推荐、字段检索和 AI Context 导出。
- 前端：新增 API 封装、类型使用、路由、菜单和维护页面。
- 文档：更新 README 当前能力、TODO P6-48 状态和 OpenSpec 验证证据。
- 验证：`mvn test`、`pnpm build`、OpenSpec validate、diff check；如 OpenAPI schema 变化影响前端类型，更新生成产物或手写兼容类型。

## Non-Goals

- 不做企业级本体、知识图谱或跨项目统一术语治理。
- 不引入向量数据库、外部 LLM 或语义 embedding。
- 不自动覆盖字段已有别名，不自动把术语写入正式字段。
- 不做审批流、发布流、权限矩阵或团队级 owner 治理。

## Verification Evidence

- 2026-06-29：`mvn test`（dataspec-server）通过，359 tests，0 failures，0 errors。
- 2026-06-29：`pnpm build`（dataspec-web）通过；Vite/Rolldown 仅输出依赖 pure annotation 和 chunk size 警告。
- 2026-06-29：`openspec validate add-business-glossary-synonym-roots --strict` 通过。
- 2026-06-29：`git diff --check` 通过；仅有仓库换行转换提示。
- 2026-06-29：本地结构化代码评审已执行，按用户要求未使用子 agent；修复了前端删除确认取消时的未处理 Promise，并补充 AI Context glossary 截断 warning 测试。
