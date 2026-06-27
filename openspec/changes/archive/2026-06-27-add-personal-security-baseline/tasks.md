## 1. OpenSpec 与数据模型

- [x] 1.1 创建 P4-9 OpenSpec proposal/design/spec/tasks，明确第一版边界。
- [x] 1.2 新增 `ds_api_token` 迁移，并为 `ds_standard_change_log` 增加 `operator` 字段。
- [x] 1.3 新增 API token entity/mapper/repository/service 和安全配置属性。

## 2. 后端安全基线

- [x] 2.1 新增请求安全上下文、token 认证拦截器和 `/api/auth/me` 认证接口。
- [x] 2.2 将安全拦截器接入 `WebConfig`，开启安全模式后校验 Bearer token 和 `projectId` 授权。
- [x] 2.3 修改 `StandardChangeLogService` 写入当前操作者，未启用安全时写入 `local`。

## 3. 前端接入

- [x] 3.1 新增 auth store/API，统一保存 token、当前操作者和退出逻辑。
- [x] 3.2 修改 request 拦截器注入 Bearer token，并处理 401/403 清理登录态。
- [x] 3.3 在 App 中增加轻量 token 登录/退出入口，不新增复杂账号体系。
- [x] 3.4 为 CLI/MCP 增加 `apiToken` / `DATASPEC_TOKEN` / `--dataspec-token` 透传能力。

## 4. 测试、文档与提交

- [x] 4.1 补后端测试，覆盖 token hash、项目授权拒绝/通过和 change log operator。
- [x] 4.2 更新 README/TODO，说明安全基线启用方式、边界和 P4-9 状态。
- [x] 4.3 运行 `mvn test`、`pnpm build`、OpenSpec validate 和 diff 检查。
- [x] 4.4 进行直接代码评审（不使用子 agent），修复发现的问题后提交本地 commit。
