CREATE TABLE tb_user
(
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(150) NOT NULL UNIQUE,
    phone    VARCHAR(20)  NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE tb_category
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE tb_product
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)   NOT NULL,
    description VARCHAR(1000),
    price       NUMERIC(19, 2) NOT NULL,
    img_url     VARCHAR(500)
);

CREATE TABLE tb_product_category
(
    product_id  BIGINT NOT NULL REFERENCES tb_product (id),
    category_id BIGINT NOT NULL REFERENCES tb_category (id),
    PRIMARY KEY (product_id, category_id)
);

CREATE TABLE tb_order
(
    id           BIGSERIAL PRIMARY KEY,
    moment       TIMESTAMP WITH TIME ZONE NOT NULL,
    order_status INTEGER                  NOT NULL,
    client_id    BIGINT REFERENCES tb_user (id)
);

CREATE TABLE tb_payment
(
    order_id BIGINT PRIMARY KEY REFERENCES tb_order (id),
    moment TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE tb_order_item
(
    order_id   BIGINT         NOT NULL REFERENCES tb_order (id),
    product_id BIGINT         NOT NULL REFERENCES tb_product (id),
    quantity   INTEGER        NOT NULL,
    price      NUMERIC(19, 2) NOT NULL,
    PRIMARY KEY (order_id, product_id)
);