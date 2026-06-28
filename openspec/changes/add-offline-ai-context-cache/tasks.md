## 1. OpenSpec 与范围确认

- [x] 1.1 验证 proposal/design/spec/tasks 完整且 `openspec validate add-offline-ai-context-cache` 通过。
- [x] 1.2 确认第一版边界：不改后端 API、不缓存 secrets、不让离线缓存执行服务端写入、不自动修改 `.gitignore`。

## 2. CLI 缓存导出

- [x] 2.1 扩展 `export-context` 参数，支持 `--cache` 和 `--cache-ttl-days`，允许与 `--output` 同时使用。
- [x] 2.2 实现 AI Context zip 安全解包到 `.dataspec/context/`，拒绝绝对路径、`..` 和越界写入。
- [x] 2.3 写入 `.dataspec/context/cache-metadata.json`，包含项目、服务端、导出参数、导出时间、过期时间、contentHash 和标准 metadata。
- [x] 2.4 保持现有 `export-context --output` 行为兼容；未传 `--cache` 时不写缓存目录。

## 3. Doctor 缓存诊断

- [x] 3.1 扩展 `doctor` 检查项，读取本地 cache metadata 并输出 missing/fresh/stale/unreadable 状态。
- [x] 3.2 服务可用时读取远端 AI Context manifest 或等价 metadata，对比 cached specHash/specVersion/source。
- [x] 3.3 服务不可用时保留离线提示：有缓存则说明可读但可能 stale，无缓存则给出刷新命令。

## 4. 测试与文档

- [x] 4.1 补 CLI 单测：cache 写入、metadata 脱敏、`--output --cache` 并存、危险 zip path 拒绝、未传 cache 不写目录。
- [x] 4.2 补 doctor 单测：无缓存、fresh、stale、服务不可用 stale、远端差异。
- [x] 4.3 更新 README/TODO 中 P6-29 状态、命令示例、缓存目录和边界说明。

## 5. 验证、评审与提交

- [x] 5.1 运行 `node --test tools/dataspec-cli.test.mjs tools/dataspec-config.test.mjs`、OpenSpec validate 和 `git diff --check`。
- [x] 5.2 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [x] 5.3 创建本地 commit，提交 P6-29 实现。
