## 1. 草稿确认

- [x] 1.1 人工确认 OpenSpec 草稿中的 change id、capability、验收标准和边界。
- [x] 1.2 补充或删除 Open Questions，确保需求可实施。

## 2. 测试先行

- [x] 2.1 根据验收标准新增失败测试：非 enabled 字段不会被默认推荐给 AI；显式检索 deprecated/disabled/draft 字段能说明替代字段或原因；质量评分和 AI Context 读取结构化生命周期字段；字段状态和替代信息变更进入变更日志和快照。
- [x] 2.2 运行失败测试，确认失败原因来自功能缺失。

## 3. 实现

- [x] 3.1 按最小改动实现：新增字段生命周期迁移和模型/API/前端字段；字段推荐默认跳过非 enabled；字段检索、AI Context 和质量评分读取 replacementFieldId/replacementReason。
- [x] 3.2 更新 README/TODO 或相关文档，记录第一版能力和边界。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate <change-id> --strict`。
- [x] 4.2 运行与改动范围匹配的验证命令，并记录证据。
- [x] 4.3 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [x] 4.4 完成提交并归档 OpenSpec change。
