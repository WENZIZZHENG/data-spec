## 1. 草稿确认

- [x] 1.1 人工确认 OpenSpec 草稿中的 change id、capability、验收标准和边界。
- [x] 1.2 补充或删除 Open Questions，确保需求可实施。

## 2. 测试先行

- [x] 2.1 新增 CLI 测试覆盖 `changed --format json`：读取 `.dataspec/config.json`、git tracked/untracked 变更、按 `defaultPaths` 过滤、输出 SQL 子集和最小 Context 建议。
- [x] 2.2 新增 CLI 测试覆盖 `lint-changed --format json`：只 lint changed SQL，复用现有 lint API 汇总，且无 SQL 时不调用服务端。
- [x] 2.3 新增 CLI 测试覆盖无 git 仓库、无 `defaultPaths` 和无变更的可恢复诊断。
- [x] 2.4 运行失败测试，确认失败原因来自功能缺失。

## 3. 实现

- [x] 3.1 实现 git 变更发现：tracked diff + untracked 文件，按 `defaultPaths` 过滤，避免扫描未配置的大型目录。
- [x] 3.2 实现 `changed` CLI 输出：支持 `--format json|text`，包含 files、summary、contextRecommendation、nextActions、diagnostics。
- [x] 3.3 实现 `lint-changed` CLI 输出：复用 changed 发现结果和 `lintSqlFiles`，只处理 changed SQL 文件。
- [x] 3.4 更新 README/TODO 或相关文档，记录第一版能力和边界。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate <change-id> --strict`。
- [x] 4.2 运行与改动范围匹配的验证命令，并记录证据。
- [x] 4.3 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [x] 4.4 完成提交并归档 OpenSpec change。
