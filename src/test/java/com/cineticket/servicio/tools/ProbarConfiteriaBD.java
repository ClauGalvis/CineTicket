package com.cineticket.servicio.tools;

import com.cineticket.dao.ComboConfiteriaDAO;
import com.cineticket.dao.impl.ComboConfiteriaDAOImpl; // <-- ajusta el nombre del impl si difiere
import com.cineticket.modelo.ComboConfiteria;
import com.cineticket.servicio.ConfiteriaService;

import java.math.BigDecimal;
import java.util.List;

public class ProbarConfiteriaBD {
    public static void main(String[] args) {
        ComboConfiteriaDAO comboDAO = new ComboConfiteriaDAOImpl();
        ConfiteriaService service = new ConfiteriaService(comboDAO);

        // 1) Listado de combos disponibles
        List<ComboConfiteria> disponibles = service.obtenerCombosDisponibles();
        System.out.println("Combos disponibles = " + disponibles.size());
        disponibles.forEach(c ->
                System.out.println(" - [" + c.getIdCombo() + "] " + c.getNombreCombo()
                        + " | $" + c.getPrecio() + " | cat=" + c.getCategoria())
        );

        // 2) Tomamos un combo conocido para probar subtotal (ej: id=10 "Combo Grande", precio 25000)
        int comboId = 10; // ajusta si quieres probar otro
        ComboConfiteria combo = service.obtenerCombo(comboId);
        System.out.println("\nCombo elegido: [" + combo.getIdCombo() + "] "
                + combo.getNombreCombo() + " | $" + combo.getPrecio());

        var cantidad = 3;
        BigDecimal subtotal = service.calcularSubtotal(comboId, cantidad);
        System.out.println("Cantidad " + cantidad + " -> Subtotal = $" + subtotal);
    }
}
