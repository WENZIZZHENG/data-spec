# P6-20 OpenSpec 归档与主规格同步证据

日期：2026-06-27

## 范围

- 将已完成的 OpenSpec change 从 `openspec/changes/` 归档到 `openspec/changes/archive/2026-06-27-*`。
- 保留 `openspec/specs/` 作为当前主规格入口，供后续 AI 和人工开发优先读取。
- 手动补齐自动归档未能匹配的主规格：
  - `openspec/specs/field-model/spec.md`
  - `openspec/specs/sql-lint-rules/spec.md`
  - `openspec/specs/ai-context-package/spec.md`

## 归档结果

- 归档 change 数量：61。
- 当前 active changes：0。
- 当前主规格数量：57。
- 历史 change 仍保留在 `openspec/changes/archive/`，未删除方案、设计、任务和 delta spec。

## 手动同步说明

`extend-personal-field-model` 和 `add-structured-naming-rules` 的自动归档同步曾因 delta spec 中的 `MODIFIED Requirements` 找不到完全同名主规格标题而无法套用。已按实际能力手动同步：

- 个人字段元数据写入 `field-model` 主规格。
- 字段目录 schema 的个人元数据要求写入 `ai-context-package` 主规格。
- 结构化 `naming:` 导出要求写入 `ai-context-package` 主规格。
- 字段后缀/前缀类型规则写入 `sql-lint-rules` 主规格。

随后这两个 change 使用 `--skip-specs` 归档，避免重复应用已手动同步的 delta。

## Verification Evidence

已执行并读取结果：

```bash
npx.cmd openspec validate extend-personal-field-model
npx.cmd openspec validate add-structured-naming-rules
npx.cmd openspec validate --all
npx.cmd openspec archive extend-personal-field-model --yes --skip-specs
npx.cmd openspec archive add-structured-naming-rules --yes --skip-specs
npx.cmd openspec list --json
```

关键结果：

- `extend-personal-field-model` 验证通过。
- `add-structured-naming-rules` 验证通过。
- `openspec validate --all` 返回 57 passed, 0 failed。
- 两个剩余 completed change 已归档为 `2026-06-27-extend-personal-field-model` 和 `2026-06-27-add-structured-naming-rules`。
- `openspec list --json` 返回 `{"changes":[]}`。

提交追踪：

- 本文件所在提交即为 P6-20 收口提交，提交信息使用 `docs: 归档已完成 OpenSpec 变更`。
- 前置 P6-19 功能提交：`354407d feat: 增加字段标准检索`。

## 遗留边界

- 本轮只收口 OpenSpec 主规格和归档状态，不实现新的产品功能。
- 不重写历史 change 内容，不删除历史证据。
- 后续新功能仍应先从 `TODO.md` 选择待办，再创建或更新对应 OpenSpec change。
