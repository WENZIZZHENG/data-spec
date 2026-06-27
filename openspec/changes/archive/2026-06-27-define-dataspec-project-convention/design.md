## Design

### Approach

在 `AiContextExportService.generateAiContextPackage` 中继续集中生成 `.dataspec/` 包内容，新增 manifest 和 README 两个 entry。manifest 使用 JSON，供 AI 或脚本稳定解析；README 使用 Markdown，供人类和 coding agent 快速理解目录约定。

### Key Decisions

- **schemaVersion 固定为 `1`**：第一版只表达目录约定版本，不绑定 DataSpec 应用版本，避免没有统一 release version 时产生伪精确。
- **generatedAt 使用 UTC ISO-8601**：便于判断业务项目内 `.dataspec/` 是否过期。
- **commands 只给约定入口**：manifest 写 `dataspec lint <path|-> --project <projectId> --format json`，保持对未来正式 CLI 包名兼容；README 补充当前仓库 Node CLI 示例。
- **不写外部仓库**：仍然只导出 zip，用户决定复制到哪个业务项目。

### Verification

- 扩展 `AiContextExportServiceTest`，校验新增 entry、manifest JSON 结构、README 和 AGENTS 片段内容。
- 运行后端 `mvn test`。
- 运行 `openspec validate define-dataspec-project-convention --strict`。
