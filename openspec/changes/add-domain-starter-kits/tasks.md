## 1. OpenSpec 与数据模型

- [x] 1.1 校验 OpenSpec change，确认 proposal/design/specs/tasks 格式有效。
- [x] 1.2 新增 Flyway 迁移，创建 starter kit 安装记录表。

## 2. 后端 Starter Kit 能力

- [x] 2.1 新增 starter kit 内置注册表、DTO、安装记录实体/mapper/repository。
- [x] 2.2 实现 starter kit 列表、项目应用和安装记录查询服务，保证重复应用跳过已有字段/枚举/模板。
- [x] 2.3 新增 REST API：列出 kits、应用 kit、查询项目安装记录，并复用项目访问校验。
- [x] 2.4 AI Context 字段目录导出 starter kit 来源元数据。

## 3. 前端入口

- [x] 3.1 新增 starter kit API 薄封装和类型导出。
- [x] 3.2 项目创建对话框支持选择 starter kit，创建后自动应用并展示摘要。
- [x] 3.3 项目列表支持对当前/指定项目手动应用 starter kit，并显示安装记录。

## 4. 测试、文档与收口

- [x] 4.1 补后端 starter kit 服务/API 和 AI Context 来源导出测试。
- [x] 4.2 补前端 smoke/单测，覆盖项目页 starter kit 入口和 API 耦合。
- [x] 4.3 更新 README、TODO 和 OpenSpec Verification Evidence。
- [x] 4.4 运行 OpenSpec validate、后端测试、前端测试/build 和 diff 检查。
- [x] 4.5 执行本地结构化代码评审并修复 findings。
- [ ] 4.6 完成提交并归档 OpenSpec change。

## Verification Evidence

- `mvn test`（`dataspec-server`）：331 tests, 0 failures, 0 errors。
- `pnpm test`（`dataspec-web`）：88 tests, 0 failures。
- `pnpm build`（`dataspec-web`）：`vue-tsc --noEmit && vite build` 通过；仅保留 Vite/Rolldown 依赖注解和 chunk size 既有警告。
- `openspec validate add-domain-starter-kits --strict`：通过。
- `git diff --check`：通过；仅输出工作区换行提示。
- 本地结构化代码评审：发现 Starter Kit catalog 加载失败可能影响项目列表初始化的降级体验，已改为失败时仅清空 catalog 并在用户主动打开对话框时提示；复查后未发现阻断性功能、安全、性能或测试问题。
