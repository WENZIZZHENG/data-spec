## 1. OpenSpec 与测试基线

- [x] 1.1 创建 P6-4 proposal/design/spec/tasks，并通过 OpenSpec validate。
- [x] 1.2 先在 `tools/dataspec-cli.test.mjs` 增加 `init` 测试，覆盖首次生成 config/README、重复执行默认跳过、`--force` 覆盖、`--with-agents` marker 和 JSON 输出。

## 2. CLI 初始化实现

- [x] 2.1 在 `tools/dataspec-cli.mjs` 增加 `init` 命令分发、参数解析和 help 文案。
- [x] 2.2 实现初始化写入：创建 `.dataspec/config.json`、`.dataspec/README.md`，支持多次 `--default-path`、默认路径、server 规范化和不写入明文 token。
- [x] 2.3 实现幂等保护：已有文件默认跳过，`--force` 覆盖 DataSpec 管理文件。
- [x] 2.4 实现可选 `--with-agents`：向 `AGENTS.md` 写入带 marker 的 DataSpec 指令片段，重复执行不追加，`--force` 仅替换 marker 范围。
- [x] 2.5 初始化完成后复用轻量 doctor 检查，支持 text/json 输出写入结果、跳过结果、configPath 和 doctor checks。

## 3. 文档与待办

- [x] 3.1 更新 README CLI 说明，增加 `dataspec init` 示例、生成文件和 token 边界说明。
- [x] 3.2 更新 TODO，将 P6-4 标记为已完成第一版，并把下一步顺序推进到 P6-5。

## 4. 验证与评审

- [x] 4.1 运行 `node --test tools/dataspec-cli.test.mjs tools/dataspec-config.test.mjs`。
- [x] 4.2 运行 `npx openspec validate add-dataspec-init` 和 `git diff --check`。
- [x] 4.3 进行直接代码评审并修复 findings，不使用子 agent。
