## 1. 草稿确认

- [x] 1.1 人工确认 OpenSpec 草稿中的 change id、capability、验收标准和边界。
- [x] 1.2 补充或删除 Open Questions，确保需求可实施。

## 2. 测试先行

- [x] 2.1 根据验收标准新增失败测试：字段推荐、字段检索和自然语言需求草案输出可读 evidence；缺失候选包含来源、置信度和文档引用；契约字段防漂移。
- [x] 2.2 运行失败测试，确认失败原因来自功能缺失。

## 3. 实现

- [x] 3.1 按最小改动实现：定义轻量 explain trace 契约；字段推荐、字段检索和需求草案附带 evidence 数组，包含 sourceType、sourceId、snapshotVersion、matchReason、confidence、ruleCode 和 docsRef；前端详情页展示证据来源。
- [x] 3.2 更新 README/TODO 或相关文档，记录第一版能力和边界。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate <change-id> --strict`。
- [x] 4.2 运行与改动范围匹配的验证命令，并记录证据。
- [x] 4.3 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [x] 4.4 完成提交并归档 OpenSpec change。
