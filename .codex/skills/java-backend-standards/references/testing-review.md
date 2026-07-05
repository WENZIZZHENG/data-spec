# 测试、验证与评审

## 验证选择

按风险选择最小有效测试，不为小改动盲目全量，但不能只靠人工看代码。

| 变更类型 | 优先验证 |
| --- | --- |
| Service 业务规则、校验、权限、敏感信息拦截 | JUnit + Mockito 单测，覆盖成功、失败和边界 |
| Controller 路由、参数、响应结构 | `@WebMvcTest` / MockMvc 或能验证映射的集成测试 |
| Repository 查询、分页、排序、逻辑删除、SQL 片段 | mapper/repository 集成测试；MyBatis 可用 `@MybatisTest` 或真实方言数据库 |
| 查询性能、分页、批量写入、导入导出 | mapper/repository 集成测试；必要时用真实方言数据库、EXPLAIN 或基准数据验证，关注 N+1、全表扫描、深分页、索引命中和批量 round trip |
| Spring Security、认证、授权、公开路径、CSRF/CORS/Actuator | MockMvc / Spring Security 集成测试，覆盖未认证、角色不足、允许公开路径、CSRF/CORS 条件和 Actuator 暴露 |
| 事务、并发、幂等、批量写入 | `@SpringBootTest`、Failsafe/Testcontainers 或更接近真实数据库的集成测试；必要时补回归用例 |
| API、存储、安全、权限、外部协议 | 项目 SDD / OpenSpec 要求的验证和独立评审 |

测试命名说明业务行为，不只写 `testCreate`。Mock 只隔离外部依赖，不用于掩盖未理解的数据流。

推荐门禁按项目实际可用命令裁剪：

- 后端基础：`mvn test`；有质量 profile 时运行 `mvn verify -Pquality`。
- 通用空白：`git diff --check`；存在暂存内容时运行 `git diff --cached --check`。
- 安全/凭据改动：优先运行 `gitleaks detect --source .` 或项目等价 secrets scanner；没有工具时用 `rg` 扫描 `password|passwd|token|secret|authorization|api_key|apikey|jdbc:|dsn`。
- API 契约变更：运行 OpenSpec strict、前后端 schema drift 检查；有条件时增加 OpenAPI diff，拦截删除字段、改类型、改必填、改状态码等 breaking change。
- API 字段说明：新增或变更 request/response 字段、枚举和错误码时，检查 `description`、示例、可空性、默认值、单位、枚举语义和脱敏要求是否同步；字段存在但无业务语义说明视为契约未完成。
- 分层约束长期化后：优先补 ArchUnit，检查 Controller 不依赖 Repository/Mapper、Service 不依赖 mapper 或 `repository.impl`、RepositoryImpl 才依赖 MyBatis mapper、domain 不依赖 Spring/MyBatis。

## 评审清单

- 规范来源：是否先依据明确规范，再参考相邻代码；历史实现不作为规范来源。
- 架构取舍：是否按复杂度选择目录结构和接口边界，见 `architecture.md`。
- 分层依赖：Controller、Service、Repository、Mapper、DTO/entity、转换和更新边界是否符合 `layering.md`。
- API 契约：路由、状态码、错误体、字段说明、OpenSpec/OpenAPI 兼容性是否完整，见 `layering.md` 与 `quality-security.md`。
- 安全质量：命名、注入、日志、凭据、授权、SQL 注入和错误码是否符合 `quality-security.md`。
- 性能风险：MyBatis 查询、分页、批量写入、N+1 和索引意识是否符合 `performance.md`。
- 验证证据：是否按上方验证选择运行受影响模块测试、OpenSpec、secrets scan、`git diff --check` 或项目等价门禁。

## 红线信号

| 红线信号 | 正确处理 |
| --- | --- |
| 主要理由是“其他文件也这么写” | 找到明确规范，或说明历史实现不一致 |
| create 命令上的 `@PostMapping` 空 path 没有类级资源路径或 API spec 支撑 | 按 `layering.md` 的 Controller 与 API 规则处理 |
| Service import `repository.impl`、`XxxRepositoryImpl` 或 MyBatis mapper | 按 `SKILL.md` 默认规则与 `layering.md` 分层规则处理 |
| 新增长期模块的 `XxxRepository.java` 是具体类 | 按 `architecture.md` 复杂度判断和 `layering.md` 分层规则处理 |
| Controller 包含业务规则 | 按 `layering.md` 的 Controller 与 service/use-case 边界处理 |
| request/response 直接复用 entity 且新增外部契约 | 按 `SKILL.md` API 契约红线和 `layering.md` 对象边界处理 |
| `@Transactional` 写在 controller 或 private 方法 | 按 `layering.md` 事务边界处理 |
| 日志、示例或测试包含真实凭据 | 立即脱敏并检查是否需要停止提交 |
| 只因为全局 skill 或网上示例这么写 | 回到项目规范优先级重新判断 |
| 外部资料提出 `quality-security.md` 已列的不采纳项 | 按 `quality-security.md` 的“不采纳项与正确处理”执行，不在评审表重复展开 |
| 安全配置出现 `/api/**.permitAll()` | 按 `quality-security.md` 安全规则处理 |
| Mapper XML 或 SQL 示例出现生产默认 `SELECT *` / `${用户输入}` / 用户输入 SQL 片段 | 按 `quality-security.md` SQL 安全和 `performance.md` 查询规则处理 |
| 循环内按 id 单条查询或逐条查询关联数据 | 按 `performance.md` 的 N+1 规则处理 |
| 更新 DTO 直接覆盖 entity/PO | 按 `SKILL.md` 更新红线和 `layering.md` 对象边界处理 |
| 事务里包含远程调用、长耗时 IO 或异步副作用 | 按 `layering.md` 事务与副作用边界处理 |
| API 响应字段、错误码或必填性变化但无 OpenSpec/OpenAPI 记录 | 按 `quality-security.md` 错误码和项目 SDD/OpenSpec 处理 |
