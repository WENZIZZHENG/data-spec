CREATE TABLE UserOrder (
    id BIGINT UNSIGNED NOT NULL,
    userId BIGINT,
    amount DECIMAL(10,2) UNSIGNED,
    is_paid TINYINT(1),
    create_time DATETIME,
    KEY idx_user_id (userId)
) COMMENT='订单表';
