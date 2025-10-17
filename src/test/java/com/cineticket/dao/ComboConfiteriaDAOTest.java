package com.cineticket.dao;

import com.cineticket.dao.impl.ComboConfiteriaDAOImpl;
import com.cineticket.modelo.ComboConfiteria;
import com.cineticket.util.ConnectionPool;

import java.math.BigDecimal;
import java.util.List;

public class ComboConfiteriaDAOTest {

    private static final boolean KEEP_DATA = true; // true -> conserva el combo para inspección

    public static void main(String[] args) {
        ComboConfiteriaDAO dao = new ComboConfiteriaDAOImpl();

        try {
            String suf = String.valueOf(System.currentTimeMillis());
            ComboConfiteria combo = new ComboConfiteria(
                    "Combo Demo " + suf,
                    "Palomitas + Gaseosa",
                    new BigDecimal("18000.00"),
                    "https://img/combodemo.png",
                    true,
                    "Combos"
            );

            // 1) CREAR
            Integer id = dao.crear(combo);
            System.out.println("[CREAR] id=" + id);

            // 2) BUSCAR ID
            ComboConfiteria byId = dao.buscarPorId(id);
            System.out.println("[BUSCAR ID] nombre=" + (byId != null ? byId.getNombreCombo() : null));

            // 3) LISTAR TODOS / DISPONIBLES
            List<ComboConfiteria> todos = dao.listarTodos();
            List<ComboConfiteria> disp  = dao.listarDisponibles();
            System.out.println("[LISTAR TODOS] total=" + todos.size());
            System.out.println("[LISTAR DISPONIBLES] total=" + disp.size());

            // 4) ACTUALIZAR (cambiar precio y disponibilidad)
            combo.setPrecio(new BigDecimal("20000.00"));
            combo.setDisponible(false);
            boolean upd = dao.actualizar(combo);
            System.out.println("[ACTUALIZAR] ok=" + upd);

            // 5) ELIMINAR (según flag)
            if (!KEEP_DATA) {
                boolean del = dao.eliminar(id);
                System.out.println("[ELIMINAR] ok=" + del);
            } else {
                System.out.println("[KEEP_DATA] Combo conservado para inspección.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ConnectionPool.close();
            System.out.println("[FIN] ComboConfiteriaDAO smoke test.");
        }
    }
}
