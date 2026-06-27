## 1. 后端备份包导出

- [ ] 1.1 新增 Flyway 迁移 `V14__add_project_restore_record.sql`，创建恢复摘要记录表，不保存完整备份包。
- [ ] 1.2 新增 `projectbackup` 模块 DTO/模型，定义 `ProjectBackupPackage`、asset sections、counts、sanitization、restore plan/result 和 restore record。
- [ ] 1.3 新增 `ProjectBackupService.exportPackage(projectId)`，读取项目、数据域、字段、枚举、规则、规则基线、模板、快照、来源批次和变更日志摘要，并生成稳定 `packageHash`。
- [ ] 1.4 实现备份包脱敏扫描，确保导出不包含 password/token/JDBC URL/API token hash/source data rows，并输出 sanitization summary。
- [ ] 1.5 新增 `/api/project-backups/export?projectId=` API，接入项目访问校验和 OpenAPI 契约。

## 2. 恢复 dry-run 与 apply

- [ ] 2.1 实现备份包校验：schemaVersion、packageHash、必填资产、兼容版本和疑似敏感字段扫描。
- [ ] 2.2 实现 `previewRestore`，支持新项目和 existing targetProjectId 两种模式，输出 CREATE/SKIP/UPDATE/CONFLICT/BLOCKED 明细且不写库。
- [ ] 2.3 实现 `applyRestore`，复用 dry-run 计划按依赖顺序恢复资产，默认不覆盖，`overwrite=true` 时更新支持覆盖的资产。
- [ ] 2.4 新增恢复摘要记录 repository/service，保存 packageHash、source/target project、overwrite、counts、warnings、operator 和 summaryJson。
- [ ] 2.5 新增 `/api/project-backups/restore/preview` 和 `/api/project-backups/restore/apply` API，并接入可读错误诊断。
- [ ] 2.6 补后端单元测试，覆盖导出脱敏、hash 校验、dry-run 不写库、overwrite=false/true、恢复记录和不支持 schemaVersion。

## 3. 前端备份恢复入口

- [ ] 3.1 更新 OpenAPI 生成类型或手动补齐 `schema.ts` 中备份/恢复 API 与 DTO。
- [ ] 3.2 新增 `dataspec-web/src/api/projectBackup.ts` 和类型导出。
- [ ] 3.3 新增或扩展前端页面，支持当前项目导出备份 JSON、粘贴/上传备份包、dry-run 预览冲突和确认恢复。
- [ ] 3.4 页面明确展示敏感信息不包含 password/token/source rows；无当前项目时禁用导出但允许恢复到新项目。
- [ ] 3.5 补前端 smoke/utility 测试，锁定备份恢复入口、项目边界、dry-run 文案和 API wrapper。

## 4. 文档、评审、验证与提交

- [ ] 4.1 更新 README 和 TODO，将 P6-24 标记为已完成第一版并说明边界。
- [ ] 4.2 运行 `mvn test`、`pnpm test`、`pnpm build`、相关 Node 测试、`npx.cmd openspec validate add-project-backup-restore-package` 和 `git diff --check`。
- [ ] 4.3 按代码评审清单做直接评审，不使用子 agent；修复发现的问题或记录暂不处理理由。
- [ ] 4.4 通过验证后创建本地 commit。
