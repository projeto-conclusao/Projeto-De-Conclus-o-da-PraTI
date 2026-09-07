CREATE TABLE items (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL,
    category_id    UUID NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ANALISANDO',
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    location_text  VARCHAR(255),
    latitude       DOUBLE PRECISION,
    longitude      DOUBLE PRECISION,
    event_date     TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_items_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_items_type_category ON items (type, category_id);
CREATE INDEX idx_items_user          ON items (user_id);
CREATE INDEX idx_items_status        ON items (status);
