## 1. 共享 Finding 契约

- [x] 1.1 新增带完整字段说明、边界校验和脱敏规则的 ReviewFinding、subject、location、waiver 与枚举模型
- [x] 1.2 为 legacy lint issue 与 AI post-check issue 增加兼容适配器，并覆盖空 findings、稳定 findingKey 和去重测试

## 2. 后端闭环

- [x] 2.1 扩展 LintResult，在 SQL check 保存后回填 sqlCheckRecordId、canonical evidence ref 和 suppression waiver
- [x] 2.2 扩展 AI output post-check 请求/结果，校验外部 findings 的数量、字段和 project-scoped evidence refs
- [x] 2.3 扩展 Evidence Package 请求/结果，从 SQL check 派生 findings，并拒绝未通过 post-check 或 evidence 复验的外部 finding
- [x] 2.4 同步 OpenAPI 字段说明并补齐后端成功、失败、跨项目、脱敏和兼容性测试

## 3. CLI、MCP 与 GitHub 交付

- [x] 3.1 为 ai-output check 增加 --findings JSON 文件输入、前置边界校验和兼容输出
- [x] 3.2 为 MCP check_ai_output 增加可选 findings schema、字段说明和 handler 转发
- [x] 3.3 扩展 review-pr JSON delivery envelope，保留 commit SHA、评论 URL、findings、SQL check IDs、post-check 和 evidence package 入口
- [x] 3.4 更新 CLI/MCP contract fixtures、GitHub API fixtures 和回归测试，覆盖旧后端 fallback、空 finding 与 URL 缺失

## 4. 文档与质量门禁

- [x] 4.1 更新 README、AI 契约和受影响主规格，说明共享 Finding、evidence gating、dry-run 与兼容边界
- [x] 4.2 运行后端、前端、tools、OpenSpec strict/all、状态检查和 diff/secrets 门禁，并记录 Verification Evidence
- [x] 4.3 启动独立只读评审子 agent，修复或记录全部 findings，并关闭 agent
- [x] 4.4 同步主规格、归档 change、更新 TODO/完成归档，执行归档后验证并创建本地 commit
