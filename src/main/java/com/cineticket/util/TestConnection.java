package com.cineticket.util;

import java.sql.*;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("▶ Verificando conexion a base de datos y relación N:M entre película y género...");
        String sqlConcat =
                "SELECT p.id_pelicula, p.titulo, " +
                        "       STRING_AGG(g.nombre_genero, ', ' ORDER BY g.nombre_genero) AS generos " +
                        "FROM pelicula p " +
                        "JOIN pelicula_genero pg ON p.id_pelicula = pg.pelicula_id " +
                        "JOIN genero g ON pg.genero_id = g.id_genero " +
                        "GROUP BY p.id_pelicula, p.titulo " +
                        "ORDER BY p.id_pelicula";

        String sqlSinGenero =
                "SELECT p.id_pelicula, p.titulo " +
                        "FROM pelicula p LEFT JOIN pelicula_genero pg ON p.id_pelicula = pg.pelicula_id " +
                        "GROUP BY p.id_pelicula, p.titulo HAVING COUNT(pg.genero_id) = 0";

        try (Connection con = ConnectionPool.getConnection()) {
            // 1) Imprime películas con sus géneros (prueba de JOIN)
            try (PreparedStatement ps = con.prepareStatement(sqlConcat);
                 ResultSet rs = ps.executeQuery()) {
                int filas = 0;
                while (rs.next()) {
                    System.out.printf("  #%d  %-35s  =>  %s%n",
                            rs.getInt("id_pelicula"),
                            rs.getString("titulo"),
                            rs.getString("generos"));
                    filas++;
                }
                System.out.println("✔ Filas devueltas: " + filas);
                if (filas == 0) {
                    System.out.println("⚠ No hay relaciones en pelicula_genero (¿faltó el seed?).");
                }
            }

            // 2) Detecta películas sin género (no debería haber)
            try (PreparedStatement ps = con.prepareStatement(sqlSinGenero);
                 ResultSet rs = ps.executeQuery()) {
                boolean haySinGenero = false;
                while (rs.next()) {
                    haySinGenero = true;
                    System.out.printf("❌ Película sin género: #%d - %s%n",
                            rs.getInt("id_pelicula"),
                            rs.getString("titulo"));
                }
                if (!haySinGenero) System.out.println("✔ Todas las películas tienen al menos un género.");
            }

        } catch (Exception e) {
            System.err.println("❌ Error verificando N:M: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ConnectionPool.close();
        }
    }
}
