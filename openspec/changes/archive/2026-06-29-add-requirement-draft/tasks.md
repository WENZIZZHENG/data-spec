## 1. 草稿确认

- [x] 1.1 确认 change id 为 `add-requirement-draft`，capability 为 `requirement-draft`。
- [x] 1.2 收窄第一版范围：API + 前端入口，不做 CLI/MCP、不新增持久化表、不自动写入候选。

## 2. 测试先行

- [x] 2.1 新增后端服务测试，覆盖 matchedFields、missingCandidates、ambiguousTerms、recommendedTemplate、copyablePrompt 和只读边界。
- [x] 2.2 运行目标失败测试，确认失败原因来自功能缺失。

## 3. 实现

- [x] 3.1 新增需求草案后端模型、服务和 `/api/requirement-drafts` 接口。
- [x] 3.2 新增前端 API、类型和“需求草案”页面入口。
- [x] 3.3 更新 README/TODO，记录第一版能力和边界。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate add-requirement-draft --strict`。
- [x] 4.2 运行与改动范围匹配的验证命令，并记录证据：`mvn test`、`pnpm test`、`pnpm build`、`git diff --check`。
- [x] 4.3 执行本地结构化代码评审并修复 findings，不使用子 agent；已修复需求草案页 tabs 固定 `model-value` 导致无法切换的问题。
- [x] 4.4 完成归档前收口准备；OpenSpec change 归档后随本轮实现统一提交。
