package com.cineticket.servicio;

import com.cineticket.dao.FuncionDAO;
import com.cineticket.dao.PeliculaDAO;
import com.cineticket.dao.GeneroDAO;
import com.cineticket.excepcion.AutenticacionException;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.util.SessionManager;
import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Pelicula;
import com.cineticket.modelo.Genero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CarteleraService {

    private static final Logger log = LoggerFactory.getLogger(CarteleraService.class);

    private final PeliculaDAO peliculaDAO;
    private final FuncionDAO funcionDAO;
    private final GeneroDAO generoDAO;

    public CarteleraService(PeliculaDAO peliculaDAO,
                            FuncionDAO funcionDAO,
                            GeneroDAO generoDAO) {   // ← nuevo parámetro
        this.peliculaDAO = Objects.requireNonNull(peliculaDAO);
        this.funcionDAO  = Objects.requireNonNull(funcionDAO);
        this.generoDAO   = Objects.requireNonNull(generoDAO); // ← nuevo
    }

    /** Lanza excepción si el usuario actual no es ADMIN. */
    private void requireAdmin() {
        if (!SessionManager.getInstance().esAdministrador()) {
            throw new AutenticacionException("Solo un administrador puede realizar esta acción.");
        }
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

    /** Obtiene una función por su id (para selección de asientos). */
    public Funcion obtenerFuncionPorId(Integer funcionId) {
        if (funcionId == null) {
            throw new ValidacionException("funcionId es requerido.");
        }
        Funcion f = funcionDAO.buscarPorId(funcionId);
        if (f == null) {
            throw new ValidacionException("Función no encontrada.");
        }
        log.debug("Función obtenida por id {}.", funcionId);
        return f;
    }


    /** Lista funciones de un día específico. */
    public List<Funcion> obtenerFuncionesPorFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new ValidacionException("La fecha es requerida.");
        }
        return funcionDAO.listarPorFecha(fecha);
    }

    /** Búsqueda de películas por título (para filtros). */
    public List<Pelicula> buscarPeliculasPorTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new ValidacionException("El título de búsqueda es requerido.");
        }
        return peliculaDAO.buscarPorTitulo(titulo);
    }

    /** Obtiene los géneros asociados a una película. */
    public List<Genero> obtenerGenerosDePelicula(Integer peliculaId) {
        if (peliculaId == null) {
            throw new ValidacionException("peliculaId es requerido.");
        }
        return peliculaDAO.obtenerGenerosDePelicula(peliculaId);
    }

    /** Lista todos los géneros activos (útil para formularios de admin). */
    public List<Genero> obtenerGenerosActivos() {
        return generoDAO.listarActivos();
    }

    /** Lista todos los géneros, activos e inactivos (si lo llegas a necesitar). */
    public List<Genero> obtenerTodosLosGeneros() {
        return generoDAO.listarTodos();
    }


    /** Crea una película y opcionalmente le asigna géneros. Solo ADMIN. */
    public Integer crearPelicula(Pelicula pelicula, List<Integer> generoIds) {
        requireAdmin();
        validarPelicula(pelicula);

        Integer idGenerado = peliculaDAO.crear(pelicula);
        log.info("Película creada con id {}: {}", idGenerado, pelicula.getTitulo());

        if (generoIds != null && !generoIds.isEmpty()) {
            boolean ok = peliculaDAO.asignarGeneros(idGenerado, generoIds);
            log.debug("Asignación de géneros a película {}: {}", idGenerado, ok);
        }
        return idGenerado;
    }

    /** Actualiza la información básica de una película. Solo ADMIN. */
    public boolean actualizarPelicula(Pelicula pelicula) {
        requireAdmin();
        validarPelicula(pelicula);

        boolean actualizado = peliculaDAO.actualizar(pelicula);
        log.info("Película {} actualizada: {}", pelicula.getIdPelicula(), actualizado);
        return actualizado;
    }

    /** Elimina (o desactiva) una película. Solo ADMIN. */
    public boolean eliminarPelicula(Integer peliculaId) {
        requireAdmin();
        if (peliculaId == null) {
            throw new ValidacionException("peliculaId es requerido.");
        }

        boolean eliminado = peliculaDAO.eliminar(peliculaId);
        log.info("Película {} eliminada/desactivada: {}", peliculaId, eliminado);
        return eliminado;
    }

    /** Crea una función para una película en una sala. Solo ADMIN. */
    public Integer crearFuncion(Funcion funcion) {
        requireAdmin();
        validarFuncion(funcion);
        validarHorarioFuncion(funcion);

        Integer idGenerado = funcionDAO.crear(funcion);
        log.info("Función creada con id {} (película {}, sala {})",
                idGenerado, funcion.getPeliculaId(), funcion.getSalaId());
        return idGenerado;
    }

    /** Actualiza una función existente. Solo ADMIN. */
    public boolean actualizarFuncion(Funcion funcion) {
        requireAdmin();
        validarFuncion(funcion);
        validarHorarioFuncion(funcion);

        boolean actualizado = funcionDAO.actualizar(funcion);
        log.info("Función {} actualizada: {}", funcion.getIdFuncion(), actualizado);
        return actualizado;
    }

    /** Elimina (soft delete: estado CANCELADA) una función. Solo ADMIN. */
    public boolean eliminarFuncion(Integer funcionId) {
        requireAdmin();
        if (funcionId == null) {
            throw new ValidacionException("funcionId es requerido.");
        }

        boolean eliminado = funcionDAO.eliminar(funcionId);
        log.info("Función {} eliminada/cancelada: {}", funcionId, eliminado);
        return eliminado;
    }

    private void validarPelicula(Pelicula p) {
        if (p == null) {
            throw new ValidacionException("La película es requerida.");
        }
        if (p.getTitulo() == null || p.getTitulo().isBlank()) {
            throw new ValidacionException("El título de la película es obligatorio.");
        }
        if (p.getDuracionMinutos() == null || p.getDuracionMinutos() <= 0) {
            throw new ValidacionException("La duración de la película debe ser mayor a 0.");
        }
        if (p.getClasificacion() == null) {
            throw new ValidacionException("La clasificación de la película es obligatoria.");
        }
    }

    private void validarFuncion(Funcion f) {
        if (f == null) {
            throw new ValidacionException("La función es requerida.");
        }
        if (f.getPeliculaId() == null) {
            throw new ValidacionException("peliculaId es requerido.");
        }
        if (f.getSalaId() == null) {
            throw new ValidacionException("salaId es requerido.");
        }
        if (f.getFechaHoraInicio() == null) {
            throw new ValidacionException("La fecha/hora de inicio es requerida.");
        }
        if (f.getFechaHoraFin() == null) {
            throw new ValidacionException("La fecha/hora de fin es requerida.");
        }
        BigDecimal precio = BigDecimal.valueOf(f.getPrecioEntrada());
        if (precio == null || precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El precio de la entrada debe ser mayor o igual a 0.");
        }
    }

    private void validarHorarioFuncion(Funcion f) {
        LocalDateTime inicio = f.getFechaHoraInicio();
        LocalDateTime fin = f.getFechaHoraFin();

        if (!fin.isAfter(inicio)) {
            throw new ValidacionException("La función debe terminar después de la hora de inicio.");
        }

        boolean disponible = funcionDAO.verificarDisponibilidadSala(
                f.getSalaId(),
                inicio,
                fin,
                f.getIdFuncion() // null en creación, id al editar
        );

        if (!disponible) {
            throw new ValidacionException("La sala ya tiene una función en ese horario.");
        }
    }




}
