## Context

DataSpec 已经保存了 AI job replay、SQL check record、AI batch run，并能即时生成 coverage report。它们各自适合页面展示或单功能回放，但还没有一个统一交付物来回答“本次 AI 做了什么、依据哪版标准、输出了什么、验证结果是什么、下一步怎么做”。P6-38 第一版需要把这些已有结果组合成只读 evidence package，供用户下载、复制和交给下游 AI。

约束：

- 项目优先个人/小团队使用，不引入审批流、对象存储或审计平台。
- 证据包必须默认脱敏，不保存或导出 token、password、Authorization、完整 JDBC URL、原始业务数据行。
- 覆盖率报告当前不持久化，第一版允许前端把当前 report summary 作为输入生成证据包。
- zip 只是即时响应，不长期保存。

## Goals / Non-Goals

**Goals:**

- 定义稳定 `AiEvidencePackage` JSON 契约，包含 source、standardSnapshot、inputsSummary、outputsSummary、validationSummary、artifacts、nextActions 和 suggestedCommands。
- 后端提供只读生成 API，支持 JSON 和 zip 两种输出。
- 支持四类来源：AI job、SQL check record、coverage report payload、AI batch run。
- 前端在 SQL 校验记录、覆盖率报告和 AI 批量任务等高频结果里提供复制 JSON / 下载 zip 的最小入口。
- CLI/MCP 能以机器可读方式导出 evidence package。
- 测试覆盖稳定字段和脱敏边界。

**Non-Goals:**

- 不新增证据包数据库表，不做长期归档或对象存储。
- 不实现企业审计、审批、签名、防篡改或权限模型。
- 不回填历史记录的 evidence package。
- 不自动读取业务仓库或上传第三方。
- 不承诺覆盖所有业务数据源；第一版只使用 DataSpec 已有安全元数据或调用方传入的脱敏 summary。

## Decisions

1. **使用即时生成服务而不是落库。**
   - 做法：新增 `AiEvidencePackageService`，按 sourceType/sourceId 或 payload 读取现有记录并生成 DTO。
   - 理由：P6-38 要的是交付物，不是审计系统；即时生成避免迁移和生命周期管理。
   - 备选：新增 `ds_evidence_package` 表。暂不采用，容易偏向长期归档平台。

2. **覆盖率报告走 payload source。**
   - 做法：`sourceType=COVERAGE_REPORT` 时请求体允许携带当前 coverage summary/tables/rankings 的脱敏摘要；后端负责再清洗、裁剪和打包。
   - 理由：coverage report 当前是即时计算结果，没有可靠 id；为它新建持久化会扩大范围。
   - 备选：先只支持 SQL check。暂不采用，因为 P6-38 明确要求覆盖率报告能交付。

3. **JSON 和 zip 共用同一包模型。**
   - 做法：JSON API 返回 `AiEvidencePackage`；zip API 内含 `evidence.json`、`summary.md`、`README.md`，其中 JSON 与 API 返回同源。
   - 理由：避免两套逻辑漂移，测试也可复用。

4. **脱敏使用白名单 + sanitizer。**
   - 做法：证据包只输出摘要和稳定字段；对自由文本、错误、命令、payload JSON 再做敏感模式替换。
   - 理由：来源记录可能包含 SQL、错误或连接描述，必须避免泄漏密码、token、完整连接串和业务数据行。

5. **CLI/MCP 只读，不自动执行修复。**
   - 做法：CLI `evidence export` 读取 API 并输出 JSON 或 zip 文件；MCP tool 返回 structuredContent 和 JSON text。
   - 理由：证据包是交付和续跑上下文，不是任务调度或写入确认。

## Risks / Trade-offs

- [Risk] 证据包内容过大。→ Mitigation：默认只包含 summary、counts、关键 artifacts 和少量 diagnostics；明细数组裁剪并标记 truncated。
- [Risk] payload 来源携带敏感文本。→ Mitigation：后端统一 sanitizer，测试覆盖 token/password/JDBC URL/Authorization。
- [Risk] 即时 coverage report 不可复现。→ Mitigation：包里标记 `source.persisted=false`，并保留生成时间、项目 id、标准快照摘要和建议命令。
- [Risk] 多来源字段不一致。→ Mitigation：统一 `sourceType/sourceId/sourceTitle/status`，来源特有信息放入 `artifacts[]` 或 summary。

## Migration Plan

1. 新增 evidence DTO/service/controller 和脱敏测试。
2. 接入 SQL check、AI job、AI batch run 和 coverage payload source。
3. 新增 CLI/MCP 读取入口和 Node tests。
4. 新增前端 API/types/smoke 和最小按钮入口。
5. 更新 README、docs/ai-contracts.md、TODO 和 OpenSpec specs。
6. 运行 `mvn test`、`pnpm test`、`pnpm build`、`node --test`、`openspec validate --all` 和 `git diff --check`。
