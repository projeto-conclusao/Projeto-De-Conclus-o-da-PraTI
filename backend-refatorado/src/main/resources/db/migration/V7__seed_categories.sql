-- UUIDs fixos (não gerados) para não depender de extensões como pgcrypto/uuid-ossp.
INSERT INTO categories (id, name, created_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Eletrônicos',          now()),
    ('22222222-2222-2222-2222-222222222222', 'Documentos',           now()),
    ('33333333-3333-3333-3333-333333333333', 'Roupas e Acessórios',  now()),
    ('44444444-4444-4444-4444-444444444444', 'Chaves',               now()),
    ('55555555-5555-5555-5555-555555555555', 'Animais de Estimação', now()),
    ('66666666-6666-6666-6666-666666666666', 'Outros',               now());
