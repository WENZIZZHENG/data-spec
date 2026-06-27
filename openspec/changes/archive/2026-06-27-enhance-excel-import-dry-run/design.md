## Context

`ImportExportService.previewExcelImport` 当前只累计 `ExcelSheetSummary`，错误通过 `ExcelImportError` 返回。前端 `ImportExport.vue` 也只展示 summary 和错误表。P4-6 需要在不改变写入流程的前提下，让预览更像 dry-run：用户能看见每一行会新增、更新还是冲突，以及关键字段变化。

## Goals / Non-Goals

**Goals:**

- 给 `ExcelImportPreview` 增加 `items` 明细列表。
- 为字段、代码集、枚举值生成统一预览项模型。
- 对更新项生成字段级 diff，包含字段名、原值、新值。
- 前端展示明细、状态、原因和 diff。
- 保持现有 summary/errors 兼容。

**Non-Goals:**

- 不改变 Excel 模板列。
- 不改变 `importExcel` 的写入顺序和 upsert 策略。
- 不实现枚举覆盖策略选择器；第一版只展示“新增/更新/冲突”。
- 不实现在线编辑 Excel 预览。

## Decisions

1. **统一明细模型**
   - 理由：字段、代码集、枚举值都可用 sheet/key/action/status/reason/diffs 表达，前端可用一张表展示。
   - 替代方案：每类 sheet 各自定义明细；类型更多，前端展示重复。

2. **只在预览阶段计算 diff**
   - 理由：写入逻辑已经稳定，dry-run 信息应由预览负责，不增加导入事务复杂度。
   - 替代方案：导入时再返回 diff；会让用户在确认前仍看不到风险。

3. **保留 summary/errors**
   - 理由：当前前端和 API 消费方已有依赖，新增字段保持兼容。

## Risks / Trade-offs

- **diff 字段过多导致页面拥挤** → 前端按行展开或用紧凑表格展示。
- **对象关联字段显示不直观** → 代码集和数据域以 code 维度展示，避免暴露内部 ID。
- **更新项无实际变化** → 第一版仍归类为 update，但 diff 可为空，用于提示“已存在且无字段变化”。
