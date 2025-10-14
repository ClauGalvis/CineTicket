-- =========================================================
-- CineTicket - 02_views.sql
-- Vistas útiles para cartelera, ocupación, top ventas, confitería
-- =========================================================

BEGIN;

-- (Opcional)
-- SET search_path TO cineticket, public;

-- Ocupación por función
CREATE OR REPLACE VIEW v_ocupacion_funcion AS
SELECT
  f.id_funcion,
  p.titulo          AS pelicula,
  s.nombre_sala     AS sala,
  f.fecha_hora_inicio,
  f.fecha_hora_fin,
  s.capacidad_total,
  COUNT(e.id_entrada) FILTER (WHERE e.estado_entrada = 'ACTIVA') AS asientos_vendidos,
  s.capacidad_total - COUNT(e.id_entrada) FILTER (WHERE e.estado_entrada = 'ACTIVA') AS asientos_disponibles,
  ROUND( (COUNT(e.id_entrada) FILTER (WHERE e.estado_entrada='ACTIVA')::NUMERIC / NULLIF(s.capacidad_total,0)) * 100, 2) AS porcentaje_ocupacion
FROM funcion f
JOIN pelicula p ON f.pelicula_id = p.id_pelicula
JOIN sala s     ON f.sala_id     = s.id_sala
LEFT JOIN entrada e ON f.id_funcion = e.funcion_id
GROUP BY f.id_funcion, p.titulo, s.nombre_sala, s.capacidad_total, f.fecha_hora_inicio, f.fecha_hora_fin;

-- Top películas por entradas vendidas / ingresos
CREATE OR REPLACE VIEW v_top_peliculas AS
SELECT
  p.id_pelicula,
  p.titulo,
  COUNT(e.id_entrada) FILTER (WHERE e.estado_entrada='ACTIVA') AS entradas_vendidas,
  SUM(e.precio_unitario) FILTER (WHERE e.estado_entrada='ACTIVA') AS ingresos_totales
FROM pelicula p
JOIN funcion f ON p.id_pelicula = f.pelicula_id
LEFT JOIN entrada e ON f.id_funcion = e.funcion_id
GROUP BY p.id_pelicula, p.titulo
ORDER BY entradas_vendidas DESC NULLS LAST;

-- Ventas de confitería
CREATE OR REPLACE VIEW v_ventas_confiteria AS
SELECT
  cc.id_combo,
  cc.nombre_combo,
  cc.categoria,
  COALESCE(SUM(ccf.cantidad),0) AS unidades_vendidas,
  COALESCE(SUM(ccf.subtotal),0) AS ingresos_totales
FROM combo_confiteria cc
LEFT JOIN compra_confiteria ccf ON cc.id_combo = ccf.combo_id
LEFT JOIN compra c ON ccf.compra_id = c.id_compra AND c.estado_compra='CONFIRMADA'
GROUP BY cc.id_combo, cc.nombre_combo, cc.categoria
ORDER BY unidades_vendidas DESC, ingresos_totales DESC;

COMMIT;
