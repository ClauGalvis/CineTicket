package com.cineticket.servicio.tools;

import com.cineticket.dao.impl.EntradaDAOImpl;
import com.cineticket.dao.impl.FuncionDAOImpl;
import com.cineticket.servicio.ReservaService;

import java.util.List;

public class ProbarReservaBD {
    public static void main(String[] args) {
        var entradaDAO = new EntradaDAOImpl();
        var funcionDAO = new FuncionDAOImpl();
        var service = new ReservaService(entradaDAO, funcionDAO);

        int funcionId = 2; // PROGRAMADA (de tu captura)
        System.out.println("Ocupados: " + service.obtenerAsientosOcupadosPorFuncion(funcionId));

        boolean libres = service.verificarDisponibilidadAsientos(funcionId, List.of(7, 8));
        System.out.println("Asientos 7 y 8 libres? " + libres);

        var preEntradas = service.reservarAsientos(funcionId, List.of(7, 8));
        preEntradas.forEach(e ->
                System.out.println("Pre-Entrada -> funcion=" + e.getFuncionId() +
                        ", asiento=" + e.getAsientoId() +
                        ", precio=" + e.getPrecioUnitario())
        );
    }
}
