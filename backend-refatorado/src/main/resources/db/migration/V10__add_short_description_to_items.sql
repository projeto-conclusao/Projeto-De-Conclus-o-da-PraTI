ALTER TABLE items
    ADD COLUMN short_description VARCHAR(100) NOT NULL DEFAULT '';

ALTER TABLE items
    ALTER COLUMN short_description DROP DEFAULT;
