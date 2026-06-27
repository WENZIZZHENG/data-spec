## Context

当前规则配置以 `ds_rule_config` 的单条规则为核心，后端已有 `RuleConfigService`、规则 CRUD、项目初始化内置标准、规则配置页和 AI Context `rules.yaml`。这些能力能维护规则，但不能把一组规则作为“可选择、可复制、可回放”的基线套件来管理。P6-23 的第一版需要服务个人/小团队和 AI agent：给新项目一个稳定起点，也让 AI 能知道规则来自哪套基线。

## Goals / Non-Goals

**Goals:**

- 提供内置规则基线套件：`personal_default`、`strict`、`legacy_compatible`。
- 支持查询模板库、应用基线、导出项目规则基线、导入基线 JSON。
- 保存项目当前基线元数据，并在 AI Context `rules.yaml` 暴露 baseline name/version/source/appliedAt。
- 规则应用默认只新增缺失规则，不覆盖已有规则；显式 `overwrite=true` 时才更新同 code 规则。
- 前端规则配置页提供完整入口，并保持当前项目边界。

**Non-Goals:**

- 不做组织级发布、审批、订阅或强制统一规则。
- 不自动覆盖用户已调整的规则。
- 不引入外部规则 DSL，不重写现有 lint rule 引擎。
- 不把规则基线和标准快照合并成同一个生命周期；第一版只在 AI Context 中同时暴露二者。

## Decisions

### 1. 使用内置代码模板 + 轻量项目基线记录

内置模板以 Java 常量/配置类提供，包含 key、name、version、description、ruleCount、rules。项目当前基线使用新表 `ds_rule_baseline` 记录 `project_id`、`baseline_key`、`baseline_name`、`baseline_version`、`source`、`applied_at`、`rules_json`。这样能让 AI Context 和前端快速读取基线元数据，也能保留导入基线的原始规则包。

备选方案是只根据现有规则实时推断基线，但无法可靠区分“来自严格基线后被用户微调”和“手工配置成类似严格基线”，也不利于导出/导入回放。

### 2. 应用基线复用现有规则写入路径

`RuleBaselineService.applyBaseline(projectId, baselineKey, overwrite)` 会读取模板规则并调用/复用 `RuleConfigRepository` 的按 code 查询、insert、update 能力。默认 `overwrite=false` 时只插入缺失规则并返回 `created/updated/skipped` 统计；`overwrite=true` 时才更新已有同 code 规则。

这个选择保证现有 lint、规则配置页、AI rules export 不需要理解另一套规则存储模型。

### 3. 导入/导出使用稳定 JSON 契约

导出结果包含 `schemaVersion`、`baseline`、`rules`、`exportedAt`，规则字段与 `RuleConfig` 兼容但不包含数据库 id。导入时校验 `projectId` 由请求参数提供，不信任 JSON 内的项目 ID；导入来源记录为 `imported`。

### 4. AI Context 只新增 baseline metadata，不改变现有字段

`rules.yaml` 在 `standard:` 和 `naming:` 之外新增 `baseline:` 节点，至少包含 `key`、`name`、`version`、`source`、`applied_at`、`rule_count`。没有基线记录时输出 `key: custom` 和 `source: inferred`，避免破坏旧项目。

## Risks / Trade-offs

- [Risk] 导入 JSON 可能覆盖用户规则。→ 默认不覆盖，显式 overwrite 才更新；响应返回 skipped/updated 明细。
- [Risk] 内置模板和实际 lint rule code 漂移。→ 为模板中的 ruleCode 增加单测，确保规则代码存在且 paramsJson 可解析。
- [Risk] 项目基线记录与规则实际状态不一致。→ 基线记录只表达“最近一次应用/导入来源”，AI Context 同时输出实际规则列表；后续可用 drift 检查增强。
- [Risk] `rules.yaml` 新字段影响已有 AI 消费。→ 只做新增字段，保留原有 `standard`、`naming` 和 `rules` 结构。

## Migration Plan

- 新增 Flyway `V13__add_rule_baseline.sql`，创建 `ds_rule_baseline`，按 `project_id` 唯一。
- 老项目无记录时视为 `custom/inferred`，不需要数据回填。
- 回滚时可删除新增 API/服务并保留规则配置；基线表只是元数据，不影响现有 lint。
