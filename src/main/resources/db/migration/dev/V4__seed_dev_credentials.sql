-- Mismo id que el usuario demo de V2, ahora con credenciales reales para poder probar
-- sign-in vía IAM. Password: "demo1234" (hash BCrypt precalculado; nunca se guarda en
-- texto plano ni siquiera en dev).
INSERT INTO credentials (id, email, hashed_password)
VALUES ('11111111-1111-1111-1111-111111111111', 'demo@smartcash.pe',
        '$2a$10$Wu.HweZiqCoeCYqm1.4qf.qXjZ2r8Ow4Xgm6vwF2E2WYTu5CwzWpq');
