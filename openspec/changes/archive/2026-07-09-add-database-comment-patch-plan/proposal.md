## Why

反向导入已经能把源库 metadata 带入 DataSpec，但标准字段或表注释被修正后，源数据库 COMMENT 可能长期落后。用户和 AI 需要一份只读、可审阅、可导出的 COMMENT 回写计划，而不是直接生成不可控的写库动作。

## What Changes

- 新增数据库 COMMENT patch plan 能力：基于只读数据库 metadata 和当前 DataSpec 标准，输出表/列注释差异、风险、方言支持、dry-run SQL、rollback hint、evidence 和 nextActions。
- 新增后端只读计划 API，默认不执行数据库写入，不创建持久化任务，不读取业务数据行。
- 新增 CLI JSON/text 预览命令，调用计划 API 并稳定输出计划，不执行 SQL。
- 在反向导入页面新增 COMMENT 回写计划预览入口和结果展示，可导出或复制 SQL/JSON 证据。
- 所有输出必须脱敏，禁止返回 password、token、Authorization、完整 JDBC URL、DSN、连接串或源库业务数据行。

## Capabilities

### New Capabilities

- `db-comment-patch-plan`: 定义数据库表/列 COMMENT 回写计划的 API 响应、差异模型、安全边界、方言 SQL 草稿和证据导出契约。

### Modified Capabilities

- `dataspec-cli`: 新增 `comment-plan preview` 只读命令及 fixture/错误输出契约。
- `db-reverse-import-frontend`: 在反向导入页数据库直连流程中展示 COMMENT 回写计划入口、风险摘要、dry-run SQL 和导出证据。

## Impact

- 后端：新增或扩展反向导入/数据库 comment plan controller、service、model 和单元测试；复用现有数据库 metadata 读取、项目访问控制、脱敏与方言判断。
- 前端：新增 API wrapper、类型、反向导入页入口和展示组件/工具测试。
- CLI/tools：新增命令、契约 fixture、status/contract check 覆盖和 tools 测试。
- OpenSpec：新增 `db-comment-patch-plan` 主能力 delta，修改 `dataspec-cli` 与 `db-reverse-import-frontend` delta。
- 风险等级：SDD standard；涉及 API/CLI 外部契约，提交或归档前必须进行独立子 agent 只读评审。
