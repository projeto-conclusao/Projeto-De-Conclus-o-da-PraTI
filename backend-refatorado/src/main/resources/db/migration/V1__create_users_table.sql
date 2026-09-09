CREATE TABLE users (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255),
    role        VARCHAR(20)  NOT NULL,
    provider    VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users (email);
