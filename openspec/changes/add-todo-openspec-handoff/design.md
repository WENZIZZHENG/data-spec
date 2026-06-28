## Context

TODO.md 中的 P6 条目已经有稳定字段：状态、为什么做、已有基础、缺口、落地产物、验收标准、边界，部分条目还有参考项目。OpenSpec change 则需要 proposal、design、spec 和 tasks 四类文件。当前转换主要靠人工复制，容易丢失边界或把待办直接当成已确认实现方案。

## Goals / Non-Goals

**Goals:**
- 从指定 TODO 条目解析结构化字段。
- 生成可被 `openspec validate` 基础校验通过的 OpenSpec change 草稿目录。
- 输出 change id、capability id、生成文件、人工确认问题和建议验证命令。
- 通过单测锁定字段保留、命名、草稿结构和 dry-run 行为。

**Non-Goals:**
- 不自动实现生成的 change。
- 不自动归档 change。
- 不调用 LLM 扩写需求。
- 不保证中文标题能推断出完美英文 change id；允许 `--change` 显式覆盖。
- 不修改 TODO 原文，P6-47 自身完成状态除外。

## Decisions

1. **新增独立 Node 脚本。**
   - 选择：`tools/dataspec-todo-openspec-handoff.mjs`。
   - 原因：这是仓库开发辅助工具，不需要后端服务，也不应塞进业务仓库 CLI。

2. **默认生成 repo-local OpenSpec 目录结构。**
   - 输出 `.openspec.yaml`、`proposal.md`、`design.md`、`specs/<capability>/spec.md`、`tasks.md`。
   - `.openspec.yaml` 使用 `schema: spec-driven`、`id` 和 `status: proposed`，保持与现有 OpenSpec change 一致。

3. **规则化但保守地拆分内容。**
   - proposal 保留为什么做、变更摘要、能力和影响。
   - design 保留背景、目标、非目标、风险和 Open Questions。
   - spec 生成少量可测试 requirement，不把待办缺口扩写成完整业务设计。
   - tasks 包含 OpenSpec 校验、人工确认、测试先行、实现、文档、验证、评审、提交归档。

4. **人工确认问题是输出契约的一部分。**
   - 当 TODO 缺少缺口、验收或边界等关键字段时，脚本在 `openQuestions` 中列出。
   - 即使字段齐全，也提示确认生成的 capability/change id 和不做边界。

## Risks / Trade-offs

- [Risk] 自动 slug 不够语义化 → Mitigation：允许 `--change` 和 `--capability` 覆盖；默认 fallback 使用 `p6-xx-todo-handoff`。
- [Risk] 草稿过度具体化导致误导实现 → Mitigation：只保留 TODO 原文和保守任务，不生成 API/DB 细节。
- [Risk] OpenSpec validate 对临时输出目录不可用 → Mitigation：默认输出到 `openspec/changes/<change>`；测试使用 temp dir 验证文件结构和内容，真实仓库用 OpenSpec CLI 校验。
- [Risk] 重复覆盖已有 change → Mitigation：默认拒绝覆盖，显式 `--force` 才重写。
