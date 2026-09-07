-- Una conexión de Gmail puede quedar inutilizable sin que el token expire "normalmente": el
-- usuario revoca el acceso desde su cuenta de Google (refresh -> invalid_grant), o el grant
-- se hizo antes de que la app pidiera el scope gmail.readonly (list de mensajes -> 403
-- ACCESS_TOKEN_SCOPE_INSUFFICIENT). En ambos casos reintentar en cada poll no arregla nada:
-- solo el usuario reconectando lo resuelve. sync_error marca esa fila para dejar de
-- reintentarla y para que el frontend muestre "reconectar"; se limpia sola en el primer
-- sync exitoso posterior (típicamente tras reconectar).
ALTER TABLE gmail_connections
    ADD COLUMN sync_error    VARCHAR(32),
    ADD COLUMN sync_error_at TIMESTAMPTZ;
