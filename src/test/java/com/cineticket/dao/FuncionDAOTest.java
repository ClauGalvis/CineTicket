package com.cineticket.dao;

import com.cineticket.dao.impl.FuncionDAOImpl;
import com.cineticket.enums.EstadoFuncion;
import com.cineticket.modelo.Funcion;
import com.cineticket.util.ConnectionPool;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class FuncionDAOTest {

    private static final boolean KEEP_DATA = true; // true -> conserva función para inspección

    public static void main(String[] args) {
        FuncionDAO dao = new FuncionDAOImpl();

        try {
            int peliculaId = ensurePelicula("Peli FuncionDAO Test");
            int salaId     = ensureSala("Sala FuncionDAO Test", 6, 6);

            LocalDateTime inicio = LocalDateTime.now().plusHours(3).withSecond(0).withNano(0);
            LocalDateTime fin    = inicio.plusHours(2);

            // Disponibilidad previa
            boolean libre = dao.verificarDisponibilidadSala(salaId, inicio, fin, null);
            System.out.println("[DISPONIBILIDAD PREVIA] libre=" + libre);

            // CREAR
            Funcion f = new Funcion(peliculaId, salaId, inicio, fin, 18000.0, EstadoFuncion.PROGRAMADA);
            Integer id = dao.crear(f);
            System.out.println("[CREAR] id=" + id);

            // BUSCAR ID
            Funcion byId = dao.buscarPorId(id);
            System.out.println("[BUSCAR ID] estado=" + byId.getEstado());

            // LISTAR POR PELÍCULA
            List<Funcion> porPeli = dao.listarPorPelicula(peliculaId);
            System.out.println("[LISTAR POR PELI] total=" + porPeli.size());

            // LISTAR POR SALA
            List<Funcion> porSala = dao.listarPorSala(salaId);
            System.out.println("[LISTAR POR SALA] total=" + porSala.size());

            // LISTAR POR FECHA
            List<Funcion> porFecha = dao.listarPorFecha(LocalDate.from(inicio));
            System.out.println("[LISTAR POR FECHA] total=" + porFecha.size());

            // DISPONIBILIDAD (debe ser false porque ya existe una función en esa franja)
            boolean libre2 = dao.verificarDisponibilidadSala(salaId, inicio.plusMinutes(30), fin.minusMinutes(30), id /*excluirse*/);
            System.out.println("[DISPONIBILIDAD EXCLUYENDO MISMA] libre=" + libre2);
            boolean libreOverlap = dao.verificarDisponibilidadSala(salaId, inicio.plusMinutes(30), fin.minusMinutes(30), null);
            System.out.println("[DISPONIBILIDAD OVERLAP] libre=" + libreOverlap);

            // ACTUALIZAR (mover 30 min)
            f.setFechaHoraInicio(inicio.plusMinutes(30));
            f.setFechaHoraFin(fin.plusMinutes(30));
            f.setPrecioEntrada(20000.0);
            boolean upd = dao.actualizar(f);
            System.out.println("[ACTUALIZAR] ok=" + upd);

            // ELIMINAR (soft -> CANCELADA)
            boolean del = dao.eliminar(id);
            System.out.println("[CANCELAR] ok=" + del);

            if (!KEEP_DATA) {
                hardDeleteFuncion(id);
                System.out.println("[CLEANUP] Borrado físico de la función.");
            } else {
                System.out.println("[KEEP_DATA] Función queda CANCELADA para inspección.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionPool.close();
            System.out.println("[FIN] FuncionDAO smoke test.");
        }
    }

    // ===== Helpers (fixture minimal) =====

    private static int ensurePelicula(String titulo) throws SQLException {
        String sel = "SELECT id_pelicula FROM pelicula WHERE titulo = ?";
        String ins = """
            INSERT INTO pelicula (titulo, duracion_minutos, clasificacion, sinopsis, imagen_url, fecha_estreno, activa)
            VALUES (?, 120, '12+'::clasificacion, 'Demo', NULL, CURRENT_DATE, TRUE)
            RETURNING id_pelicula
        """;
        try (Connection c = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sel)) {
                ps.setString(1, titulo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setString(1, titulo);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    private static int ensureSala(String nombre, int filas, int cols) throws SQLException {
        String sel = "SELECT id_sala FROM sala WHERE nombre_sala = ?";
        String ins = "INSERT INTO sala (nombre_sala, capacidad_total, filas, columnas, activa) VALUES (?, ?, ?, ?, TRUE) RETURNING id_sala";
        try (Connection c = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sel)) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setString(1, nombre);
                ps.setInt(2, filas * cols);
                ps.setInt(3, filas);
                ps.setInt(4, cols);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    private static void hardDeleteFuncion(int id) throws SQLException {
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM funcion WHERE id_funcion = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
