CREATE TABLE matches (
    id             UUID PRIMARY KEY,
    lost_item_id   UUID NOT NULL,
    found_item_id  UUID NOT NULL,
    score          DOUBLE PRECISION NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    created_at     TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_matches_lost_item  FOREIGN KEY (lost_item_id)  REFERENCES items (id),
    CONSTRAINT fk_matches_found_item FOREIGN KEY (found_item_id) REFERENCES items (id)
);

CREATE INDEX idx_matches_lost_item  ON matches (lost_item_id);
CREATE INDEX idx_matches_found_item ON matches (found_item_id);
