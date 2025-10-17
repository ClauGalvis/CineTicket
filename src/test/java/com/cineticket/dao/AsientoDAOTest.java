package com.cineticket.dao;

import com.cineticket.dao.impl.AsientoDAOImpl;
import com.cineticket.enums.TipoAsiento;
import com.cineticket.modelo.Asiento;
import com.cineticket.util.ConnectionPool;

import java.sql.*;

import static java.lang.System.out;

public class AsientoDAOTest {

    private static final boolean KEEP_DATA = true; // true -> conserva asientos (y sala) para inspección

    public static void main(String[] args) {
        AsientoDAO dao = new AsientoDAOImpl();

        String suf = String.valueOf(System.currentTimeMillis());
        Integer salaId = null;

        try {
            // 0) Asegurar una sala para probar
            salaId = ensureSala("Sala Asientos " + suf, 5, 5);
            out.println("[SALA] id=" + salaId);

            // 1) CREAR asiento
            Asiento a = new Asiento(salaId, "A", 1, TipoAsiento.REGULAR);
            Integer id = dao.crear(a);
            out.println("[CREAR] asiento id=" + id + " ident=" + a.getIdentificador());

            // 2) BUSCAR por ID
            Asiento byId = dao.buscarPorId(id);
            out.println("[BUSCAR ID] fila=" + byId.getFila() + " num=" + byId.getNumero() + " tipo=" + byId.getTipoAsiento());

            // 3) LISTAR por sala
            out.println("[LISTAR POR SALA] total=" + dao.listarPorSala(salaId).size());

            // 4) BUSCAR por (sala, fila, numero)
            Asiento coords = dao.buscarPorSalaFilaNumero(salaId, "A", 1);
            out.println("[BUSCAR POR COORD] id=" + (coords != null ? coords.getIdAsiento() : null));

            // 5) ACTUALIZAR (cambiar a VIP y número)
            a.setTipoAsiento(TipoAsiento.VIP);
            a.setNumero(2);
            boolean upd = dao.actualizar(a);
            out.println("[ACTUALIZAR] ok=" + upd);

            Asiento after = dao.buscarPorId(id);
            out.println("[POST-ACT] num=" + after.getNumero() + " tipo=" + after.getTipoAsiento());

            // 6) Probar UNIQUE (opcional): descomenta para ver DaoException 23505
            // dao.crear(new Asiento(salaId, "A", 2, TipoAsiento.REGULAR));

            if (!KEEP_DATA) {
                deleteAsientosBySala(salaId);
                deleteSala(salaId);
                out.println("[CLEANUP] Asientos + sala eliminados.");
            } else {
                out.println("[KEEP_DATA] Datos conservados para inspección.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionPool.close();
            out.println("[FIN] AsientoDAO smoke test.");
        }
    }

    // ===== Helpers de fixture/cleanup =====

    private static Integer ensureSala(String nombre, int filas, int columnas) throws SQLException {
        String select = "SELECT id_sala FROM sala WHERE nombre_sala = ?";
        String insert = "INSERT INTO sala (nombre_sala, capacidad_total, filas, columnas, activa) VALUES (?, ?, ?, ?, TRUE) RETURNING id_sala";
        try (Connection c = ConnectionPool.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(select)) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(insert)) {
                ps.setString(1, nombre);
                ps.setInt(2, filas * columnas);
                ps.setInt(3, filas);
                ps.setInt(4, columnas);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    private static void deleteAsientosBySala(int salaId) throws SQLException {
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM asiento WHERE sala_id = ?")) {
            ps.setInt(1, salaId);
            ps.executeUpdate();
        }
    }

    private static void deleteSala(int salaId) throws SQLException {
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM sala WHERE id_sala = ?")) {
            ps.setInt(1, salaId);
            ps.executeUpdate();
        }
    }
}
