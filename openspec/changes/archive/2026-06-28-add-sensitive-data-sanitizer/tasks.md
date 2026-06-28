## 1. OpenSpec 与现状梳理

- [x] 1.1 校验 OpenSpec change，确认 proposal/design/spec/tasks 格式有效。
- [x] 1.2 梳理后端/CLI 已有脱敏实现和高风险出口，确认第一版替换范围。

## 2. 后端统一脱敏工具

- [x] 2.1 新增 `SensitiveDataSanitizer`，覆盖敏感 key 判断、文本脱敏、递归 payload 清洗和长度截断。
- [x] 2.2 新增后端单测，覆盖 password/token/Authorization/Bearer/JDBC/connectionString/嵌套 payload。
- [x] 2.3 改造 `AiEvidencePackageServiceImpl` 复用统一 sanitizer。
- [x] 2.4 改造数据库直连错误/诊断脱敏逻辑，统一脱敏占位和测试。
- [x] 2.5 改造项目备份敏感 payload 扫描与快照兜底，复用统一敏感 key/pattern。

## 3. CLI 与文档

- [x] 3.1 收敛 CLI 内部脱敏 helper，补充本地交付包和错误输出脱敏测试。
- [x] 3.2 更新 README/TODO，记录 P6-45 第一版能力、允许持久化字段和禁止输出字段。

## 4. 验证与收口

- [x] 4.1 运行 `openspec validate add-sensitive-data-sanitizer --strict`。
- [x] 4.2 运行 `mvn test`、`node --test tools\dataspec-cli.test.mjs` 和必要前端测试。
- [x] 4.3 运行 `git diff --check`。
- [x] 4.4 执行本地结构化代码评审并修复 findings，不使用子 agent。
- [x] 4.5 完成提交并归档 OpenSpec change。
