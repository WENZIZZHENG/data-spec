## Context

`buildValidationAdvice(inputPaths)` 通过静态规则把 `openspec/` 路径映射到 `openspec validate --all`。这对主规格、多 change 或 archive 变更是合理的，但对单个 active change 来说过重，也不贴合 `tasks.md` 中通常记录的 `openspec validate <change-id> --strict`。

## Goals / Non-Goals

**Goals:**

- 单个 active OpenSpec change 路径推荐 change strict 命令。
- 多 change、主规格和 archive 路径继续推荐 `openspec validate --all`。
- 保持 `openspec-validate` command id 不变，避免下游按 id 读取时失效。

**Non-Goals:**

- 不自动执行推荐命令。
- 不解析 OpenSpec artifacts 内容。
- 不改变非 OpenSpec 路径的推荐规则。

## Decisions

1. **复用 `openspec-validate` id，动态改 command/reason。**
   - 原因：id 是建议项的稳定标识，下游无需适配新增 id；命令文本是建议内容，可以按路径收窄。
   - 备选方案：新增 `openspec-change-validate` id。放弃原因是会让同一次 change 同时出现两个 OpenSpec validate 建议，去重和排序更复杂。

2. **只从 `openspec/changes/<change-id>/...` 提取 active change id。**
   - 原因：主规格和 archive 的合适校验范围不是单 change；用 `--all` 更保守。

## Risks / Trade-offs

- [Risk] 单 change strict 不能覆盖其他 active changes。→ Mitigation：仅在输入路径唯一指向一个 active change 时使用 strict；多 change 或其他 OpenSpec 路径回退 `--all`。
- [Risk] Windows 路径分隔符导致 change id 提取失败。→ Mitigation：沿用现有 `normalizePaths()` 把反斜杠统一为 `/`。
