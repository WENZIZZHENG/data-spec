# ai-task-recommendation-queue Specification

## Purpose
定义 DataSpec 如何基于已有诊断信号生成只读 AI 任务推荐队列，帮助用户把字段治理、候选处理和质量修复转成可执行的安全任务卡。
## Requirements
### Requirement: AI 任务推荐队列
系统 SHALL 为指定项目生成只读 AI 任务推荐队列，基于已有诊断信号输出 3 到 8 个推荐任务卡，并按优先级从高到低排序。

#### Scenario: 生成项目推荐任务卡
- **WHEN** 调用方请求 `GET /api/ai-task-recommendations?projectId=<id>`
- **THEN** 系统返回项目级 `summary` 与任务级 `items`，每个任务卡包含 `taskType`、`priority`、`title`、`reason`、`targetRoute`、`recommendedCommand`、`evidenceRefs` 和 `completionCheck`

#### Scenario: 不同诊断信号生成不同任务
- **WHEN** 项目存在高风险热区字段、待处理候选或质量门禁失败项
- **THEN** 系统 SHALL 生成对应任务卡，并在 `evidenceRefs` 中引用来源能力和摘要计数

### Requirement: 只读与安全任务摘要
系统 SHALL 只推荐 DataSpec 已有能力或安全 dry-run 入口，不自动执行写操作，并且响应不得包含 SQL 原文、AI 原始输入输出、候选 raw evidence、JDBC URL、DSN、token、password 或 Authorization。

#### Scenario: 推荐任务不执行写入
- **WHEN** 推荐队列包含候选采纳、字段质量修复或热区治理任务
- **THEN** 系统只返回页面路由、命令模板和完成判定，不创建、修改、采纳或删除任何字段标准

#### Scenario: 不暴露原始诊断内容
- **WHEN** 来源诊断中存在敏感 SQL、AI payload 或候选 evidence
- **THEN** 系统只返回脱敏摘要、计数、来源类型和下一步说明，不返回原始内容
