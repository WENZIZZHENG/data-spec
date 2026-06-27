## Context

当前 AI Context 通过 `AiContextExportService` 直接读取字段、枚举和规则生成 `field-catalog.json`、`rules.yaml`、manifest、README 和 AGENTS fragment。SQL 检查记录由 `SqlCheckRecordService` 保存原 SQL、fixedSql 和 issues；DDL 生成由 `DdlGeneratorService` 返回 `ddl` 和 `lintResult`。

P6-1 的关键是先建立可复现的“标准版本元数据”，而不是一次性实现复杂发布治理。

## Goals / Non-Goals

**Goals:**

- 用户可为项目当前标准创建命名快照，得到 `version`、`snapshotHash` 和创建时间。
- 快照 hash 基于项目字段、枚举、规则的确定性 JSON payload 计算，重复内容产生同一 hash。
- AI Context 导出带上当前最新快照的 `specVersion`、`specHash`、`snapshotId`。
- SQL 检查记录与 DDL 生成结果引用当前最新快照，便于后续回放。
- 没有快照时保持现有流程可用，版本字段为空或标记为 `unversioned`。

**Non-Goals:**

- 不做审批、发布流、语义化版本自动升级或强制冻结标准。
- 不实现按历史快照完整还原字段/规则的复杂查询；第一版保存 payload，为后续回放打基础。
- 不阻断用户在无快照状态下继续 lint、导出或生成。
- 不做跨项目快照合并或组织级标准治理。

## Decisions

1. 新增 `ds_standard_snapshot` 表。
   - 字段：`project_id`、`version`、`name`、`description`、`snapshot_hash`、`payload_json`、`created_at`、`updated_at`、`is_deleted`。
   - 原因：个人/小团队需要能追溯“这版标准是什么”，payload JSON 比分散复制多张表更容易快速落地。

2. 快照 hash 使用 SHA-256(payload JSON)。
   - 原因：AI Context 和记录只需稳定标识内容；hash 可用于判断两次导出是否使用同一份标准。
   - payload 生成时按字段/枚举/规则主键和业务键排序，避免数据库返回顺序影响 hash。

3. 当前版本引用使用“最新快照”。
   - 原因：第一版不引入复杂发布态；用户创建快照后，后续 lint/DDL/AI Context 自动引用最新快照。
   - 无快照时返回 `unversioned` 元数据，业务能力不阻断。

4. SQL 检查记录只保存快照 ID/版本/hash。
   - 原因：记录表已保存 SQL 与 issues，不适合再塞完整标准 payload；完整 payload 在快照表中。

5. DDL 生成结果增加 `standardSnapshot` 元数据。
   - 原因：AI/CLI 拿到生成结果时可以直接知道引用标准，不必额外查 API。

## Risks / Trade-offs

- [payload 变大] → 第一版只保存字段、枚举、规则必要字段；不保存检查记录或生成结果。
- [hash 与业务感知版本不一致] → 允许用户填写版本名/说明，hash 用于内容一致性。
- [无快照时用户误以为已版本化] → AI Context manifest 明确输出 `specVersion: unversioned`。
- [历史快照还原未实现] → payload 已保存，后续可基于 payload 做回放或导出指定快照。

## Migration Plan

1. 新增 V9 迁移创建 `ds_standard_snapshot`，并为 `ds_sql_check_record` 增加快照引用列。
2. 旧项目和旧检查记录不自动补快照；用户可手动创建第一版快照。
3. 新增能力均兼容无快照状态，避免破坏现有 lint/导出/生成流程。
