-- =========================================================
-- CineTicket - 03_seed_minimo.sql
-- Semillas mínimas para probar el flujo de compra
--  - Usuario admin demo
--  - 4 salas (con asientos autogenerados)
--  - 3 películas (2 funciones c/u)
--  - 5 combos de confitería
-- Idempotente: se puede ejecutar varias veces
-- =========================================================
BEGIN;

-- ===== Usuario admin demo (pass: Admin123) =====
-- Hash Bcrypt de "Admin123" (solo demo)
INSERT INTO usuario (nombre_completo, correo_electronico, nombre_usuario, contrasena_hash, rol)
VALUES ('Administrador', 'admin@cineticket.com', 'admin',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIqn8nOKDm',  -- "Admin123"
        'ADMIN')
ON CONFLICT DO NOTHING;

-- ===== Géneros mínimos (opcional, por si los usas en cartelera) =====
INSERT INTO genero (nombre_genero, descripcion) VALUES
 ('Acción','Películas de alta adrenalina'),
 ('Aventura','Historias épicas y viajes'),
 ('Ciencia Ficción','Tecnología y futuros alternativos')
ON CONFLICT (nombre_genero) DO NOTHING;

-- ===== Salas (4 salas) =====
-- Usa tamaños pequeños para que sea visible en UI
INSERT INTO sala (nombre_sala, capacidad_total, filas, columnas) VALUES
 ('Sala 1', 60, 6, 10),
 ('Sala 2', 60, 6, 10),
 ('Sala 3', 80, 8, 10),
 ('Sala 4', 40, 5, 8)
ON CONFLICT (nombre_sala) DO NOTHING;

-- ==== Utilidad para etiquetar filas: A..Z, AA.. (1-based) ====
CREATE OR REPLACE FUNCTION label_fila(idx int) RETURNS text AS $$
DECLARE
  n int := idx;
  res text := '';
  rem int;
BEGIN
  IF n < 1 THEN RAISE EXCEPTION 'idx debe ser >= 1'; END IF;
  WHILE n > 0 LOOP
    rem := (n - 1) % 26;
    res := chr(65 + rem) || res;
    n := (n - 1) / 26;
  END LOOP;
  RETURN res;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- ==== Generación de asientos por sala según filas/columnas ====
-- Inserta si no existen (idempotente por (sala_id,fila,numero))
DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN (SELECT id_sala, filas, columnas FROM sala) LOOP
    INSERT INTO asiento (sala_id, fila, numero, tipo_asiento, activo)
    SELECT r.id_sala,
           label_fila(fi) AS fila,
           co AS numero,
           CASE
             WHEN fi = 1 THEN 'PREFERENCIAL'::tipo_asiento
             WHEN fi >= GREATEST(r.filas - 1, 2) THEN 'VIP'::tipo_asiento
             ELSE 'REGULAR'::tipo_asiento
           END,
           TRUE
    FROM generate_series(1, r.filas)  AS fi
    CROSS JOIN generate_series(1, r.columnas) AS co
    ON CONFLICT (sala_id, fila, numero) DO NOTHING;
  END LOOP;
END $$;

-- ===== Películas (3) =====
INSERT INTO pelicula (titulo, duracion_minutos, clasificacion, sinopsis, fecha_estreno, activa)
VALUES
 ('Amanecer Galáctico', 130, '12+', 'Tripulación enfrenta un misterio a las afueras del sistema solar.', CURRENT_DATE, TRUE),
 ('Ciudad Sombría',     118, '15+', 'Un detective persigue a un criminal en una urbe corrupta.',     CURRENT_DATE, TRUE),
 ('Risas en Familia',    95, 'T',   'Una familia intenta unas vacaciones perfectas… casi.',            CURRENT_DATE, TRUE)
ON CONFLICT DO NOTHING;

-- (Opcional) Mapear géneros si quieres
DO $$
DECLARE
  pid1 int; pid2 int; pid3 int;
  g1 int; g2 int; g3 int;
BEGIN
  SELECT id_pelicula INTO pid1 FROM pelicula WHERE titulo='Amanecer Galáctico';
  SELECT id_pelicula INTO pid2 FROM pelicula WHERE titulo='Ciudad Sombría';
  SELECT id_pelicula INTO pid3 FROM pelicula WHERE titulo='Risas en Familia';
  SELECT id_genero   INTO g1   FROM genero   WHERE nombre_genero='Ciencia Ficción';
  SELECT id_genero   INTO g2   FROM genero   WHERE nombre_genero='Acción';
  SELECT id_genero   INTO g3   FROM genero   WHERE nombre_genero='Aventura';

  IF pid1 IS NOT NULL AND g1 IS NOT NULL THEN
    INSERT INTO pelicula_genero(pelicula_id,genero_id) VALUES (pid1,g1) ON CONFLICT DO NOTHING;
  END IF;
  IF pid2 IS NOT NULL AND g2 IS NOT NULL THEN
    INSERT INTO pelicula_genero(pelicula_id,genero_id) VALUES (pid2,g2) ON CONFLICT DO NOTHING;
  END IF;
  IF pid3 IS NOT NULL AND g3 IS NOT NULL THEN
    INSERT INTO pelicula_genero(pelicula_id,genero_id) VALUES (pid3,g3) ON CONFLICT DO NOTHING;
  END IF;
END $$;

-- ===== Funciones: 2 por película (horarios hoy 18:00 y 21:00) =====
-- Evitamos solapes usando distintas salas o bloques 18-20 / 21-23
DO $$
DECLARE
  p1 int; p2 int; p3 int;
  s1 int; s2 int; s3 int; s4 int;
  f_ini1 timestamp := (CURRENT_DATE + time '18:00');
  f_fin1 timestamp := (CURRENT_DATE + time '20:00');
  f_ini2 timestamp := (CURRENT_DATE + time '21:00');
  f_fin2 timestamp := (CURRENT_DATE + time '23:00');
BEGIN
  SELECT id_pelicula INTO p1 FROM pelicula WHERE titulo='Amanecer Galáctico';
  SELECT id_pelicula INTO p2 FROM pelicula WHERE titulo='Ciudad Sombría';
  SELECT id_pelicula INTO p3 FROM pelicula WHERE titulo='Risas en Familia';

  SELECT id_sala INTO s1 FROM sala WHERE nombre_sala='Sala 1';
  SELECT id_sala INTO s2 FROM sala WHERE nombre_sala='Sala 2';
  SELECT id_sala INTO s3 FROM sala WHERE nombre_sala='Sala 3';
  SELECT id_sala INTO s4 FROM sala WHERE nombre_sala='Sala 4';

  -- Amanecer Galáctico
  IF p1 IS NOT NULL AND s1 IS NOT NULL THEN
    INSERT INTO funcion (pelicula_id, sala_id, fecha_hora_inicio, fecha_hora_fin, precio_entrada)
    VALUES (p1, s1, f_ini1, f_fin1, 25000.00)
    ON CONFLICT DO NOTHING;
  END IF;
  IF p1 IS NOT NULL AND s1 IS NOT NULL THEN
    INSERT INTO funcion (pelicula_id, sala_id, fecha_hora_inicio, fecha_hora_fin, precio_entrada)
    VALUES (p1, s1, f_ini2, f_fin2, 25000.00)
    ON CONFLICT DO NOTHING;
  END IF;

  -- Ciudad Sombría
  IF p2 IS NOT NULL AND s2 IS NOT NULL THEN
    INSERT INTO funcion (pelicula_id, sala_id, fecha_hora_inicio, fecha_hora_fin, precio_entrada)
    VALUES (p2, s2, f_ini1, f_fin1, 22000.00)
    ON CONFLICT DO NOTHING;
  END IF;
  IF p2 IS NOT NULL AND s2 IS NOT NULL THEN
    INSERT INTO funcion (pelicula_id, sala_id, fecha_hora_inicio, fecha_hora_fin, precio_entrada)
    VALUES (p2, s2, f_ini2, f_fin2, 22000.00)
    ON CONFLICT DO NOTHING;
  END IF;

  -- Risas en Familia
  IF p3 IS NOT NULL AND s3 IS NOT NULL THEN
    INSERT INTO funcion (pelicula_id, sala_id, fecha_hora_inicio, fecha_hora_fin, precio_entrada)
    VALUES (p3, s3, f_ini1, f_fin1, 18000.00)
    ON CONFLICT DO NOTHING;
  END IF;
  IF p3 IS NOT NULL AND s4 IS NOT NULL THEN
    INSERT INTO funcion (pelicula_id, sala_id, fecha_hora_inicio, fecha_hora_fin, precio_entrada)
    VALUES (p3, s4, f_ini2, f_fin2, 18000.00)
    ON CONFLICT DO NOTHING;
  END IF;
END $$;

-- ===== Combos de confitería (5) =====
INSERT INTO combo_confiteria (nombre_combo, descripcion, precio, categoria, disponible)
VALUES
 ('Agua Embotellada', 'Agua mineral 500ml',                 4000,  'Bebidas', TRUE),
 ('Gaseosa Grande',   'Bebida gaseosa 32oz',                6000,  'Bebidas', TRUE),
 ('Nachos con Queso', 'Nachos con salsa de queso',          8000,  'Snacks',  TRUE),
 ('Combo Clásico',    'Palomitas medianas + Gaseosa med.', 18000,  'Combos',  TRUE),
 ('Combo Familiar',   '2 Palomitas grandes + 4 Gaseosas',  40000,  'Combos',  TRUE)
ON CONFLICT (nombre_combo) DO NOTHING;

COMMIT;

-- Tips:
-- 1) Si ejecutas esto cerca de medianoche y quieres horarios "mañana",
--    cambia CURRENT_DATE por (CURRENT_DATE + 1).
-- 2) Si cambias filas/columnas de una sala, vuelve a correr el bloque de asientos.
