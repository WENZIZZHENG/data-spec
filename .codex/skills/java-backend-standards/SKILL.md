---
name: java-backend-standards
description: 用于实现、修改或评审 Java/Spring Boot 后端代码，尤其是 Controller/API 路由、Service/Repository 分层、MyBatis mapper 访问、DTO/entity 边界、事务、校验、安全、持久化，或现有 Java 代码与明确项目规范冲突时。纯 Java 算法、仅构建工具、仅前端或纯文档任务不要使用。
---

# Java 后端开发规范

## 核心原则

历史代码是证据，不是规范。编写或评审 Java/Spring 后端代码时，先按明确规范和架构目标判断，再参考现有实现。

规范优先级：

1. 用户最新指令。
2. 项目 `AGENTS.md`、`SDD.md`、OpenSpec、API 文档和设计文档。
3. 本 skill 及其 references。
4. 现有代码示例，仅当它们不与更高优先级规范冲突时才可参考。

## 使用流程

1. 判断是否触发 SDD / OpenSpec，尤其是 API、存储、安全、权限、外部协议、核心业务变更。
2. 按任务类型读取下方对应 reference；多场景命中时读取所有直接相关 reference，不要无差别加载全部资料。
3. 如果现有代码和规范冲突，把现有代码标记为历史实现或不一致实现，不要静默照抄。
4. 新代码按当前任务的复杂度选择结构；简单任务保持最小改动，复杂长期模块先定架构边界。

## Reference 路由

| 场景 | 必读 |
| --- | --- |
| 新项目、复杂业务、核心域、长期演进、目录结构选择 | `references/architecture.md` |
| Controller、Service、Repository、Mapper、API 路由、DTO/DO/PO、事务 | `references/layering.md` |
| Java 命名、`IUserService`、注释、配置、安全、日志、SQL 安全 | `references/quality-security.md` |
| MyBatis 查询性能、批量写入、N+1、分页、索引意识 | `references/performance.md` |
| 测试、验证、代码评审、红线信号 | `references/testing-review.md` |

## 默认规则与红线

以下规则除非更高优先级规范另有要求，否则作为 Java/Spring 后端新增或大幅修改时的默认约束。小范围 hotfix 可以保持最小改动，但不得新增分层、安全或契约风险。

- 新增或长期演进模块默认接口优先：`XxxController`、`XxxService`、`XxxRepository` 表达契约，具体类承载实现；普通单实现 CRUD 可按项目明确约定使用具体类，但必须保持边界清晰。
- 跨层依赖优先使用接口类型；不得让 service 注入 `XxxRepositoryImpl`。Service 直接注入 MyBatis mapper 只允许在项目明确的轻量例外中出现，不得作为长期模块规范。
- Controller 是 API 契约边界，保持薄层；业务校验、权限、事务和数据一致性放在 service/use-case 层。
- 新增 API 必须隔离 request/response model 和持久化 entity；复杂或长期模块使用 DTO -> domain model/aggregate -> PO/entity，并同步字段说明、OpenAPI/schema description 和错误码文档。
- Create 等命令型方法不得造成映射含糊；collection-style `POST /resource` 可保留，既有 OpenSpec、README、前端或外部调用方依赖的契约不得擅自改成 `/create`。
- 不采纳项与正确处理统一见 `references/quality-security.md`；不要在 `SKILL.md` 重复维护清单。
- 更新接口不得用 DTO 直接覆盖 entity/PO；service 先加载现有数据，再按白名单合并字段。

## 完成前检查

- 是否按 Reference 路由读取了本任务所需资料？
- 是否先依据明确规范和复杂度判断，而不是把历史代码当规范？
- 是否识别 API、事务、安全、持久化、性能或外部协议风险，并执行匹配验证？
- 完整评审清单见 `references/testing-review.md`，不要在 `SKILL.md` 重复展开。

## 冲突说明模板

当历史代码冲突时，在实现说明或最终回复中使用类似表述：

> 我发现现有代码里有直接使用具体 repository 类的历史实现。我没有把它当作规范，而是按接口优先分层完成本次新变更。

如果修正会牵涉大量旧模块迁移，当前任务保持最小范围，并单独提出重构或 OpenSpec 变更。
