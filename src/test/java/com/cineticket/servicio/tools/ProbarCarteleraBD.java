package com.cineticket.servicio.tools;

import com.cineticket.dao.FuncionDAO;
import com.cineticket.dao.PeliculaDAO;
import com.cineticket.dao.GeneroDAO;
import com.cineticket.dao.impl.FuncionDAOImpl;
import com.cineticket.dao.impl.PeliculaDAOImpl;
import com.cineticket.dao.impl.GeneroDAOImpl;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Pelicula;
import com.cineticket.servicio.CarteleraService;

import java.util.List;

public class ProbarCarteleraBD {
    public static void main(String[] args) {
        // DAOs reales (usan ConnectionPool y application.properties)
        PeliculaDAO peliculaDAO = new PeliculaDAOImpl();
        FuncionDAO funcionDAO   = new FuncionDAOImpl();
        GeneroDAO generoDAO     = new GeneroDAOImpl();   // ← nuevo

        CarteleraService service = new CarteleraService(
                peliculaDAO,
                funcionDAO,
                generoDAO       // ← nuevo
        );

        // 1) Cartelera completa
        List<Pelicula> activas = service.obtenerCarteleraCompleta();
        System.out.println("Películas activas = " + activas.size());
        activas.stream().limit(10).forEach(p ->
                System.out.println(" - [" + p.getIdPelicula() + "] " + p.getTitulo())
        );

        // 2) Detalles de la película 26
        int peliculaId = 26; // <- ajusta si quieres probar otra
        Pelicula p = service.obtenerDetallesPelicula(peliculaId);
        System.out.println("\nDetalles película " + peliculaId + ": " + p.getTitulo() +
                " | Clasif=" + p.getClasificacion() + " | Dur=" + p.getDuracionMinutos());

        // 3) Funciones de esa película
        List<Funcion> funcs = service.obtenerFuncionesPorPelicula(peliculaId);
        System.out.println("Funciones de " + peliculaId + " = " + funcs.size());
        for (Funcion f : funcs) {
            System.out.println(" - Función " + f.getIdFuncion() + " | Sala " + f.getSalaId()
                    + " | " + f.getFechaHoraInicio() + " → " + f.getFechaHoraFin()
                    + " | $ " + f.getPrecioEntrada() + " | " + f.getEstado());
        }
    }
}
