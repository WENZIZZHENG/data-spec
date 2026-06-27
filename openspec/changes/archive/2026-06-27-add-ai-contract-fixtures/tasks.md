## 1. OpenSpec 与契约范围

- [x] 1.1 新增 `add-ai-contract-fixtures` OpenSpec proposal/design/spec/tasks 并通过基础校验。
- [x] 1.2 梳理第一版稳定 AI contract 字段清单，限定在 AI Context、lint/fixedSql、字段推荐、DDL 预览、CLI/MCP 输出。

## 2. 后端 AI Contract Fixtures

- [x] 2.1 新增后端 AI contract 测试，校验 `field-catalog.json`、`rules.yaml`、manifest 和 workflows 的稳定字段路径。
- [x] 2.2 新增 lint/fixedSql 契约测试，校验 `LintResult`、`LintIssue`、source range、suggestion/replacement 和 `fixedSql`。
- [x] 2.3 新增字段推荐与 DDL 预览契约测试，校验推荐结果和 `lintResult` 关键字段。
- [x] 2.4 将契约测试接入现有 `mvn test`，避免只靠手动快照。

## 3. CLI/MCP AI Contract Fixtures

- [x] 3.1 扩展 CLI 单测，锁定 `lint`、`lint-files`、`workflow list/show`、`suggest-field`、`generate-ddl` JSON 关键字段。
- [x] 3.2 扩展 MCP 单测，锁定 resources/tools 的 `structuredContent`、`content[].text` 和 workflow resource 关键字段。
- [x] 3.3 确认兼容新增字段不会让测试依赖完整对象深等，避免过度冻结。

## 4. 文档、验证与收尾

- [x] 4.1 更新 README 和 TODO，将 P6-12 标记为已完成并指向 P6-13。
- [x] 4.2 运行 `mvn test`、`node --test`、OpenSpec validate 和 `git diff --check`。
- [x] 4.3 进行直接代码评审并修复发现问题。
- [x] 4.4 创建本地 commit 后继续下一个待办。
