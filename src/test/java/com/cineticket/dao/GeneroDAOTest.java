package com.cineticket.dao;

import com.cineticket.dao.impl.GeneroDAOImpl;
import com.cineticket.modelo.Genero;
import com.cineticket.util.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class GeneroDAOTest {

    private static final boolean KEEP_DATA = true; // true -> conserva el género creado

    public static void main(String[] args) {
        GeneroDAO dao = new GeneroDAOImpl();

        String suf = String.valueOf(System.currentTimeMillis());
        Genero g = new Genero("Genero Test " + suf, "Descripción demo");

        try {
            // CREAR
            Integer id = dao.crear(g);
            System.out.println("[CREAR] id=" + id);

            // BUSCAR POR ID
            System.out.println("[BUSCAR ID] " + dao.buscarPorId(id).getNombreGenero());

            // LISTAR TODOS
            System.out.println("[LISTAR TODOS] total=" + dao.listarTodos().size());

            // LISTAR ACTIVOS
            System.out.println("[LISTAR ACTIVOS] total=" + dao.listarActivos().size());

            // BUSCAR POR NOMBRE
            System.out.println("[BUSCAR POR NOMBRE] " + dao.buscarPorNombre(g.getNombreGenero()).getDescripcion());

            // ACTUALIZAR
            g.setDescripcion("Descripción actualizada");
            g.setActivo(true);
            System.out.println("[ACTUALIZAR] ok=" + dao.actualizar(g));

            if (!KEEP_DATA) {
                deleteGenero(id);
                System.out.println("[CLEANUP] Género eliminado.");
            } else {
                System.out.println("[KEEP_DATA] Género conservado para inspección.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionPool.close();
            System.out.println("[FIN] GeneroDAO smoke test.");
        }
    }

    private static void deleteGenero(int id) throws Exception {
        try (Connection c = ConnectionPool.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM genero WHERE id_genero = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
