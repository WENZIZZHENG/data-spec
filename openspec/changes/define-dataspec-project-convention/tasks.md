## 1. OpenSpec

- [x] 1.1 新增 P2-3 proposal/design/tasks/spec delta。
- [x] 1.2 运行 `openspec validate define-dataspec-project-convention --strict`。

## 2. 后端实现

- [x] 2.1 扩展 AI Context 包，新增 `.dataspec/manifest.json`。
- [x] 2.2 扩展 AI Context 包，新增 `.dataspec/README.md`。
- [x] 2.3 更新 `AGENTS.md.fragment`，说明 manifest 与 lint 命令入口。
- [x] 2.4 扩展 `AiContextExportServiceTest` 覆盖新增约定文件。

## 3. 文档与验证

- [x] 3.1 更新 TODO.md P2-3 状态。
- [x] 3.2 运行后端测试、OpenSpec validate 和 diff 空白检查。
- [x] 3.3 直接代码评审，不使用子 agent。
- [x] 3.4 修复评审发现后创建本地 commit。
