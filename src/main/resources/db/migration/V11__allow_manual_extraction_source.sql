-- Los ingresos ya no se detectan por correo (ver ExtractionPrompts) -- se cargan a mano
-- desde la app. extraction_source necesita un tercer valor para reflejar eso con precisión
-- en vez de mentir con 'LLM'/'CACHE' en una fila que nunca pasó por ninguno de los dos.
ALTER TABLE transactions DROP CONSTRAINT transactions_extraction_source_check;
ALTER TABLE transactions
    ADD CONSTRAINT transactions_extraction_source_check
        CHECK (extraction_source IN ('LLM', 'CACHE', 'MANUAL'));
