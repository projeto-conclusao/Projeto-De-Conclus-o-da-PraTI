CREATE TABLE messages (
    id          UUID PRIMARY KEY,
    match_id    UUID NOT NULL,
    sender_id   UUID NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_messages_match  FOREIGN KEY (match_id)  REFERENCES matches (id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)
);

CREATE INDEX idx_messages_match_created ON messages (match_id, created_at);
