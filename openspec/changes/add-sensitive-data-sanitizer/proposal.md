## Why

DataSpec 已经在数据库直连、项目备份、AI evidence package、CLI 交付包等局部出口做了脱敏，但规则分散，容易出现某个新出口漏掉 password、token、Authorization、完整 JDBC URL 或连接串。P6-45 要把常见敏感信息处理收敛成统一边界，优先保护 AI/CLI/日志/导出这些最容易被复制转发的结果。

## What Changes

- 新增后端公共 `SensitiveDataSanitizer`，统一处理敏感 key、JDBC/URL、Bearer/Authorization、password/token/apiKey 等文本模式，并支持递归清洗 Map/List/POJO 摘要。
- 让现有高风险出口复用统一脱敏：AI evidence package、数据库直连错误/诊断、项目备份安全扫描和快照摘要。
- 补 CLI 侧脱敏 fixture，锁定 `sanitizeSecretText`/交付包/错误输出不会泄漏 token、password、Bearer 或完整 JDBC URL。
- 更新 README/TODO/OpenSpec，明确第一版允许持久化与禁止输出的字段边界。

## Capabilities

### New Capabilities

- `sensitive-data-sanitization`: DataSpec 后端与 CLI 的敏感信息脱敏边界、统一脱敏工具、测试夹具和文档约束。

### Modified Capabilities

- `ai-evidence-package`: evidence 输入/输出/diagnostics/suggestedCommands 继续使用脱敏摘要。
- `db-reverse-import-frontend` / `db-readonly-security-diagnostics`: 数据库直连错误和安全诊断复用统一脱敏。
- `project-backup-restore`: 备份包扫描和变更快照摘要复用统一敏感字段判断。

## Impact

- 后端：新增 `com.dataspec.common.sanitize` 包和单测；替换 evidence、reverse import、project backup 的局部正则。
- CLI：补充 Node 测试覆盖本地交付包与错误输出脱敏，不改命令参数协议。
- 文档/规范：README 与 TODO 记录脱敏边界和本轮验证证据。
- 不新增数据库表，不改变业务 API shape；第一版只改变敏感值的展示/导出文本。

## Verification Evidence

- `openspec validate add-sensitive-data-sanitizer --strict`：通过。
- `mvn test`（`dataspec-server`）：通过，344 tests / 0 failures / 0 errors。
- `node --test tools\dataspec-cli.test.mjs`：通过，67 tests / 0 failures。
- `git diff --check`：通过，仅提示 Windows 工作区行尾转换。
- 本地结构化代码评审：已执行，不使用子 agent；发现并修复 `Authorization: Bearer ...` 二次脱敏和 CLI 复合敏感 key 识别偏窄问题。
