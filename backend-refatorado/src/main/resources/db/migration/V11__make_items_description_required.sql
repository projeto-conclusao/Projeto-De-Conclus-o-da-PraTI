UPDATE items SET description = '' WHERE description IS NULL;

ALTER TABLE items
    ALTER COLUMN description SET NOT NULL;
