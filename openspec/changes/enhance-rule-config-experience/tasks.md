## 1. 参数模型与测试

- [x] 1.1 新增规则参数工具函数，覆盖常见规则的 JSON 解析、表单模型生成、保存 JSON 生成和摘要输出。
- [x] 1.2 先补 Node 单测覆盖 `requiredColumns`、`forbiddenNames`、`recommendations`、`suffixTypes/prefixTypes`、未知规则兜底。
- [x] 1.3 将新增测试接入 `dataspec-web` 的 `pnpm test`。

## 2. 规则配置页改造

- [x] 2.1 在 `RuleConfig.vue` 中根据 `ruleCode` 展示结构化参数编辑器，并同步 JSON 预览。
- [x] 2.2 支持列表类、键值映射类、后缀/前缀类型映射类参数的新增、删除和编辑。
- [x] 2.3 规则列表展示参数摘要，保留原始 JSON 兜底查看。

## 3. 验证与收尾

- [x] 3.1 运行 `pnpm test`、`pnpm build` 和 `openspec validate enhance-rule-config-experience`。
- [x] 3.2 更新 README/TODO 中规则配置体验状态。
- [x] 3.3 进行直接代码评审（不使用子 agent），修复发现的问题后提交本地 commit。
