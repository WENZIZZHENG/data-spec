# P6-57 反向导入字段映射策略与确认理由

## Why

数据库直连反向导入已经能预览候选、确认导入、记录批次和字段来源，但缺少“每个真实字段为什么被匹配、导入、跳过或忽略”的结构化记录。用户和 AI 后续复盘时只能看到结果，无法稳定判断 `mobile_no` 是别名命中标准手机号字段、作为新候选导入，还是被本次人工忽略。

## What Changes

- 在反向导入预览中输出字段级 mapping decision：包含 `decisionType`、`matchedFieldId`、`matchedFieldName`、`matchReason`、`confidence`、`ignoreReason` 和 `confirmReason`。
- 确认导入时记录本批次最终决策：导入、跳过已存在、忽略未选候选，并返回 `batchId` 与决策列表。
- 新增轻量持久表保存 reverse import mapping decision，供前端和 AI 查询批次历史。
- 前端数据库直连预览页允许为候选填写确认理由；未选择候选会以默认忽略理由提交，导入结果展示本批次决策摘要。

## Non-Goals

- 不实现审批流、发布流或多人审核。
- 不自动覆盖已有标准字段定义。
- 不要求每个字段人工填写长说明；第一版提供默认理由，用户可改。
- 不把 SQL 文本反向导入强制改成数据库批次流程。

## SDD Level

`full`。本次涉及数据库迁移、持久化模型、后端 API 契约和前端确认流程，必须有设计说明、spec delta、测试和 OpenSpec 验证。

## User Impact

- 用户确认数据库反向导入时，可以给字段候选填写简短确认理由。
- 导入完成后能看到本次哪些字段被导入、哪些因已存在被跳过、哪些是本次忽略。
- AI 可以读取结构化 mapping history，减少重复建议同一批次已忽略或已映射字段。
