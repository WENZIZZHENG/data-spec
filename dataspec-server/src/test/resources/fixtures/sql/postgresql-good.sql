CREATE TABLE user_order (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL,
    amount_cent bigint NOT NULL DEFAULT 0,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE user_order IS '用户订单表';
COMMENT ON COLUMN user_order.id IS '主键';
COMMENT ON COLUMN user_order.user_id IS '用户ID';
COMMENT ON COLUMN user_order.amount_cent IS '订单金额，单位分';
COMMENT ON COLUMN user_order.created_at IS '创建时间';
COMMENT ON COLUMN user_order.updated_at IS '更新时间';
COMMENT ON COLUMN user_order.is_deleted IS '是否删除';
