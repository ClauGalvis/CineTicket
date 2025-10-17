package com.cineticket.dao;

import com.cineticket.dao.impl.SalaDAOImpl;
import com.cineticket.modelo.Sala;
import com.cineticket.util.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class SalaDAOTest {

    private static final boolean KEEP_DATA = true; // true -> conserva la sala creada

    public static void main(String[] args) {
        SalaDAO dao = new SalaDAOImpl();

        String suf = String.valueOf(System.currentTimeMillis());
        Sala s = new Sala("Sala Test " + suf, 6, 7); // capacidadTotal = 42

        try {
            // CREAR
            Integer id = dao.crear(s);
            System.out.println("[CREAR] id=" + id + " cap=" + s.getCapacidadTotal());

            // BUSCAR POR ID
            Sala byId = dao.buscarPorId(id);
            System.out.println("[BUSCAR ID] " + (byId != null ? byId.getNombreSala() : "null"));

            // LISTAR TODAS
            System.out.println("[LISTAR TODAS] total=" + dao.listarTodas().size());

            // LISTAR ACTIVAS
            System.out.println("[LISTAR ACTIVAS] total=" + dao.listarActivas().size());

            // BUSCAR POR NOMBRE
            Sala byName = dao.buscarPorNombre(s.getNombreSala());
            System.out.println("[BUSCAR POR NOMBRE] " + (byName != null ? byName.getIdSala() : "null"));

            // ACTUALIZAR (cambiar filas/columnas y nombre)
            s.setFilas(8);
            s.setColumnas(8);
            s.setNombreSala("Sala Test Actualizada " + suf);
            s.setActiva(true);
            boolean upd = dao.actualizar(s);
            System.out.println("[ACTUALIZAR] ok=" + upd);

            // Verificar nueva capacidad (= 64)
            Sala after = dao.buscarPorId(id);
            System.out.println("[POST-ACT] cap=" + (after != null ? after.getCapacidadTotal() : null));

            if (!KEEP_DATA) {
                deleteSala(id);
                System.out.println("[CLEANUP] Sala eliminada.");
            } else {
                System.out.println("[KEEP_DATA] Sala conservada para inspección.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionPool.close();
            System.out.println("[FIN] SalaDAO smoke test.");
        }
    }

    private static void deleteSala(int id) throws Exception {
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM sala WHERE id_sala = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
