## Why

DataSpec 已经可以从字段库、数据库反向导入、标准候选 Inbox 和质量评分中积累证据，但 AI 在消费标准字段时还缺少一个统一信号来判断“这是可直接采用的强标准，还是需要人工复核的候选/推断”。P6-94 需要先把这些已有证据聚合成只读、可解释的来源可信度与 AI 置信度摘要，避免 AI 把低证据字段当作强制标准使用。

## What Changes

- 新增只读字段来源可信度摘要能力，按项目返回每个标准字段的来源证据、候选证据、质量评分、AI 置信度等级、建议用途和复核提醒。
- 新增后端只读 API：`GET /api/fields/provenance-confidence?projectId=<id>`，不写入字段、候选、来源批次或质量评分数据。
- 复用已有 `ds_field`、`ds_field_source`、`ds_standard_candidate` 和字段质量评分结果推导置信度，不新增数据库 schema、迁移或存储语义。
- 对返回的来源引用做脱敏处理，只暴露表/列、候选来源等可解释摘要，不返回 raw metadata、JDBC URL、token、password、DSN 或 Authorization。
- 不做自动采纳、自动修复、候选决策、批量重算入库、前端页面和 AI Context 包格式变更。

## Capabilities

### New Capabilities

- `field-provenance-confidence`: 聚合标准字段来源证据、候选证据和质量评分，输出 AI 可消费的置信度摘要。

### Modified Capabilities

- 无。

## Impact

- 后端：新增 `fieldprovenance` controller/service/model，只读依赖字段库、来源记录、标准候选和字段质量评分；为候选 Repository 增加按项目只读查询。
- API：新增只读响应契约，响应字段需要业务语义说明和单元测试覆盖。
- 安全：沿用项目访问边界；输出前脱敏来源引用，不返回敏感连接串或 raw evidence。
- 验证：新增 service/controller 单测，运行受影响后端测试、OpenSpec strict、`git diff --check` 和 commit 前敏感词扫描。
