-- Usuario fijo para probar el webhook en dev/local sin tener que crear uno primero.
-- id predecible a propósito: se usa directamente en el ejemplo de curl del README.
INSERT INTO users (id, email, display_name)
VALUES ('11111111-1111-1111-1111-111111111111', 'demo@smartcash.pe', 'Usuario Demo');
