## Context

DataSpec 当前已有项目、字段、枚举、规则、规则基线、模板、标准快照、反向导入来源和变更日志等资产，但导出能力分散在 AI Context、数据字典和 Excel 交换里。P6-24 需要给个人/小团队一个完整迁移入口：在本机、演示环境或轻量部署间移动项目标准资产，同时避免把源数据库数据行、密码、token 或完整连接串带进包里。

## Goals / Non-Goals

**Goals:**

- 导出单个项目的标准资产备份 JSON 包，稳定描述 schemaVersion、sourceProject、asset counts、specHash/packageHash 和脱敏结果。
- 支持恢复 dry-run，输出目标项目、兼容性、冲突、将创建/跳过/覆盖的资产和风险提示。
- 支持确认恢复，默认不覆盖已有资产；`overwrite=true` 时只更新可安全覆盖的同名/同编码资产。
- 保留恢复摘要，方便用户和 AI 复盘“恢复了什么、跳过了什么、哪些风险需要人工处理”。
- 复用现有写入路径和项目访问校验，避免绕过变更日志、项目边界和已有幂等约束。

**Non-Goals:**

- 不备份源数据库业务数据行，不做数据库物理备份。
- 不保存数据库密码、API token 明文、完整 JDBC URL 或浏览器本地敏感缓存。
- 不做跨版本无限兼容，不引入复杂迁移 DSL。
- 不做审批流、多人发布、定时备份或远程对象存储。
- 不自动删除目标项目已有资产。

## Decisions

### 1. 使用稳定 JSON 备份包，第一版不压缩

备份包直接以 JSON 作为 API body 和下载内容，包含 `schemaVersion`、`exportedAt`、`sourceProject`、`assets`、`counts`、`sanitization`、`packageHash`。JSON 对前端粘贴、AI 读取、单测 fixture 和 Git diff 都更友好。后续如包过大，可在不改变内部 schema 的情况下增加 zip 下载。

### 2. 恢复分为 dry-run 和 apply 两步

`dry-run` 只解析包并与目标项目资产比对，返回 `CREATE/SKIP/UPDATE/CONFLICT/BLOCKED` 明细，不写库。`apply` 必须显式调用，并复用同一套计划生成逻辑后执行。这样 AI 和用户都能先看风险，再决定是否恢复。

### 3. 默认创建新项目，指定 targetProjectId 时写入现有项目

如果请求没有 `targetProjectId`，恢复会创建一个新项目，名称来自备份包并在冲突时追加后缀。如果传入 `targetProjectId`，恢复写入该项目并执行项目访问校验。默认 `overwrite=false`，遇到同名字段、同编码枚举/规则、同名模板时跳过或标记冲突；显式覆盖时才更新可覆盖资产。

### 4. 资产恢复顺序按依赖拓扑执行

恢复顺序为项目 -> 数据域 -> 枚举字典/枚举值 -> 字段 -> 规则 -> 规则基线 -> 模板/模板字段 -> 标准快照 -> 来源批次/字段来源 -> 变更日志摘要。模板字段、字段来源等引用旧 ID 的资产通过备份包内稳定 key 重映射到新 ID。

### 5. 脱敏是导出契约的一部分

导出阶段不把 token、password、完整连接串、数据库业务数据行写入包。对于来源批次和连接相关上下文，只保留 databaseType、databaseName、schemaName、tableNames 和结构字段，输出 `sanitization.removedFields` 说明剔除了哪些字段类型。恢复阶段再次扫描包内字符串，发现疑似敏感字段时阻断或 warning。

### 6. 恢复记录只保存摘要

新增轻量 `ds_project_restore_record` 记录 projectId、packageHash、sourceProjectName、schemaVersion、dryRun、overwrite、created/updated/skipped/conflict counts、summaryJson、operator、createdAt。它不保存完整备份包，避免把历史敏感内容长期落库。

## Risks / Trade-offs

- [Risk] 资产依赖多，恢复顺序或 ID 映射容易漏。→ 第一版只覆盖标准资产的主路径，后端单测使用包含字段、枚举、规则、模板、快照和来源的包验证映射。
- [Risk] 误覆盖用户已有标准。→ 默认不覆盖，dry-run 明细先展示，apply 仍根据计划执行；不做删除。
- [Risk] 包 schema 后续演进导致旧包不可用。→ 使用 schemaVersion 和兼容性检查；不支持版本返回 `BLOCKED` 和可读建议。
- [Risk] 脱敏漏掉敏感字符串。→ 使用字段名黑名单和字符串模式双重扫描；包内出现 `password/token/jdbc` 等高风险键时阻断恢复。
- [Risk] 备份包过大。→ 第一版面向个人/小团队，使用 JSON；大项目可后续接入分页、zip 或离线缓存。

## Migration Plan

- 新增 Flyway 迁移创建恢复记录表；旧项目不需要回填。
- 新增 API 与前端入口后保持现有导出/导入能力不变。
- 回滚时可移除 API/前端入口，恢复记录表可保留为历史摘要，不影响核心标准资产。
