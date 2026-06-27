## 1. OpenSpec 与测试基线

- [x] 1.1 创建 P5-9 proposal/design/spec/tasks，并通过 OpenSpec validate。
- [x] 1.2 后端新增 token 管理服务测试，先验证失败，再实现。

## 2. 后端 token 管理

- [x] 2.1 新增 V8 迁移，为 `ds_api_token` 增加 `last_used_at` 和 `disabled_at`。
- [x] 2.2 扩展 ApiToken entity/repository/service，支持列表、创建、禁用和 lastUsedAt 更新。
- [x] 2.3 新增 token 管理 Controller，返回 metadata 和创建时一次性 plainToken。
- [x] 2.4 确认管理接口要求全项目权限，不向响应泄漏 tokenHash。

## 3. 前端 token 管理页面

- [x] 3.1 新增前端 token API 封装和类型。
- [x] 3.2 新增 API Token 管理页与路由入口，支持列表、创建、复制一次性明文 token 和禁用。
- [x] 3.3 保持现有顶部 API Token 登录入口可用，不改变 CLI/MCP token 使用方式。

## 4. 文档、评审与验证

- [x] 4.1 更新 README/TODO，记录 P5-9 状态、使用方式和 bootstrap 边界。
- [x] 4.2 运行后端测试、前端测试/构建、OpenSpec validate 和 diff 检查。
- [x] 4.3 进行直接代码评审并修复 findings。
