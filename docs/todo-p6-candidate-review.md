# DataSpec P6 候选价值评审

更新时间：2026-07-14

本文件记录 2026-07-12 对 P6 候选的第二轮收束和第三轮功能方向复评。评审口径是个人 / 小团队快速使用，并优先服务 AI 稳定理解、生成、导入、校验和维护数据字段标准。

## 评审证据

- 独立只读评审子 agent：`019f5504-90ac-7212-8b71-7720c0999bf5`。
- 用途：逐项检查 60 个候选，结合 TODO、完成/删除归档、OpenSpec 主规格和现有实现判断近期做、保留、合并或删除。
- 生命周期：已完成并关闭；未修改文件、未运行测试。
- 关键核对规格：`ai-task-runs`、`ai-session-bootstrap`、`standard-health-trends`、`standard-change-what-if`、`standard-candidate-inbox`、`standard-maintenance-workflows`、`cli-mcp-contract-fixtures`、`synthetic-standard-examples`。
- 第三轮本地差距评审 agent：`019f5681-8513-7992-8210-67b8479ede7f`；核对命名分词、缩写、历史别名、AI post-check、PR review 和 evidence 实现，已完成并关闭。
- 第三轮 GitHub 对标 agent：`019f5681-98f3-77c0-8108-d3b1b92df586`；核对 DataHub、OpenMetadata、DataContract CLI、ODCS、SQLFluff、Soda Core、jieba 和 PR-Agent，已完成并关闭。
- 第三轮关键事实：中文当前是整段子串匹配而非词典分词；历史别名和 evidence claim 验证未达到主规格；PR review、post-check 和 Evidence Package 尚未形成共享 finding 闭环。
- 2026-07-13 进度复核：`P6-190` 已补齐历史别名和 evidence claim 真实性并归档，相关事实缺口关闭；命名解析和统一 Finding/Evidence 仍由 `P6-189`、`P6-191` 承接。
- 2026-07-14 进度复核：`P6-189`、`P6-120`、`P6-86`、`P6-191`、`P6-137` 和 `P6-111` 已依次完成；近期开发只剩 `P6-85`。

## 当前结论

- 第二轮基线仍为 60 个原候选：36 个原编号合并保留，7 项由现有能力覆盖，17 项删除或等待触发；原文保存在 [历史快照](archive/todo-p6-candidates-2026-07-12.md)。
- 第三轮新增 `P6-189` 到 `P6-191`，分别承接命名解析与缩写治理、既有契约真实性修复、Finding/Evidence 与 AI/PR 评审闭环。
- 完成 `P6-111` 后剩余 3 个实施主题，结构为 1 个近期主题和 2 个条件主题；共承接 15 个原候选编号。
- 近期队列只剩 `P6-85`，约 3-5 个工作日；条件主题只在真实标准包升级或消费仓库出现后增加约 5-8 个工作日。

## 近期值得做

| 顺序 | 主题 | 承接编号 | 价值判断 | 估算 |
| --- | --- | --- | --- | --- |
| 1 | 确定性数标命名解析与缩写治理（已完成） | P6-189 | 一次增强推荐、检索、标准问答、候选和 AI Context，且可复用现有 glossary | 3-5 天 |
| 2 | 推荐质量与 AI 场景回归（已完成） | P6-120、P6-126、P6-133、P6-162 | 防止命名评分和关键 AI 链路静默退化，不新增回放框架 | 1-2 天 |
| 3 | 字段库性能与密集操作（已完成） | P6-86、P6-122、P6-145、P6-151 | 已有全量加载和固定结果上限的代码证据，直接影响性能与结果可达性 | 2-3 天 |
| 4 | Finding/Evidence 与 AI/PR 评审闭环（已完成） | P6-191 | 统一现有 lint、post-check、PR 评论和证据包，不重造 Reviewer | 3-5 天 |
| 5 | `.dataspec/config.json` Schema 与运行摘要（已完成） | P6-101、P6-103、P6-118、P6-119、P6-137、P6-173 | 低成本补稳定配置入口，并吸收重复的独立运行指纹主题 | 0.5-1 天 |
| 6 | 标准候选来源管道（已完成） | P6-110、P6-111、P6-112、P6-113、P6-124、P6-136、P6-142 | 第一版只接未知词/歧义缩写等一个证据充分来源 | 2-3 天 |
| 7 | 演示项目 dry-run 清理与重建（待开发） | P6-85 | 补本地维护闭环，但必须保留事务、预览和安全配置边界 | 3-5 天 |

## 保留但暂缓

| 主题 | 承接编号 | 启动条件 | 估算 |
| --- | --- | --- | --- |
| 标准变更 diff、发布说明、decision note 与迁移预演 | P6-74、P6-80、P6-98、P6-104、P6-108、P6-117、P6-139、P6-160 | 真实标准包升级无法由现有 What-if/Schema Plan/Patch Plan 解释 | 2-3 天 |
| 标准消费 Schema、Contract-as-Code 与轻量锁定 | P6-123、P6-127、P6-129、P6-149、P6-154、P6-174 | 至少一个真实消费仓库明确目标协议，或多仓发生版本漂移 | 3-5 天 |

## 合并调整

| 原独立主题 | 处理结论 | 原因 |
| --- | --- | --- |
| P6-101 环境运行指纹与漂移诊断 | 并入 P6-137 | doctor、版本握手、apiSchemaHash、specHash 和 bootstrap 已覆盖主体，只需补配置 schema/version 摘要 |
| P6-104 标准决策理由与字段 ADR | 并入 P6-74 | 仅需在统一变更说明中增加 decision note，无需独立 ADR 平台 |
| P6-129 标准包 Lockfile 与同步漂移 | 并入 P6-123 | lock 应随首个真实消费格式和仓库一起设计，避免先造无消费者协议 |

## 现有能力覆盖

以下编号的核心目标已由现有能力覆盖，已补入 [P5/P6 完成归档](archive/todo-completed-p5-p6.md)：

- `P6-109`、`P6-135`：capability catalog、session bootstrap、task profile、write safety 和 OpenSpec readiness 已覆盖能力边界与 preflight。
- `P6-131`：synthetic examples 与标准测试数据包已提供不读取真实业务行的安全样例。
- `P6-143`：`dataspec init`、AI session bootstrap 和 MCP guidance pack 已组成 Agent 启动包。
- `P6-146`：Standard Health 已提供健康快照、趋势和下一步改进计划。
- `P6-156`：CLI/MCP 契约 fixture、示例和 drift check 已稳定覆盖工具契约。
- `P6-159`：AI task run 已提供状态、失败步骤、幂等、重试和恢复命令。

## 删除或等待外部触发

以下编号已移入 [删除 / 不做归档](archive/todo-removed-p6-candidates.md)：

- 等真实规模或外部依赖后再提：`P6-95`、`P6-96`、`P6-102`、`P6-138`、`P6-157`、`P6-163`、`P6-168`、`P6-171`。
- 当前收益不足或明显重复：`P6-116`、`P6-121`、`P6-130`、`P6-134`、`P6-140`、`P6-141`、`P6-144`、`P6-150`、`P6-155`。

这些编号不是永久禁止；只有归档中记录的恢复条件出现时，才重新提出更小、更可验证的任务。

## GitHub 对标取舍

- [jieba](https://github.com/fxsjy/jieba)：只借鉴自定义词典和确定性最长匹配，不引入 Python 运行时或把自然语言分词直接当标准名。
- [SQLFluff](https://github.com/sqlfluff/sqlfluff)：借鉴稳定规则编码、位置、fix 和 warning 语义，不替换现有 JSqlParser 链路。
- [PR-Agent](https://github.com/The-PR-Agent/pr-agent)：借鉴受约束 finding、最大问题数和允许空结果，不采用 PR 总分或无证据 LLM finding。
- [DataHub](https://github.com/datahub-project/datahub) 与 [OpenMetadata](https://github.com/open-metadata/OpenMetadata)：只借鉴术语来源归因和 typed relation，不建设 Kafka、图数据库、海量 connector 或企业治理平台。
- [DataContract CLI](https://github.com/datacontract/datacontract-cli) 与 [ODCS](https://github.com/bitol-io/open-data-contract-standard)：只在真实消费仓库出现后借鉴 lint/test/export 和 schema 结构。
- [Soda Core](https://github.com/sodadata/soda-core)：只借鉴检查定义与结果分离，不把 DataSpec 扩成真实数据扫描和可观测平台。

## 收束原则

1. 不再按 P6 编号顺序自动开发；近期只推进 `P6-85`，两个条件主题等待真实触发。
2. 合并项只是主题的可选阶段，不代表第一版一次性实现全部编号。
3. 未来想法优先补到现有主题；无法承接且有真实证据时再新增编号。
4. 不新增第二套词典、搜索 API、PR reviewer 或 evidence 格式；优先扩展现有 glossary、Standard Query、`review-pr` 和 Evidence Package。
5. 开工前按任务风险决定快速、常规或 OpenSpec 流程，完成后同步候选池、估算和归档。
