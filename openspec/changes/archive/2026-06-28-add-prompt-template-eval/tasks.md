## 1. Registry 与评测核心

- [x] 1.1 新增 Prompt 模板模型、registry 服务和稳定模板定义。
- [x] 1.2 新增 Prompt 模板评测服务，检查必备段落、必备短语和 golden diff。
- [x] 1.3 新增模板元数据与评测 API，保持现有 Prompt API 字符串响应不变。

## 2. 现有链路接入

- [x] 2.1 建表 Prompt、SQL 修正 Prompt 使用 registry 版本，并在 prompt 文本和 AI job 记录中携带版本。
- [x] 2.2 SQL lint/fixedSql 和 DDL preview 的 AI job 记录使用 registry 版本。
- [x] 2.3 生成或更新 prompt golden fixture，覆盖 create-table 和 fix-sql 样例。

## 3. 验证与文档

- [x] 3.1 增加 registry、评测和回放版本单元测试，并接入 `mvn test`。
- [x] 3.2 更新 README 与 TODO P6-31 状态，说明模板版本和评测入口。
- [x] 3.3 运行 `mvn test`、`node --test tools/prompt-template-eval.test.mjs`、`npx openspec validate add-prompt-template-eval` 和文档格式检查。
- [x] 3.4 完成结构化代码评审，修复或记录 findings 后提交。
