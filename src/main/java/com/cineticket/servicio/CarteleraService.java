package com.cineticket.servicio;

import com.cineticket.dao.FuncionDAO;
import com.cineticket.dao.PeliculaDAO;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Pelicula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class CarteleraService {

    private static final Logger log = LoggerFactory.getLogger(CarteleraService.class);

    private final PeliculaDAO peliculaDAO;
    private final FuncionDAO funcionDAO;

    public CarteleraService(PeliculaDAO peliculaDAO, FuncionDAO funcionDAO) {
        this.peliculaDAO = Objects.requireNonNull(peliculaDAO);
        this.funcionDAO  = Objects.requireNonNull(funcionDAO);
    }

    /** Retorna todas las películas activas (para la vista de cartelera). */
    public List<Pelicula> obtenerCarteleraCompleta() {
        List<Pelicula> activas = peliculaDAO.listarActivas();
        log.debug("Cartelera: {} películas activas", activas.size());
        return activas;
    }

    /** Lista funciones disponibles para una película (para ver horarios/precio). */
    public List<Funcion> obtenerFuncionesPorPelicula(Integer peliculaId) {
        if (peliculaId == null) throw new ValidacionException("peliculaId es requerido.");
        List<Funcion> funciones = funcionDAO.listarPorPelicula(peliculaId);
        log.debug("Funciones de película {}: {}", peliculaId, funciones.size());
        return funciones;
    }

    /** Retorna los detalles básicos de la película seleccionada. */
    public Pelicula obtenerDetallesPelicula(Integer peliculaId) {
        if (peliculaId == null) throw new ValidacionException("peliculaId es requerido.");
        Pelicula p = peliculaDAO.buscarPorId(peliculaId);
        if (p == null) throw new ValidacionException("Película no encontrada.");
        return p;
    }
}
