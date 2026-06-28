## Why

DataSpec 优先服务个人/小团队，本地库、演示库和轻量部署经常需要换机器或重建环境。现有导出更偏 AI Context、数据字典或 Excel 字段交换，缺少一个能恢复项目标准资产、提前 dry-run 冲突并明确剔除敏感信息的迁移包。

## What Changes

- 新增项目备份包导出能力，覆盖项目元数据、数据域、标准字段、枚举、规则、规则基线、表模板、标准快照、反向导入来源批次和必要变更日志。
- 新增备份包恢复 dry-run，恢复前输出版本兼容性、目标项目冲突、将创建/跳过/覆盖的资产、敏感信息剔除结果和风险提示。
- 新增确认恢复能力，默认创建新项目或写入指定项目，默认不覆盖已有同名/同编码资产；显式 `overwrite=true` 时才更新可覆盖资产。
- 备份包使用稳定 JSON 契约，包含 schemaVersion、exportedAt、sourceProject、asset counts、specHash/packageHash 和 restoreWarnings。
- README/TODO 更新 P6-24 完成范围、验证证据和不做边界。

## Capabilities

### New Capabilities

- `project-backup-restore`: 覆盖项目备份包导出、恢复 dry-run、确认恢复、兼容性检查、敏感信息剔除和恢复摘要。

### Modified Capabilities

- 无。第一版新增独立备份/恢复能力，复用项目、字段、枚举、规则、模板、快照、来源和变更日志的既有契约，不改变其原有需求。

## Impact

- 后端：新增备份包 DTO、服务和 API；读取现有 repository/service 生成包，恢复时复用现有写入路径并记录摘要。
- 前端：新增或扩展设置/项目页面入口，支持导出备份 JSON、粘贴/上传备份包 dry-run、查看冲突摘要并确认恢复。
- 数据：可新增轻量恢复记录表保存 restore summary；不保存源数据库数据行、数据库密码、API token 明文或完整连接串。
- OpenAPI/类型：新增备份/恢复相关 schema 与前端 API wrapper。
- 测试：后端覆盖导出脱敏、dry-run 冲突、恢复幂等和 overwrite 行为；前端 smoke 覆盖入口与项目边界。

## Verification Evidence

- `mvn test`：240 tests, 0 failures, 0 errors。
- `pnpm test`：57 tests, 0 failures。
- `pnpm build`：通过；仅保留既有第三方 `@vueuse/core` pure annotation 与 chunk size warning。
- `node --test tools\dataspec-config.test.mjs tools\dataspec-cli.test.mjs tools\dataspec-mcp.test.mjs`：62 tests, 0 failures。
- `npx.cmd openspec validate add-project-backup-restore-package`：valid。
- `git diff --check`：通过；仅输出 Windows LF/CRLF 换行提示。
- 直接代码评审：未使用子 agent；修复了前端 dry-run 参数变更未清空旧计划、手工 OpenAPI `StandardSnapshot` 字段与后端实体不一致两个 finding。
