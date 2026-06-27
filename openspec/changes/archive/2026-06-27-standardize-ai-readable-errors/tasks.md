## 1. OpenSpec

- [x] 1.1 创建 P6-18 OpenSpec change，并通过 `openspec validate standardize-ai-readable-errors`。

## 2. 后端错误契约

- [x] 2.1 新增 AI 可读错误诊断模型与分类器，覆盖 token、projectId、权限、资源不存在、SQL、数据库、校验和内部错误。
- [x] 2.2 扩展统一响应 `R.fail`，在失败响应中追加可选 `error`，保持 `code/message/data` 兼容。
- [x] 2.3 新增后端单测，锁定 error 字段和高频分类。

## 3. CLI/MCP 透传

- [x] 3.1 CLI 解析后端 `error` 并在 stderr 输出 `DataSpecError` JSON 诊断行。
- [x] 3.2 MCP 将后端 `error` 放入 JSON-RPC `error.data.dataspecError`。
- [x] 3.3 新增 CLI/MCP 单测覆盖结构化错误。

## 4. 文档与待办

- [x] 4.1 更新 README，说明错误诊断字段、CLI/MCP 读取方式和边界。
- [x] 4.2 更新 TODO，将 P6-18 标记为已完成第一版并推进下一步顺序。

## 5. 验证、评审与提交

- [x] 5.1 运行后端目标测试、CLI/MCP 单测、OpenSpec validate 和 `git diff --check`。
- [x] 5.2 进行直接代码评审，不使用子 agent；修复 findings 或记录暂不处理理由。
- [x] 5.3 创建本地 commit 后继续下一个待办。
