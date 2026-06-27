## 1. Doctor 行为测试

- [x] 1.1 在 `tools/dataspec-cli.test.mjs` 增加 `doctor --format json` 成功用例，覆盖配置读取、server/project/auth/openapi/defaultPaths 检查。
- [x] 1.2 增加失败用例，覆盖服务不可达或项目不可访问时返回 1 且输出结构化失败项。
- [x] 1.3 增加参数错误用例，覆盖非法 `--format` 或未知参数返回 2。

## 2. CLI 实现

- [x] 2.1 在 `tools/dataspec-cli.mjs` 增加 `doctor` 命令分发和 help 文案。
- [x] 2.2 扩展参数解析以支持布尔 flag `--check-openapi`，保持现有命令行为不变。
- [x] 2.3 实现 doctor 检查模型、文本输出、JSON 输出和退出码规则。
- [x] 2.4 复用现有 token/header/config 逻辑，调用 `/api-docs`、`/api/auth/me` 和 `/api/projects/{id}`。
- [x] 2.5 接入可选完整 OpenAPI 漂移检查，默认只做轻量 OpenAPI 状态检查。

## 3. 文档、待办与验证

- [x] 3.1 更新 README CLI 说明，增加 `doctor` 示例和返回码说明。
- [x] 3.2 更新 `TODO.md`，将 P5-2 标记为已完成第一版，并把下一步顺序推进到 P5-3。
- [x] 3.3 运行 OpenSpec validate、Node CLI 测试、必要的 diff/格式检查。
- [x] 3.4 进行直接代码评审（不使用子 agent），修复发现的问题后提交本地 commit。
