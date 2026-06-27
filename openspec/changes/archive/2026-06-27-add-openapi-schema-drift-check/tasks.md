## 1. 脚本核心与测试

- [x] 1.1 新增 `check-openapi-schema` 脚本测试，覆盖参数解析、source 优先级、schema 文本比较和漂移消息。
- [x] 1.2 新增 `dataspec-web/scripts/check-openapi-schema.mjs`，实现临时生成、比较和错误输出。

## 2. 验证入口与文档

- [x] 2.1 在 `dataspec-web/package.json` 增加 `check:api` 脚本。
- [x] 2.2 更新 README 验证说明，补充 `pnpm check:api` 和 `--source` 用法。
- [x] 2.3 更新 TODO.md 中 P4-5 状态。

## 3. 验证与收尾

- [x] 3.1 运行新增 Node 测试、`pnpm test`、`pnpm build`、`openspec validate add-openapi-schema-drift-check` 和 `git diff --check`。
- [x] 3.2 进行直接代码评审（不使用子 agent），修复发现的问题后提交本地 commit。
