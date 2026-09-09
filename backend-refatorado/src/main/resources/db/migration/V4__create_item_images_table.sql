CREATE TABLE item_images (
    id             UUID PRIMARY KEY,
    item_id        UUID NOT NULL,
    url            VARCHAR(500) NOT NULL,
    display_order  INT,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_item_images_item FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE
);

CREATE INDEX idx_item_images_item ON item_images (item_id);
