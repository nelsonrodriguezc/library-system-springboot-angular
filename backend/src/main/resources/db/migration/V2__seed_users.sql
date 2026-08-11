-- Demo accounts for local evaluation. These are throwaway credentials for a disposable
-- database, documented in the README so the reviewer can sign in; they are not secrets
-- and must never be reused outside local development.
--
--   admin@libris.cl          / Admin123!    -> ADMIN
--   bibliotecario@libris.cl  / Biblio123!   -> BIBLIOTECARIO
--   lector@libris.cl         / Demo123!     -> BIBLIOTECARIO

insert into app_user (name, email, password_hash, role) values
    ('María López',   'admin@libris.cl',         '$2a$10$7ZgPKVqrU1Jvj8fNl7I2COS3Hcbi/pnWy71Sr/jp7XCsOQv9z369y', 'ADMIN'),
    ('Carlos Gómez',  'bibliotecario@libris.cl', '$2a$10$NWpdtZK18XASx7jlB/OQnOvKbBN8/xzUH.1.C/P44VCh8Lu98pC4i', 'BIBLIOTECARIO'),
    ('Ana Torres',    'lector@libris.cl',        '$2a$10$I3eA1WZt2pWZ5s71SAvXXuaO27RGWpHnwxjpXZkivoZ7rcaWFP8Lm', 'BIBLIOTECARIO');
