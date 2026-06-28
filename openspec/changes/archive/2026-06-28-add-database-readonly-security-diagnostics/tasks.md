## 1. OpenSpec 与契约

- [x] 1.1 验证 proposal/design/spec/tasks 完整且 `openspec validate add-database-readonly-security-diagnostics` 通过
- [x] 1.2 确认实现仅增强数据库连接测试结果，不新增凭据存储、写权限探测或阻塞流程

## 2. 后端诊断实现

- [x] 2.1 扩展数据库连接测试响应模型，新增安全诊断结构并保持 `success/message` 兼容
- [x] 2.2 在数据库连接测试成功后基于只读 metadata 和方言查询生成 PostgreSQL/MySQL/未知方言诊断
- [x] 2.3 对连接失败消息做敏感信息兜底脱敏，避免 password、token 或完整 JDBC URL 泄漏

## 3. 后端测试

- [x] 3.1 补充只读、安全未知、高风险和失败脱敏的后端单元测试
- [x] 3.2 确认诊断逻辑不执行写操作，且 metadata 查询失败不会让连接测试失败

## 4. 前端展示与类型

- [x] 4.1 同步或手工补齐 OpenAPI/前端类型中的连接安全诊断字段
- [x] 4.2 在反向导入页展示诊断卡片，包含风险、用户、范围、warnings、建议动作和推荐 SQL
- [x] 4.3 在覆盖率报告页展示同样的诊断卡片，且不缓存或展示敏感凭据
- [x] 4.4 补充前端 smoke/utility 测试，覆盖诊断字段和关键页面耦合

## 5. 文档、验证与提交

- [x] 5.1 更新 TODO.md 中 P6-25 状态和验收说明
- [x] 5.2 运行 `mvn test`、`pnpm test`、`pnpm build`、OpenSpec validate 和 `git diff --check`
- [x] 5.3 执行本地结构化代码评审并修复 findings，不使用子 agent
- [x] 5.4 创建本地 commit，提交 P6-25 实现
