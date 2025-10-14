-- =========================================================
-- CineTicket - 03_seed.sql
-- Datos semilla: admin, géneros, salas, asientos, combos, películas
-- Seguro de ejecutar múltiples veces (usa ON CONFLICT donde aplica)
-- =========================================================

BEGIN;

-- (Opcional)
-- SET search_path TO cineticket, public;

-- Usuario admin (hash Bcrypt de "Admin123" a modo de demo)
INSERT INTO usuario (nombre_completo, correo_electronico, nombre_usuario, contrasena_hash, rol)
VALUES ('Administrador del Sistema', 'admin@cineticket.com', 'admin',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIqn8nOKDm',
        'ADMIN')
ON CONFLICT (correo_electronico) DO NOTHING;

-- Géneros
INSERT INTO genero (nombre_genero, descripcion) VALUES
 ('Acción','Películas de alta adrenalina'),
 ('Aventura','Historias épicas y viajes'),
 ('Ciencia Ficción','Tecnología y futuros alternativos'),
 ('Comedia','Películas humorísticas'),
 ('Drama','Historias emotivas y realistas'),
 ('Terror','Películas de miedo'),
 ('Suspenso','Thrillers y misterio'),
 ('Animación','Películas animadas'),
 ('Romance','Historias de amor'),
 ('Documental','Contenido educativo y real')
ON CONFLICT (nombre_genero) DO NOTHING;

-- Salas
INSERT INTO sala (nombre_sala, capacidad_total, filas, columnas) VALUES
 ('Sala 1', 60, 6, 10),
 ('Sala 2', 80, 8, 10),
 ('Sala VIP', 40, 5, 8)
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
-- Limpia y repuebla cada sala si ya existe (idempotente por (sala_id,fila,numero))
DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN (SELECT id_sala, filas, columnas FROM sala) LOOP
    -- opcional: si quieres regenerar siempre, descomenta:
    -- DELETE FROM asiento WHERE sala_id = r.id_sala;

    INSERT INTO asiento (sala_id, fila, numero, tipo_asiento)
    SELECT r.id_sala,
           label_fila(fi) AS fila,
           co AS numero,
           CASE
             WHEN fi = 1 THEN 'PREFERENCIAL'::tipo_asiento
             WHEN fi >= GREATEST(r.filas - 1, 2) THEN 'VIP'::tipo_asiento
             ELSE 'REGULAR'::tipo_asiento
           END
    FROM generate_series(1, r.filas)  AS fi
    CROSS JOIN generate_series(1, r.columnas) AS co
    ON CONFLICT (sala_id, fila, numero) DO NOTHING;
  END LOOP;
END $$;

-- Combos de confitería
INSERT INTO combo_confiteria (nombre_combo, descripcion, precio, categoria) VALUES
 ('Combo Clásico', 'Palomitas medianas + Gaseosa mediana', 8.50, 'Combos'),
 ('Combo Grande', 'Palomitas grandes + Gaseosa grande + Nachos', 12.00, 'Combos'),
 ('Combo Dulce', 'Chocolate + Galletas + Gaseosa pequeña', 7.00, 'Combos'),
 ('Palomitas Grandes', 'Palomitas de maíz tamaño grande', 5.50, 'Snacks'),
 ('Nachos con Queso', 'Nachos con salsa de queso', 4.50, 'Snacks'),
 ('Gaseosa Grande', 'Bebida gaseosa 32oz', 3.50, 'Bebidas'),
 ('Agua Embotellada', 'Agua mineral 500ml', 2.00, 'Bebidas'),
 ('Combo Familiar', '2 Palomitas grandes + 4 Gaseosas', 18.00, 'Combos')
ON CONFLICT (nombre_combo) DO NOTHING;

-- Películas demo
INSERT INTO pelicula (titulo, duracion_minutos, clasificacion, sinopsis, fecha_estreno, activa)
VALUES
-- 🎬 Acción / Aventura / Ciencia ficción
('Avatar 3', 190, '12+', 'Jake y Neytiri enfrentan nuevos desafíos en Pandora junto a su familia.', '2025-12-19', TRUE),
('Misión Imposible – Dead Reckoning Part Two', 160, '15+', 'Ethan Hunt continúa su lucha contra una inteligencia artificial que amenaza al mundo.', '2025-05-23', TRUE),
('Dune: Part Two', 166, '12+', 'Paul Atreides une fuerzas con los Fremen para vengar a su familia y cambiar el destino del universo.', '2024-03-01', TRUE),
('Oppenheimer', 180, '15+', 'La historia del científico que lideró el desarrollo de la bomba atómica.', '2023-07-21', TRUE),
('The Batman', 176, '15+', 'Bruce Wayne investiga a un asesino en una Gotham corrupta.', '2022-03-04', TRUE),

-- 🎞️ Animación (DreamWorks / Ghibli / Pixar)
('Inside Out 2', 100, 'T', 'Riley enfrenta la adolescencia con nuevas emociones como la Ansiedad y la Envidia.', '2024-06-14', TRUE),
('How to Train Your Dragon: The Hidden World', 104, 'T', 'Hipo y Chimuelo descubren un mundo oculto mientras buscan proteger a su tribu.', '2019-02-22', TRUE),
('Kung Fu Panda 4', 95, 'T', 'Po busca su sucesor mientras enfrenta a una nueva villana que puede robar los poderes de sus enemigos.', '2024-03-08', TRUE),
('Puss in Boots: The Last Wish', 102, 'T', 'El Gato con Botas se embarca en una aventura para recuperar sus vidas perdidas.', '2022-12-21', TRUE),
('Spirited Away', 125, 'T', 'Una niña entra a un mundo mágico donde debe rescatar a sus padres transformados en cerdos.', '2001-07-20', TRUE),
('My Neighbor Totoro', 86, 'T', 'Dos hermanas descubren a un espíritu del bosque mientras se adaptan a su nuevo hogar.', '1988-04-16', TRUE),
('Howl’s Moving Castle', 119, 'T', 'Sophie es transformada en anciana por una maldición y busca ayuda en el misterioso castillo de Howl.', '2004-11-20', TRUE),
('The Boy and the Heron', 124, '12+', 'Un joven descubre un mundo fantástico mientras lidia con la pérdida y el crecimiento.', '2023-07-14', TRUE),

-- 💕 Romance / Drama / Comedia
('La La Land', 128, '12+', 'Una actriz y un músico luchan por sus sueños y su amor en Los Ángeles.', '2016-12-09', TRUE),
('The Notebook', 123, '12+', 'Una historia de amor eterno contada a través de los años.', '2004-06-25', TRUE),
('Barbie', 114, 'T', 'Barbie comienza a cuestionarse el sentido de su existencia y explora el mundo real.', '2023-07-21', TRUE),

-- 👻 Terror / Suspenso
('A Quiet Place', 90, '15+', 'Una familia debe vivir en silencio para sobrevivir a criaturas que cazan por el sonido.', '2018-04-06', TRUE),
('Get Out', 104, '15+', 'Un joven afroamericano descubre un inquietante secreto en la familia de su novia.', '2017-02-24', TRUE),
('Parasite', 132, '15+', 'Una familia pobre se infiltra en la vida de una familia rica con consecuencias inesperadas.', '2019-05-30', TRUE)
ON CONFLICT DO NOTHING;


-- Mapear algunos géneros (asumiendo ids insertados en orden)
-- Si ya existen, ajusta los IDs según tu data real.
INSERT INTO pelicula_genero (pelicula_id, genero_id) VALUES
-- Avatar 3
(1,1),(1,2),(1,3),
-- Misión Imposible – Dead Reckoning Part Two
(2,1),(2,7),
-- Dune: Part Two
(3,1),(3,2),(3,3),(3,5),
-- Oppenheimer
(4,5),
-- The Batman
(5,1),(5,7),

-- Inside Out 2
(6,8),(6,5),(6,4),
-- How to Train Your Dragon: The Hidden World
(7,8),(7,2),(7,1),
-- Kung Fu Panda 4
(8,8),(8,2),(8,4),
-- Puss in Boots: The Last Wish
(9,8),(9,2),(9,4),
-- Spirited Away
(10,8),(10,2),(10,5),
-- My Neighbor Totoro
(11,8),(11,2),(11,5),
-- Howl’s Moving Castle
(12,8),(12,2),(12,9),(12,5),
-- The Boy and the Heron
(13,8),(13,5),(13,2),

-- La La Land
(14,9),(14,5),(14,4),
-- The Notebook
(15,9),(15,5),
-- Barbie
(16,8),(16,4),(16,5),

-- A Quiet Place
(17,6),(17,7),
-- Get Out
(18,6),(18,7),(18,5),
-- Parasite
(19,5),(19,7)
ON CONFLICT DO NOTHING;


COMMIT;

-- ====== Consultas rápidas de verificación (opcional) ======
-- SELECT table_name, COUNT(*) AS filas FROM information_schema.columns
--  WHERE table_schema = 'public' AND table_name IN ('usuario','genero','pelicula','sala','asiento','funcion','compra','entrada','combo_confiteria','compra_confiteria')
--  GROUP BY table_name ORDER BY table_name;
