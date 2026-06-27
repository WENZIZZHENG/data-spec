## Design

### Approach

继续在 `MarkdownGeneratorService` 内集中生成项目级 Markdown。方法开头一次性读取数据域、字段、枚举和模板，后续章节复用这些集合和映射，避免重复调用和输出顺序漂移。

### Key Decisions

- **展示真实模型**：索引/外键等当前没有结构化模型，本轮不输出空章节，避免误导。
- **字段域关系**：通过 `Field.domainId` 匹配 `Domain.id`，输出 `名称(code)`，无法匹配时显示 `-`。
- **模板约束**：模板字段展示 `isRequired`、`nullable`、`defaultValue`、`sortOrder`，作为当前可用的“约束”信息。
- **Markdown 表格转义**：统一处理 `|`、换行和空值，避免字段注释破坏表格。

### Verification

- 新增 `MarkdownGeneratorServiceTest` 覆盖概览、字段元数据、数据域关系、枚举值类型和表模板章节。
- 运行后端 `mvn test`。
- 运行 `openspec validate enhance-data-dictionary-markdown --strict`。
