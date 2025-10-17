package com.cineticket.dao;

import com.cineticket.modelo.Sala;

import java.util.List;

public interface SalaDAO {

    Integer crear(Sala sala);

    Sala buscarPorId(Integer id);

    List<Sala> listarTodas();

    List<Sala> listarActivas();

    boolean actualizar(Sala sala);

    Sala buscarPorNombre(String nombre); // búsqueda exacta por nombre_sala
}
