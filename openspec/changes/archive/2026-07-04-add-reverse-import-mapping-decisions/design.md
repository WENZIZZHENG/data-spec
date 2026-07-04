# Design

## Data Model

新增 `ds_reverse_import_decision`，按批次记录字段级决策。该表只保存元数据、理由和标准字段引用，不保存数据库密码、JDBC URL 或业务数据行。

关键字段：

- `project_id`、`batch_id`、`source_type`：绑定项目、导入批次和来源类型。
- `schema_name`、`table_name`、`column_name`、`data_type`：定位真实数据库字段。
- `decision_type`：`EXISTING_MATCH`、`NEW_CANDIDATE`、`IMPORTED`、`SKIPPED_EXISTING`、`IGNORED`。
- `matched_field_id`、`matched_field_name`：命中或最终创建/跳过的标准字段。
- `match_reason`、`confidence`、`ignore_reason`、`confirm_reason`：给用户和 AI 的复盘依据。
- `metadata_json`：保存候选字段快照，复用现有来源追踪的 JSON 边界。

## Backend Flow

预览阶段沿用现有 `standardFieldIndex`，但扩展为带匹配类型的索引：

- 字段名命中：`EXISTING_MATCH`，置信度 `1.00`。
- alias 命中：`EXISTING_MATCH`，置信度 `0.92`，理由包含 alias 与 canonical 字段。
- 未命中：`NEW_CANDIDATE`，置信度 `0.65`，同时进入候选和非标准列表。

确认阶段：

- 选中的未命中候选成功创建字段后记录 `IMPORTED`。
- 确认时发现字段已存在，记录 `SKIPPED_EXISTING`。
- 前端未选择的候选随请求作为 `ignoredCandidates` 提交，记录 `IGNORED`。
- 只要产生最终决策，就创建批次；字段来源记录仍只为真正新建字段写入 `ds_field_source`。

## Frontend Flow

数据库直连预览页在候选表中增加“确认理由”输入。默认理由来自后端 `matchReason` 或“确认作为新标准字段导入”。导入时提交：

- `candidates`：当前勾选候选，包含 `confirmReason`。
- `ignoredCandidates`：未勾选候选，包含默认 `ignoreReason`。

导入结果区域展示 `mappingDecisions` 摘要，便于用户复盘本批次处理。

## Compatibility

- 旧客户端只提交 `candidates` 时仍可导入；未提供 `ignoredCandidates` 时不会记录忽略项。
- `FieldCandidate` 新增字段均为可选，保持 JSON 兼容。
- 已有来源追踪和幂等写保护继续生效。

## Risks

- 记录忽略项会增加批次表和决策表写入量；第一版只记录用户显式确认导入时同一预览中的未选候选，不做全库自动记录。
- 预览中的匹配理由是确定性规则，不等同语义推荐；语义相似但未命中的字段仍作为候选或非标准字段处理。

## Validation

- 后端新增迁移、实体、repository、服务测试和 controller/API 测试。
- 前端新增 selection utility 测试和 smoke 测试。
- 变更完成后运行 `openspec validate add-reverse-import-mapping-decisions --strict`、后端相关测试、`mvn test`、`pnpm gen:api`、`pnpm check:api`、`pnpm test`、`pnpm build` 和 `openspec validate --all`。
