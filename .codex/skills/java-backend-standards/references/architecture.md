# 架构与目录结构选择

不要把当前项目目录或外部示例目录当成所有场景的唯一答案。先按业务复杂度、生命周期、团队规模和契约稳定性选择结构。

| 场景 | 推荐结构 | 说明 |
| --- | --- | --- |
| 当前 data-spec 或小团队快速闭环 | 按能力模块组织：`feature/controller`、`feature/service`、`feature/repository`、`feature/mapper`、`feature/model`、`feature/entity` | 保持最小可用闭环，避免为简单 CRUD 引入过重目录 |
| 新项目但业务中等复杂 | 按业务模块组织，模块内拆 `api/model/service/repository/mapper/entity` | 兼顾可读性和演进空间 |
| 复杂业务、核心域、长期演进 | 采用应用层/领域层/基础设施层，或 Clean/Hexagonal 风格：`api`、`application`、`domain`、`infrastructure/persistence` | 适合复杂规则、长期契约、多人协作和多种持久化/外部适配 |

## 复杂度判断标准

满足以下条件时，可按轻量模块处理，不强制拆完整接口或领域层：单表或主从表 CRUD、无跨服务调用、无复杂状态流转、无跨资源一致性、无外部副作用、校验仅限非空/长度/范围等简单规则，且不是长期公共契约核心。

出现以下任一信号时，必须拆清接口和架构边界：涉及 2 张以上核心表协作、状态流转、对象归属/权限、跨服务协调、远程调用、批量导入、幂等、并发写、稳定公共 API、长期领域规则或多团队协作。

复杂长期项目可以借鉴外部示例或历史材料中的分层思想，但要按项目命名和依赖方向重塑：

```text
user/
  api/
    controller/
    model/
    converter/
  application/
    service/
    port/
      out/
  domain/
    model/
    repository/
  infrastructure/
    persistence/
      entity/
      mapper/
      repository/
```

取舍规则：

- 当前项目内新增普通模块时，优先保持现有 feature/module 风格，只在模块内部强化接口、DTO、RepositoryImpl、Mapper 隔离。
- 当前项目风格只是低风险增量维护的默认选项，不是新项目模板，也不是重构复杂域时的约束。
- 新项目或核心复杂域可以采用 DTO -> DO/domain model -> PO/entity 的完整边界；复杂度没出现前不要强制创建空洞的 domain/factory 层。
- 外部示例中的 `application`、`domain`、`repo/persistence` 思想可用于新项目，但不要原样强套包名。
- 一旦选择复杂架构，必须同步定义依赖规则：API 不依赖 persistence，domain 不依赖 Spring/MyBatis，Repository port/interface 位于 `application/port/out` 或 `domain/repository`，`RepositoryImpl`、MyBatis mapper、entity 位于 infrastructure。
