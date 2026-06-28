## Context

DataSpec 已有项目创建、内置基础 standards 初始化、规则基线、演示项目、字段/枚举/模板 CRUD 和 AI Context 导出。P6-42 要解决的是“新项目如何快速拥有可用领域标准”，让 AI 在订单、用户、支付、库存、审计等常见场景下不用从空字段库开始猜。

现有字段表没有独立来源列，但已有 `tags`、`category`、`exampleValue`、`status` 等 AI 元数据。第一版需要最小改动，不引入行业主数据平台，也不把 starter kit 设计成企业审批/发布流程。

## Goals / Non-Goals

**Goals:**
- 提供内置领域 starter kit 列表，覆盖用户账号、订单交易、支付金额、库存商品和审计日志等小型组合。
- 支持把 starter kit 幂等应用到项目，创建缺失字段、枚举和表模板，跳过已存在对象。
- 在字段标签和应用记录中标明 kit key/version，AI Context 可导出来源摘要。
- 前端项目创建和项目列表支持选择或应用 starter kit。
- 重复应用不覆盖用户修改，保持个人快速使用体验。

**Non-Goals:**
- 不追求行业全量数据模型，不做组织级模板仓库、审批、发布或权限流。
- 不自动删除、改名或覆盖已有字段/枚举/模板。
- 不引入外部依赖或外部 LLM。
- 不重写现有内置 standards 初始化和 demo project 逻辑。

## Decisions

1. **使用代码内置 Starter Kit Registry，而不是数据库管理模板。**
   - 原因：第一版 starter kit 是产品内置、版本随代码发布，适合测试和 AI Context 稳定导出。
   - 备选：新增可编辑数据库模板。暂不采用，因为会引入模板发布、校验和 UI 编辑复杂度。

2. **字段来源用 `tags` 标记，并新增安装记录表保存应用摘要。**
   - 原因：字段已有 tags，可直接暴露给 AI Context，避免为字段表新增多个来源列；安装记录表用于记录应用过的 kit、版本和 counts。
   - 备选：给字段/枚举/模板全部加 source columns。暂不采用，因为本轮只需要 AI 可读来源和重复安装防护。

3. **应用逻辑按项目内唯一键幂等跳过。**
   - 字段按 `projectId + name` 判断存在。
   - 枚举按 `projectId + code` 判断存在，枚举值按 value 跳过。
   - 模板按 `projectId + name` 判断存在，模板字段只在新建模板时写入，已有模板不覆盖。

4. **前端先在项目创建和项目列表提供入口。**
   - 项目创建后再调用 starter kit apply，避免改变 `Project` 基础创建契约过多。
   - 已存在项目提供“Starter Kit”操作，方便补装。

5. **AI Context 只做兼容性加字段。**
   - 字段已有 `tags` 时增加 `starterKitSources` 数组；没有来源时保持旧结构可用。
   - 不要求已有字段回填来源。

## Risks / Trade-offs

- [Risk] 用 tags 表达来源可能不如专用列强约束。→ Mitigation：安装记录表保留结构化 kitKey/version/counts；tags 仅作为字段级 AI 提示。
- [Risk] 重复应用跳过已有模板，可能不会补齐用户删除的部分模板字段。→ Mitigation：第一版明确不覆盖已有模板，结果返回 skipped counts 和 warnings。
- [Risk] 内置 kit 字段不可能覆盖所有业务差异。→ Mitigation：保持 kit 小而可组合，用户可继续编辑字段标准。
- [Risk] 项目创建后第二步应用 kit 失败，会产生只有基础标准的项目。→ Mitigation：前端提示失败并保留项目，用户可从项目列表重试应用。

## Migration Plan

- 新增 Flyway 迁移 `ds_starter_kit_installation`，仅记录应用摘要，不影响已有数据。
- 部署后旧项目不自动套用 starter kit。
- 回滚代码时安装记录表可保留，不影响现有字段/枚举/模板读写。
