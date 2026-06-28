## Context

P6-40 的核心目标不是企业级任务编排，而是让 AI/CLI 自动重试和前端重复点击不会在个人版里制造明显重复数据。当前高风险写入大多是同步服务调用，且项目默认运行在单个后端实例，因此单机内存保护能以最小改动覆盖主要风险。

## Decisions

### 1. 使用单机内存幂等缓存，不新增数据库表

第一版采用 `ConcurrentHashMap` 保存成功结果和失败摘要，key 由 `projectId + operation + idempotencyKey` 组成。原因：

- 避免 Flyway/实体/清理任务膨胀，把本轮聚焦在写入口保护。
- 个人/小团队默认单实例运行，能覆盖重复点击、CLI retry 和同一 agent 重试。
- 服务重启后的历史幂等不做承诺，在 README/TODO 边界中说明。

### 2. 锁使用项目级 operation try-lock

高风险写入进入业务逻辑前先获取 `projectId + operation` 的 `ReentrantLock`。获取失败时立即抛出业务异常，错误消息包含“操作正在进行”，由 `ErrorCatalog` 映射为 retryable 的冲突诊断。

### 3. Header 优先，服务层可显式传 key

Controller 读取 `Idempotency-Key` header 并传给服务方法，避免 ThreadLocal 隐式耦合。内部 AI job 记录没有 controller，因此按稳定请求指纹自动去重。

### 4. 不复用幂等 key 跨 operation

同一个 key 可在不同 operation 下独立使用，避免 CLI 全局 key 同时触发 snapshot 和 batch 时互相污染。重复 key 的返回值只在同一 project/operation 下复用。

### 5. 不让幂等保护吞掉真实失败

失败默认不缓存为成功结果；调用方重试同一个 key 时会重新执行。锁冲突使用 409，提示稍后重试。

## Open Questions

- 后续如果要支持多实例或重启后幂等，需要新增持久化 idempotency 表和 TTL 清理任务，本轮不做。
- 如果前端需要自动生成 key，可后续在请求拦截器或关键按钮处加入，本轮优先后端和 CLI。
