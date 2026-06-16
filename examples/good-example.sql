-- 示例：用户表（符合 DataSpec 规范）

CREATE TABLE users (
    id          bigserial PRIMARY KEY,
    username    varchar(50)     NOT NULL,
    email       varchar(100)    NOT NULL,
    phone       varchar(20),
    avatar_url  varchar(500),
    status      integer         NOT NULL DEFAULT 0,
    created_at  timestamp with time zone NOT NULL DEFAULT now(),
    updated_at  timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted  boolean         NOT NULL DEFAULT false
);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID（自增主键）';
COMMENT ON COLUMN users.username IS '用户名';
COMMENT ON COLUMN users.email IS '邮箱地址';
COMMENT ON COLUMN users.phone IS '手机号';
COMMENT ON COLUMN users.avatar_url IS '头像URL';
COMMENT ON COLUMN users.status IS '状态：0=正常, 1=禁用';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';
COMMENT ON COLUMN users.is_deleted IS '软删除标记';
