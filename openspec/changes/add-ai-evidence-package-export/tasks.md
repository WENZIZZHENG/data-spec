## 1. OpenSpec 与范围确认

- [x] 1.1 创建 P6-38 proposal、design、delta specs 和 tasks。
- [x] 1.2 通过 OpenSpec change 校验。

## 2. 后端 Evidence Package

- [x] 2.1 新增 evidence DTO、sourceType 枚举、request/response 模型和 service/controller，提供 JSON 与 zip 导出 API。
- [x] 2.2 实现 SQL_CHECK、AI_JOB、AI_BATCH_RUN 和 COVERAGE_REPORT 四类 source 聚合。
- [x] 2.3 实现 evidence sanitizer，覆盖 token、password、Authorization、完整 JDBC URL 和自由文本 diagnostics。
- [x] 2.4 zip 输出包含 `evidence.json`、`summary.md` 和 `README.md`，并复用同一 JSON package。
- [x] 2.5 补后端单测，覆盖四类 source、zip 内容、稳定字段和敏感信息脱敏。

## 3. CLI、MCP 与前端入口

- [x] 3.1 CLI 新增 `evidence export`，支持 json/zip、sourceType/sourceId/payload 和安全 output path。
- [x] 3.2 MCP 新增 `export_evidence_package` tool，并在 prompts 中提示交付前导出 evidence package。
- [x] 3.3 前端新增 evidence API/types，SQL 校验记录、覆盖率报告和 AI batch 页面提供复制 JSON / 下载 zip 的最小入口。
- [x] 3.4 补 Node CLI/MCP 测试和前端 smoke，覆盖稳定字段、zip 输出和敏感信息不泄漏。

## 4. 文档、验证与收尾

- [x] 4.1 更新 README、TODO 和 docs/ai-contracts.md，说明 evidence package、脱敏边界和非审计边界。
- [x] 4.2 执行后端、前端、CLI/MCP、OpenSpec 和 diff 验证。
- [x] 4.3 完成结构化代码评审并修复 findings。
- [ ] 4.4 创建本地 commit。
- [ ] 4.5 归档 OpenSpec change 并再次验证。
