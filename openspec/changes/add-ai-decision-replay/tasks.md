## 1. OpenSpec 与测试基线

- [x] 1.1 创建 P6-3 proposal/design/spec/tasks，并通过 OpenSpec validate。
- [x] 1.2 后端先新增 AI 作业记录服务测试，验证创建记录、缺少 projectId/jobType 报错、详情解析和回放 payload。

## 2. 后端 AI 作业记录模型/API

- [x] 2.1 新增 Flyway V10 迁移：`ds_ai_job_record` 表、项目/类型/时间索引和注释。
- [x] 2.2 新增 `aireplay` 模块 entity/mapper/repository/model/service/controller。
- [x] 2.3 实现 `AiJobRecordService`：create/list/detail，详情返回 parsed input/output、replayPayload 和 replayCommand。
- [x] 2.4 API 支持按 projectId 分页、jobType 过滤和详情查询。

## 3. 现有链路接入回放记录

- [x] 3.1 `AiContextExportService` 记录 create-table prompt 和 fix-sql prompt，包含 promptVersion、输入、输出和标准快照。
- [x] 3.2 `SqlLintService` 在保存 SQL 检查记录后写入 lint/fixedSql 作业，并关联 sqlCheckRecordId。
- [x] 3.3 `DdlGeneratorService` 在 DDL preview 成功后写入 ddl-preview 作业，包含 templateId/tableName/ddl/lint summary。
- [x] 3.4 集成点记录失败只打 warn，不阻断原有 prompt/lint/DDL 主流程。

## 4. 前端 AI 回放页面

- [x] 4.1 新增前端 API/types 和 AI 回放展示工具函数测试。
- [x] 4.2 新增“AI 协作 / AI 回放”页面，支持项目记录列表、jobType 筛选、分页和详情弹窗。
- [x] 4.3 详情展示标准快照、输入、输出、lint summary，并提供复制 replayPayload/replayCommand。

## 5. 文档与收尾

- [x] 5.1 更新 README/TODO，记录 P6-3 状态、使用方式和边界。
- [x] 5.2 运行后端测试、前端测试/构建、OpenSpec validate 和 diff 检查。
- [x] 5.3 进行直接代码评审并修复 findings。
