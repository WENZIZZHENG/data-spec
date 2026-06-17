## Context

`AiContextExportService` 已生成 `DATABASE_RULES.md`、`field-catalog.json` 和 `rules.yaml` 三个单文件，`AiContextController` 已提供预览和下载接口。P0-1 需要把这些分散文件升级为 AI 可直接消费的上下文包，并补充 agent 指令、JSON Schema、Prompt 模板和 SQL 示例。

## Goals / Non-Goals

**Goals:**
- 复用现有导出服务，新增一个稳定 zip 包生成方法。
- zip 包内容使用固定相对路径，方便业务项目直接解压或复制。
- 生成内容全部为 UTF-8，字段目录 JSON Schema 与当前 `field-catalog.json` 保持一致。
- 新增测试覆盖 zip 文件名、目录结构和关键文件内容。

**Non-Goals:**
- 不实现 CLI、MCP 或 CI 调用入口。
- 不自动写入外部业务仓库。
- 不引入新的压缩库，优先使用 JDK `ZipOutputStream`。
- 不重做字段模型；P0-4 的别名、敏感标记、状态等后续扩展仍按待办独立处理。

## Decisions

- **包内路径固定为 `.dataspec/*` + 根目录 `AGENTS.md.fragment`。** 这样业务项目可以直接保留 `.dataspec/` 作为 AI 上下文入口，同时把 agent 指令片段按需合并到项目级 `AGENTS.md`。
- **服务层返回 `byte[]`。** 当前 Controller 已用 `ResponseEntity<byte[]>` 下载单文件；zip 继续沿用该模式，避免引入 streaming 复杂度。第一版包内容较小，内存打包足够。
- **Prompt 和 Schema 由服务内模板生成。** 第一版模板与字段目录结构强绑定，放在 service 内更容易跟现有生成方法同步；后续若模板变多再迁移到资源文件。
- **测试优先验证 zip 合同而不是压缩实现细节。** 单测解压 zip，断言必要 entry 存在、内容非空、Schema 可解析、示例 SQL 来自当前 `examples/`。

## Risks / Trade-offs

- **[Risk] 包内容和后续字段模型扩展可能漂移。** → `field-catalog.schema.json` 只声明当前字段目录合同，P0-4 扩展字段模型时同步更新 Schema 和测试。
- **[Risk] Windows/中文内容编码问题。** → 所有 zip entry 按 UTF-8 字节写入；测试读取关键中文/英文片段。
- **[Risk] 下载接口不走 `R<T>` 包装。** → 与现有单文件下载接口保持一致，二进制响应更适合 `ResponseEntity<byte[]>`。

## Migration Plan

新增接口与服务方法不影响现有 API。回滚时删除新增 zip 方法、Controller endpoint、测试和 README 段落即可；现有单文件导出保持可用。
