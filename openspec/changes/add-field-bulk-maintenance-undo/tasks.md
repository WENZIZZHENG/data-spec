## 1. 后端测试基线

- [x] 1.1 通过 `openspec validate add-field-bulk-maintenance-undo`，确认 proposal/design/spec/tasks 可实施。
- [x] 1.2 新增字段批量维护服务测试，覆盖预览、提交、显式 patch、无变化跳过、无效 key、跨项目拒绝和变更日志写入。
- [x] 1.3 新增字段回退服务测试，覆盖按日志恢复、记录 undo 日志、跨项目/目标不匹配/缺 beforeJson/字段名冲突拒绝。
- [x] 1.4 新增控制器测试，覆盖批量预览、批量提交和回退接口转发。

## 2. 后端批量维护与回退

- [x] 2.1 新增批量维护模型，如 `FieldBulkUpdateReq`、`FieldBulkUpdatePreview`、`FieldBulkUpdateResult`、`FieldBulkUpdateItem`、`FieldChangeUndoResult`。
- [x] 2.2 扩展 `StandardChangeLogRepository/Service`，支持按日志 ID 查询并保持项目边界校验。
- [x] 2.3 扩展 `FieldService/FieldServiceImpl`，实现批量维护 preview 与 submit，复用显式 patch、合法状态校验、字段名唯一性和批量归组中的全量校验模式。
- [x] 2.4 实现字段变更日志回退：从 beforeJson 恢复字段关键属性，拒绝不安全回退，并写入 `undo` 变更日志。
- [x] 2.5 扩展 `FieldController`，新增批量维护预览、批量维护提交和字段回退接口。

## 3. 前端字段库体验

- [x] 3.1 扩展 `src/api/field.ts`、必要的 change-log API 和类型导出，接入批量维护与回退接口。
- [x] 3.2 改造 `FieldLibrary.vue`：新增“批量维护”入口，支持 status/category/tags/sensitive/codeSetId/aliases 显式更新。
- [x] 3.3 批量维护对话框展示后端预览结果，用户确认后提交并刷新字段列表、分组摘要和选择状态。
- [x] 3.4 新增字段最近变更弹窗或入口，展示 update/undo 日志并支持对可回退日志执行确认回退。
- [x] 3.5 保持现有字段创建、编辑、删除、批量归组、来源、影响、分页和 query keyword 跳转行为可用。

## 4. 文档与契约同步

- [x] 4.1 更新 README 和 TODO，说明 P6-15 批量维护、预览和轻量回退状态。
- [x] 4.2 重新生成或同步 OpenAPI `schema.ts`，确保前端类型契约包含新增接口。
- [x] 4.3 检查新增注释是否覆盖显式 patch、回退边界和项目一致性约束。

## 5. 验证、评审与提交

- [x] 5.1 运行后端相关测试与 `mvn test`，运行前端 `pnpm build` / `pnpm test`，运行 OpenSpec validate、OpenAPI check 和 `git diff --check`。
- [x] 5.2 进行直接代码评审，不使用子 agent；修复 findings 或记录暂不处理理由。
- [x] 5.3 创建本地 commit 后继续下一个待办。
