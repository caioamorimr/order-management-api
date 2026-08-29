CREATE TABLE tb_refresh_token
(
    id         BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(255)             NOT NULL UNIQUE,
    user_id    BIGINT                   NOT NULL REFERENCES tb_user (id),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN                  NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_refresh_token_user_id ON tb_refresh_token (user_id);