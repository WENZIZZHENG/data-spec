## 1. OpenSpec 与现状定位

- [x] 1.1 创建 P4-10 OpenSpec proposal/design/spec/tasks，明确第一版边界。
- [x] 1.2 定位项目创建、内置 standards、模板、规则和前端工作台/项目页现有实现。

## 2. 后端实现

- [x] 2.1 新增演示项目结果模型和服务，支持创建或复用 demo 项目。
- [x] 2.2 为 demo 项目幂等补种内置标准、核心规则配置、订单表模板和示例 SQL 元数据。
- [x] 2.3 在项目 API 暴露 `POST /api/projects/demo`，并补充 service 单测。

## 3. 前端实现

- [x] 3.1 更新前端 API schema/types/project wrapper，接入 demo 创建 API。
- [x] 3.2 在 Dashboard 和 ProjectList 增加创建演示项目入口，并在成功后同步当前项目。
- [x] 3.3 为 Dashboard 增加快速开始入口，SQL 校验和 DDL 生成页支持演示 query 参数预填，并补齐 AI Context 导出页最小可用入口。

## 4. 文档与验证

- [x] 4.1 更新 README 与 TODO 中 P4-10 状态和演示路径。
- [x] 4.2 运行后端测试、前端测试/构建、OpenSpec validate 和 diff 检查。
- [x] 4.3 完成直接代码评审，修复或记录发现后创建本地 commit。
