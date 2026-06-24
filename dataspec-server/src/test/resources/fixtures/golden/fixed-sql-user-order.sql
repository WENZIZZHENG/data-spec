CREATE TABLE user_order (
    id bigserial NOT NULL,
    created_at timestamp NOT NULL,
    phone varchar(20),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    is_deleted boolean NOT NULL DEFAULT false
);
