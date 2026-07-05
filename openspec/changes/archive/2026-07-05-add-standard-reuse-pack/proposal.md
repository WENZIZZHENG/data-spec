## Why

多个业务项目会复用用户、订单、支付等通用字段标准。当前 DataSpec 已有领域 Starter Kit 和项目备份恢复，但 Starter Kit 只能使用内置种子，备份恢复又偏完整项目迁移。用户如果在一个项目沉淀了自己的通用标准，只能复制到其他项目，后续升级会产生漂移，AI Context 也无法说明字段来自共享基线还是项目局部覆盖。P6-68 需要提供个人 / 小团队可用的轻量标准复用包能力，让项目之间能复用字段、枚举、规则和模板，同时保留项目本地差异。

## What Changes

- 新增标准复用包模型和 API：从源项目导出版本化共享包，包内包含 `basePackVersion`、字段、枚举、规则、模板、内容 hash 和资产计数。
- 新增应用预览与确认应用：目标项目可 dry-run 共享包，看到新增、跳过、覆盖/漂移项；确认应用时只创建缺失资产，不覆盖本地资产，并记录应用摘要。
- 新增漂移报告：共享包升级或目标项目本地修改后，可对比目标项目与包内容，输出 missing、matched、overridden、drifted 等项目级差异。
- 扩展 AI Context：字段目录和 manifest 以 additive 方式说明共享包应用摘要，字段若带 `pack:<key>@<version>` 来源标记则导出 `standardPackSources`，帮助 AI 区分共享包来源与项目覆盖。
- 扩展前端入口：提供项目级“标准复用包”页面，用于创建包、查看包、预览应用、确认应用和查看漂移概览。

## Capabilities

### New Capabilities
- `standard-reuse-pack`: 项目间标准复用包导出、应用、漂移报告和 AI 可读来源说明。

### Modified Capabilities
- `ai-context-package`: AI Context 字段目录和 manifest 以兼容方式增加标准复用包来源和漂移摘要。
- `project-standards`: 项目标准资产可以通过共享包初始化，并保留本地覆盖。

## Impact

- SDD 等级：SDD full。该变更涉及数据库迁移、API 公共契约、标准资产写入、项目间复用语义和 AI 外部可见契约。
- API/AI 契约：新增 `/api/standard-reuse-packs` 系列 endpoint、请求响应对象和 AI Context 字段；所有公共模型字段需要 Javadoc/TSDoc 或 schema 说明。
- 后端：新增复用包实体、应用记录、Repository、Service、Controller；复用项目备份和 Starter Kit 的自然键、复制、hash 与幂等思想。
- 前端：新增 API wrapper、类型、页面和导航入口，保持现有项目选择、Element Plus 和 request 模式一致。
- 验证：需要 OpenSpec strict、后端 service/controller/AI Context 测试、前端 utility/smoke 测试、相关构建验证、独立子 agent 评审。
