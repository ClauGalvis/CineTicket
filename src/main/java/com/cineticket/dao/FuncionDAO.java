package com.cineticket.dao;

import com.cineticket.modelo.Funcion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface FuncionDAO {

    Integer crear(Funcion funcion);

    Funcion buscarPorId(Integer id);

    List<Funcion> listarPorPelicula(Integer peliculaId);

    List<Funcion> listarPorSala(Integer salaId);

    List<Funcion> listarPorFecha(LocalDate fecha);

    boolean actualizar(Funcion funcion);

    /** Soft delete: marca estado = CANCELADA */
    boolean eliminar(Integer id);

    /** true si la sala está libre en [inicio, fin) (excluyendo una función opcional) */
    boolean verificarDisponibilidadSala(Integer salaId,
                                        LocalDateTime inicio,
                                        LocalDateTime fin,
                                        Integer funcionIdExcluir);
}
