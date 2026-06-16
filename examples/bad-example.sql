-- 示例：不符合规范的建表语句（用于测试 SQL 校验功能）

CREATE TABLE UserOrder (
    userId bigint NOT NULL,
    uid bigint NOT NULL,
    create_time datetime NOT NULL,
    update_time datetime NOT NULL,
    totalAmount float NOT NULL,
    status int NOT NULL
);

-- 预期校验结果:
-- ERROR: 表名 UserOrder 不符合 snake_case
-- ERROR: 字段 userId 不符合 snake_case
-- ERROR: uid 是禁用字段名
-- ERROR: create_time 是禁用字段名
-- ERROR: update_time 是禁用字段名
-- ERROR: 缺少必含列 id, created_at, updated_at, is_deleted
-- SUGGESTION: create_time 建议改为 created_at
-- SUGGESTION: update_time 建议改为 updated_at
-- WARNING: totalAmount 金额字段不应使用 float
-- SUGGESTION: 所有字段和表缺少注释
