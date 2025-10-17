package com.cineticket.dao;

import com.cineticket.dao.impl.PeliculaDAOImpl;
import com.cineticket.enums.Clasificacion;
import com.cineticket.modelo.Pelicula;
import com.cineticket.util.ConnectionPool;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class PeliculaDAOTest {

    // 🔁 Interruptor de limpieza
    // Si es true → conserva los datos creados en la BD.
    // Si es false → limpia relaciones (pelicula_genero) al final.
    private static final boolean KEEP_DATA = true;

    public static void main(String[] args) {
        PeliculaDAO dao = new PeliculaDAOImpl();

        String suf = String.valueOf(System.currentTimeMillis());
        Pelicula p = new Pelicula(
                "Prueba DAO " + suf,
                123,
                Clasificacion.DOCE_MAS,
                "Sinopsis inicial",
                "https://img/prueba.png",
                LocalDate.now().minusDays(5)
        );

        try {
            // 1) CREAR
            Integer id = dao.crear(p);
            System.out.println("[CREAR] id=" + id);

            // 2) BUSCAR POR ID
            Pelicula byId = dao.buscarPorId(id);
            System.out.println("[BUSCAR ID] titulo=" + (byId != null ? byId.getTitulo() : "null"));

            // 3) LISTAR TODAS
            System.out.println("[LISTAR TODAS] total=" + dao.listarTodas().size());

            // 4) LISTAR ACTIVAS
            System.out.println("[LISTAR ACTIVAS] total=" + dao.listarActivas().size());

            // 5) BUSCAR POR TÍTULO (LIKE)
            System.out.println("[BUSCAR LIKE] total=" + dao.buscarPorTitulo("Prueba DAO").size());

            // 6) ACTUALIZAR
            p.setSinopsis("Sinopsis actualizada");
            p.setActiva(true);
            boolean upd = dao.actualizar(p);
            System.out.println("[ACTUALIZAR] ok=" + upd);

            // 7) PREPARAR GÉNEROS
            int genAccion = ensureGenero("Acción", "Películas de alta adrenalina");
            int genSciFi  = ensureGenero("Ciencia Ficción", "Futuros, tecnología, espacio");

            // 8) ASIGNAR GÉNEROS
            boolean asignados = dao.asignarGeneros(id, List.of(genAccion, genSciFi));
            System.out.println("[ASIGNAR GÉNEROS] ok=" + asignados);

            // 9) OBTENER GÉNEROS
            var generos = dao.obtenerGenerosDePelicula(id);
            System.out.println("[OBTENER GÉNEROS] total=" + generos.size());
            generos.forEach(g -> System.out.println(" - " + g.getIdGenero() + " | " + g.getNombreGenero()));

            // 10) ELIMINAR (lógico)
            boolean del = dao.eliminar(id);
            System.out.println("[ELIMINAR LOGICO] ok=" + del);

            // 11) POST-ELIMINAR
            Pelicula after = dao.buscarPorId(id);
            System.out.println("[POST-ELIMINAR] activa=" + (after != null && after.isActiva()));

            // 12) DEBUG opcional: contar registros en la tabla puente
            System.out.println("[DEBUG] Relaciones N:M = " + countJoinRows(id));

            // 🔁 Limpieza condicional
            if (!KEEP_DATA) {
                cleanupJoinForPelicula(id);
                System.out.println("[CLEANUP] Relaciones eliminadas (modo limpio).");
            } else {
                System.out.println("[KEEP_DATA] Relaciones mantenidas (puedes verlas en SQL).");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            ConnectionPool.close();
            System.out.println("[FIN] PeliculaDAO full smoke test.");
        }
    }

    // === Helpers ===

    private static int ensureGenero(String nombre, String descripcion) throws SQLException {
        String select = "SELECT id_genero FROM genero WHERE nombre_genero = ?";
        String insert = "INSERT INTO genero (nombre_genero, descripcion, activo) VALUES (?, ?, TRUE) RETURNING id_genero";
        try (Connection c = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(select)) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(insert)) {
                ps.setString(1, nombre);
                ps.setString(2, descripcion);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    private static void cleanupJoinForPelicula(int peliculaId) throws SQLException {
        String sql = "DELETE FROM pelicula_genero WHERE pelicula_id = ?";
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, peliculaId);
            ps.executeUpdate();
        }
    }

    private static int countJoinRows(int peliculaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pelicula_genero WHERE pelicula_id = ?";
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, peliculaId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
