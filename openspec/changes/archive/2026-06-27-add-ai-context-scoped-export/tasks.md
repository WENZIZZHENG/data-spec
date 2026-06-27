## 1. OpenSpec 与测试

- [x] 1.1 新增 `add-ai-context-scoped-export` OpenSpec artifacts 并通过校验。
- [x] 1.2 新增后端裁剪字段目录、zip manifest 元数据和 Controller 参数测试。
- [x] 1.3 新增 CLI/MCP 裁剪参数测试。
- [x] 1.4 新增前端裁剪参数构造或页面行为测试。

## 2. 后端实现

- [x] 2.1 新增 AI Context 裁剪请求/摘要模型。
- [x] 2.2 扩展字段目录导出，支持 `scope/query/status/limit` 过滤并输出 `contextScope`。
- [x] 2.3 扩展 zip 包导出和 manifest/README，说明完整包与按需包使用时机。
- [x] 2.4 扩展 Controller 预览、下载和 zip 接口可选参数。

## 3. CLI / MCP / 前端

- [x] 3.1 `dataspec export-context` 增加裁剪参数并拼接下载 URL。
- [x] 3.2 MCP `get_field_catalog` 支持裁剪参数，新增 `search_field_catalog` tool。
- [x] 3.3 AI Context 前端页面增加裁剪条件、预览和按需下载。

## 4. 文档、验证与收尾

- [x] 4.1 更新 README 的按需 AI Context 用法。
- [x] 4.2 更新 TODO，将 P6-7 标记为已完成并指向 P6-8。
- [x] 4.3 运行后端、前端、工具测试与 OpenSpec 校验。
- [x] 4.4 进行直接代码评审并修复发现问题。
- [x] 4.5 创建本地 commit 后继续下一个待办。
