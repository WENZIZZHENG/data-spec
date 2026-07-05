# 分层、API 与持久化规则

## 分层规则

接口和具体类的取舍以 `SKILL.md` 默认规则与 `architecture.md` 复杂度判断为准；本节只展开分层落地方式。

轻量例外的判断标准见 `architecture.md` 的复杂度判断标准；例外只能降低样板数量，不能放松 DTO/entity 隔离、安全、事务和契约兼容要求。

| 层级 | 必要形态 | 依赖规则 |
| --- | --- | --- |
| Controller | 稳定 API 契约、OpenAPI-first、需要统一文档或多实现时使用 `XxxController` 接口声明映射，`XxxControllerImpl` 作为 `@RestController`；普通轻量模块可直接用 `@RestController` class | Controller 实现依赖 service/use-case 边界 |
| Service | 长期模块使用 `XxxService` 接口加实现类；轻量单实现模块可按项目约定使用具体类 | Service/use-case 依赖 repository port/interface |
| Repository | 长期模块使用 `XxxRepository` port/interface 加 `XxxRepositoryImpl`；复杂架构下接口放 application/domain，实现在 infrastructure | Repository 实现依赖 MyBatis mapper 或基础设施 client |
| Mapper | 只作为 MyBatis mapper | Controller 不得注入；Service 默认不得注入，仅在 `architecture.md` 轻量标准或 hotfix 且说明局部原因时例外 |

实现类可以是 Spring Bean；上层依赖不得穿透到具体实现或 MyBatis API。若因 hotfix 或明确项目规则暂不拆接口，必须说明这是局部例外，不得扩大为新规范。

推荐依赖形态：

- Service 注入 `XxxRepository` port/interface，`XxxRepositoryImpl` 封装 MyBatis mapper 调用。
- Service 方法中不出现 `LambdaQueryWrapper`、`selectList`、`insert`、`updateById` 等 MyBatis API。
- Repository 接口暴露业务语义方法，如 `findEnabledByProject`、`saveDraft`、`existsByNameExcludingId`。
- 更新流程在 service/use-case 中先查旧数据，再按白名单合并字段，Repository 只负责读取和写入。

## Controller 与 API

- 使用 Controller 接口时，接口是 API 契约。除非项目明确规定其他位置，否则路由注解放在接口上，并通过测试或启动校验证明 Spring 能注册该映射。
- 已知陷阱：Spring 官方要求使用 controller interface 时映射注解保持在同一侧；项目配置或版本导致接口注解未生效时，应按项目约定在实现类声明映射，并用启动测试或 MockMvc 验证映射注册成功。
- 每个 endpoint 方法都必须有清晰映射。Create 等命令型方法避免因裸 `@PostMapping` 造成歧义；若类级路径已经表达集合资源，且 API spec 要求 collection-style `POST /resource`，方法级空 path 是合法选择。
- 已有 OpenSpec、README、前端调用或外部调用方依赖的 API 契约优先于默认路由偏好。维护既有接口时不得擅自把 `POST /resource` 改成 `POST /resource/create`。
- Controller 保持薄层：只负责 HTTP 绑定、请求/响应包装和委派。校验、权限、事务和业务决策放在 service/use-case 层。
- Request/response model 必须表达外部契约，不把数据库字段、内部状态或安全字段顺手暴露出去。
- 新增资源、删除、异步任务、校验失败、未授权、未找到和冲突等 API 行为必须明确状态码、错误体和兼容策略；新增资源按项目约定考虑 `201 Created` 和 `Location`。
- 新增 request model 时优先使用 Bean Validation，如 `@NotNull`、`@NotBlank`、`@Size`、`@Min`、`@Pattern`；复杂业务校验放 service。
- 全局异常处理优先通过已被项目规范确认或稳定一致的 `BizException` / `R` / `@ControllerAdvice` 风格统一；若历史实现与更高优先级规范冲突，以规范为准，不为单个接口临时返回不一致结构。

## 对象边界与转换

| 对象 | 作用 | 边界 |
| --- | --- | --- |
| DTO / Req / Resp | API 入参出参 | 只在 controller/API 契约层使用 |
| DO / Domain model | 业务语义对象 | 用于 service 与 repository 之间，适合复杂业务或需要隔离 PO 时引入；复杂域优先使用 `DomainModel`、Aggregate、Value Object 等更明确命名 |
| Entity / PO | 持久化映射对象 | 不直接作为新增外部 API 契约；PO 不穿透 repository 实现层 |

- 新核心模块或重做长期模块时，优先采用 DTO -> domain model/aggregate -> PO/entity 的清晰边界；使用 `DO` 前先确认团队语义，避免和 Data Object/PO 混淆。
- 小范围维护既有 MyBatis entity 模块时，不强制大规模改名或引入 DO/PO，但必须保证 entity 不泄漏到新增 API 契约。
- 转换逻辑集中在 converter/assembler/factory/helper，不散落在 controller 和 service 中；避免把 API 转换器命名为 `Mapper`，以免和 MyBatis mapper 混淆。
- API 转换优先命名为 `Converter` / `Assembler`，持久化转换可命名为 `PersistenceConverter`；MyBatis 访问层才使用 `Mapper`。
- 转换器或 helper 应无状态、无副作用、无需 Spring 注入；不要在转换层访问数据库、权限、配置或外部服务。
- 更新流程执行 `SKILL.md` 默认红线：Service 先加载现有数据，再按明确规则合并允许更新的字段。

## Repository、Mapper 与事务

- Repository 是持久化端口，不只是 MyBatis 包装类的命名。
- 如果新增长期模块的 `XxxRepository.java` 包含 `public class XxxRepository`，必须停止并改为接口加实现类，或记录要求具体 repository 类的明确项目规则。
- Mapper 例外只限轻量 CRUD 或 hotfix；暂时沿用时必须说明局部原因，并避免把 mapper API 泄漏给更多 service。
- Repository 接口方法使用业务语义命名，如 `findEnabledByProject`，不要把 `selectList`、`LambdaQueryWrapper` 泄漏到 service。
- 查询条件、排序、分页上限和逻辑删除兼容放在 repository 实现或更低层；service 只表达业务意图。
- 多步写操作、批量导入、状态流转、删除和一致性流程放在 `@Transactional` 的 public service/use-case 方法上；单条写入按项目和一致性边界判断。
- 只读复杂查询可使用 `@Transactional(readOnly = true)`；简单单表查询可按项目既有约定决定。
- 不在 private 方法或 self-invocation 上依赖 `@Transactional` 生效；事务传播、隔离级别和 rollback 规则偏离默认值时必须说明原因。
- 事务属于 application/use-case 边界，不属于 Controller、domain model 或 mapper；避免在同一事务中包远程调用或长耗时 IO，异步事件、副作用和最终一致性策略要显式说明。
- 远程调用、消息发送、文件生成、通知等副作用优先通过 `@TransactionalEventListener(phase = AFTER_COMMIT)`、Spring Event、outbox 或任务队列拆到事务提交后执行，保证事务主体只覆盖数据库一致性。
- 更新流程必须先确认目标存在和归属，再合并字段；唯一性检查要排除当前记录，避免把自身误判为冲突。
- 幂等、重复提交、并发写、状态非法迁移、逻辑删除唯一约束和乐观锁/version 必须按业务风险设计测试或约束。
