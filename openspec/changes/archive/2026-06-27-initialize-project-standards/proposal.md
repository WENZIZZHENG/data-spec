## Why

P2-1 需要让新项目创建后立即拥有可用的字段标准和数据域。当前 `standards/fields/standard-fields.yaml` 与 `standards/domains/standard-domains.yaml` 已存在，但项目创建后仍是空数据，字段推荐、AI Context 导出和 SQL lint 都缺少项目级标准素材。

## What Changes

- 创建项目时默认导入内置标准字段和数据域。
- 创建项目 API 增加可选开关，允许调用方跳过内置 standards 导入。
- 后端从 classpath 加载内置 YAML，避免打包后依赖仓库根目录文件。
- 新建项目页面增加导入内置标准的开关。
- TODO 路线图同步更新 P2-1 状态。

## Scope

- 第一版只覆盖项目创建时的一次性初始化。
- 导入逻辑按项目维度去重，避免重复调用产生重复字段或数据域。
- 不做 Excel 导入导出、导入预览、标准版本迁移或复杂冲突合并。

## Impact

- `/api/projects` 创建请求体新增可选字段 `importBuiltInStandards`。
- 新项目默认包含内置数据域和标准字段。
- 内置 standards YAML 增加后端 classpath 副本。
