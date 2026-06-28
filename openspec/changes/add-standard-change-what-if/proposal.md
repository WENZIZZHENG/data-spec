## Why

字段、规则、模板或分组变更会影响 AI Context、DDL 生成、SQL lint 和字段推荐。当前字段页面已经有影响分析和变更日志，但缺少统一 what-if 契约，AI/前端无法在保存前稳定拿到 diff、影响范围、验证命令和回退入口。P6-41 第一版聚焦个人/小团队高频的字段更新与规则更新/启停，补齐保存前预览和保存后回退辅助。

## What Changes

- 新增标准变更 what-if 预览服务，返回目标对象、属性 diff、影响摘要、风险等级、建议验证命令、回退提示和当前快照信息。
- 字段更新预览复用现有字段影响分析，补充本次将修改的属性、需要关注的影响和跳转变更日志/快照的提示。
- 规则更新和启停预览基于规则配置 diff，输出对 SQL lint、AI Context 和规则基线的影响提示。
- 前端字段编辑保存前使用统一 preview 结果确认；规则编辑和启停保存前也展示 what-if 摘要。
- 增加后端单测和前端显示工具测试，继续通过 `mvn test`、`pnpm test`、`pnpm build`、OpenSpec 校验和 `git diff --check`。

## Capabilities

### New Capabilities

- `standard-change-what-if`: DataSpec 提供标准变更保存前预览和回退辅助契约。

### Modified Capabilities

- `field-impact-analysis`: 字段影响分析可作为字段变更 what-if 的影响来源。
- `rule-config-experience`: 规则编辑和启停支持保存前 what-if 预览。
- `change-log`: 变更日志可作为保存后的回退入口提示。
- `standard-snapshot-versioning`: what-if 预览会暴露当前标准快照，提示是否需要新建快照。

## Impact

- 后端：新增 `standardchange` 包；新增只读 preview controller/service/model，不改字段/规则写入语义。
- 前端：字段库、规则配置页保存前增加统一确认摘要；已有影响/变更入口继续保留。
- 测试：新增服务单测、controller 单测和前端 utility 测试；不引入数据库迁移。
- 边界：不做强审批流，不自动回滚数据库，不自动创建快照；第一版只覆盖字段更新和规则更新/启停。

## Verification Evidence

- 后端：`mvn test` 通过，324 tests，0 failures，0 errors。
- 前端：`pnpm test` 通过，87 tests；`pnpm build` 通过，保留既有 Rolldown/VueUse pure annotation warning。
- OpenSpec：`npx.cmd openspec validate --all` 通过，78 items。
- Diff 检查：`git diff --check` 退出码 0，仅有既有 CRLF 换行提示。
- 本地结构化代码评审：重点检查只读性、项目边界、预览失败降级、规则启停确认和 schema 手动补丁一致性；发现并修复规则预览缺少 `ruleId` 空值显式校验和一个未用 import。
