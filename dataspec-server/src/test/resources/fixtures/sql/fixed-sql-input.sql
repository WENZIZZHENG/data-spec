CREATE TABLE UserOrder (
    id bigserial PRIMARY KEY,
    create_time timestamp NOT NULL,
    phone varchar(20)
);
