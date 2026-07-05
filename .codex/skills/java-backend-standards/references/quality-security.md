# Java 质量、安全与命名规则

## 参考取舍

吸收：

- Java 17+/21、Spring Boot 3.x、Jakarta 包名、Spring Security 6 DSL、外部化配置、Bean Validation、统一异常、测试、安全、事务和生产可维护性基线。
- DTO/DO/PO 隔离、Factory/Converter 转换边界、Repository 接口与实现分离、参数数量控制、更新合并旧数据、SQL 注入检查和安全审查清单。

不采纳项与正确处理：

| 不采纳项 | 正确处理 |
| --- | --- |
| 接口统一加 `I` 前缀 | 按 `SKILL.md` 命名规则和本文件接口命名说明处理 |
| 禁止所有 `//` 注释 | 按本文件注释规则，仅为业务约束、兼容、安全边界和非显而易见原因补注释 |
| 强制类/方法/字段全量 Javadoc | 公共代码表面按项目 `AGENTS.md` 补职责和字段语义，直观样板不写空泛注释 |
| `/api/**.permitAll()` | 按本文件 Spring Security 规则逐项列出公开路径，业务 API 默认认证或拒绝 |
| 示例明文数据库密码 | 使用脱敏示例，不写真实账号、JDBC URL、DSN、token 或 secret |
| 生产查询默认 `SELECT *` | 按 SQL 安全规则显式列字段；性能场景同时看 `performance.md` |
| 裸 `@PostMapping` 作为所有 create 默认规范 | 按 `layering.md` 的 Controller/API 规则判断 collection-style `POST /resource` 是否成立 |
| 把所有 `@PostMapping` 空 path 都判为错误 | 结合类级资源路径、API spec 和映射测试判断 |
| 直接把外部示例目录结构强套到当前项目 | 按 `architecture.md` 复杂度判断选择结构 |

## Java 质量基线

- 使用构造器注入；依赖字段使用 `private final`。
- 不使用 field injection，不新增 wildcard import。
- 接口命名遵循 `SKILL.md` 的默认规则；补充理由是 `I` 前缀表达技术类型而非业务角色，孤立历史实现不能作为命名规范。
- 实现类使用 `XxxServiceImpl`、`XxxRepositoryImpl`；多实现时用 `Default`、`MyBatis`、`Cached` 等表达实现策略。
- 遵循单文件单顶层 public 类型；import 不使用通配符，静态导入保持克制。
- 方法参数超过 4-5 个或语义成组时优先封装为 request/command/query 对象。
- `find*` 类方法可以返回 `Optional<T>` 表达可能不存在；不要对 `Optional` 直接 `get()`。
- `Optional` 只用于返回值，不用于字段、DTO 属性或方法参数。
- DTO 可优先使用 `record` 或不可变对象；entity 是否可变按 ORM/MyBatis 映射需要决定。
- Entity 不使用 Lombok `@Data`；按需要使用 `@Getter`、`@Setter`，避免 equals/hashCode、toString 暴露敏感字段或触发 ORM 复杂对象图。DTO/record 不受同一限制。
- stream 用于清晰的数据转换；复杂分支、异常处理或多层嵌套时优先普通循环。
- 异常要有业务语义和上下文；避免无差别 `catch (Exception)` 后吞掉或只打印。
- 不使用 `System.out` / `printStackTrace`；使用项目日志方式和统一异常处理。
- 魔法值提取为常量、枚举或配置；业务枚举值要和持久化/API 契约保持一致。
- 注释允许使用 `//`；注释重点解释业务约束、兼容性、安全边界和非显而易见原因，不为简单 getter/setter 或直观代码补空泛 Javadoc。
- Spring Boot 3.x 新代码使用 `jakarta.*` 相关 API，不新增 `javax.*` Bean Validation / Servlet 依赖；Spring Security 6 使用 `SecurityFilterChain` 和 `authorizeHttpRequests` 风格。

## 安全、配置与 SQL

- 不硬编码 secret、token、Authorization、JDBC URL、DSN、生产地址或真实账号。
- 示例配置也必须脱敏；不要把 `password: postgres`、生产地址、真实用户名写进 README、测试快照、OpenSpec 或 skill 示例。
- 需要配置时优先使用项目已有配置方式；新增成组配置优先类型安全配置，散落 `@Value` 只用于简单低风险值。
- 对用户输入、AI 输入、导入文件、SQL 片段、外部 URL 和路径参数做边界校验。
- 日志只记录必要上下文，如 id、projectId、状态和错误摘要；不得记录 raw request body 中的敏感字段。
- 日志不得输出 raw secret、token、Authorization、JDBC URL、DSN 或完整敏感请求对象。
- Spring Security 配置默认拒绝或要求认证，公开路径必须逐项列出，如健康检查、登录、公开资源等低风险路径；不得用 `/api/**.permitAll()` 放开业务 API。
- 对象归属、项目归属和越权访问校验放在 service/use-case 边界；Controller 或 filter 的认证结果不能替代业务授权。
- CSRF、CORS、Actuator 暴露和 method security 按应用类型明确配置；状态型浏览器应用不能无理由关闭 CSRF。
- 使用 `.last(...)`、`apply`、`inSql`、`exists` 或 XML 动态片段拼 SQL 结构时，只允许内部安全常量、数字或白名单值，不能拼用户输入。
- 动态排序字段、表名、列名、方向、分片名和无法参数绑定的 SQL 结构必须走 allowlist，并覆盖篡改测试。
- SQL / XML mapper 禁止使用 `${}` 拼接用户输入；使用 `#{}` 参数绑定或类型安全 wrapper。
- 生产代码和外部响应来源查询字段优先显式列出；避免 `SELECT *` 成为新代码或示例规范。临时探查、窄表内部 mapper 或受控视图例外不得扩大为默认规范。
- Repository 查询需要稳定排序；分页查询必须有最大 page size 或 limit。

## 错误码与错误响应

- 新增 API 错误必须有稳定错误码、用户可理解消息和开发者可定位上下文；不要只返回异常类名或数据库错误。
- 错误码分段可采用项目默认示例：`10xxx` 认证/权限，`20xxx` 参数和业务校验，`30xxx` 数据冲突和状态冲突，`40xxx` 外部依赖或集成失败，`50xxx` 系统异常。
- 错误体不得包含 raw secret、SQL、堆栈、JDBC URL、Authorization 或完整敏感请求体；日志和响应使用不同详细程度。
- 修改错误码、错误体字段、HTTP 状态码或兼容语义时，按 API 契约变更处理。
