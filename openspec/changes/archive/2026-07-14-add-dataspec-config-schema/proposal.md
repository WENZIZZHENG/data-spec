## Why

`.dataspec/config.json` 已成为 CLI、MCP 和业务仓库集成的默认入口，但当前只有运行时手写校验，没有可供编辑器和 AI 读取的 JSON Schema、显式配置版本或稳定关联方式。用户容易在执行命令后才发现字段类型错误，也无法从 doctor 判断当前配置采用哪版契约。

## What Changes

- 提供 versioned `.dataspec/config.json` JSON Schema，覆盖现有字段、安全说明、类型、范围和兼容边界。
- `dataspec init` 为新业务仓库生成本地 `config.schema.json`，并在 `config.json` 中写入 `$schema` 和 `configVersion`，让通用 JSON 编辑器无需插件即可提示和校验。
- 配置 loader additive 读取 `$schema` 与 `configVersion`，保留旧配置和未知扩展字段的运行时兼容。
- `dataspec doctor` 输出 schema 路径、支持版本、声明版本和关联状态；旧配置缺少关联时给 warning，声明不受支持版本时 fail。
- 更新测试和 README，说明 editor association、legacy 兼容和凭据字段边界。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `dataspec-local-config`: 增加 JSON Schema、`$schema`、`configVersion` 和 legacy 兼容语义。
- `dataspec-init`: 新初始化仓库生成 schema 文件及编辑器关联字段。
- `dataspec-doctor`: 增加配置 schema/version 摘要与关联/版本诊断。

## Impact

- 代码：`tools/dataspec-config.mjs`、`tools/dataspec-cli.mjs` 及对应 Node 测试。
- 新资产：仓库内 canonical config JSON Schema；`dataspec init` 生成的 `.dataspec/config.schema.json`。
- 文档与规格：README、上述三个主规格和 OpenSpec delta。
- 兼容性：不删除或重命名既有字段，不改变显式 CLI 参数优先级，不新增 npm 依赖，不修改后端 API、数据库或 MCP tool schema。
