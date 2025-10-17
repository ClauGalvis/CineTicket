package com.cineticket.dao;

import com.cineticket.modelo.Genero;

import java.util.List;

public interface GeneroDAO {

    Integer crear(Genero genero);

    Genero buscarPorId(Integer id);

    List<Genero> listarTodos();

    List<Genero> listarActivos();

    boolean actualizar(Genero genero);

    Genero buscarPorNombre(String nombre); // búsqueda exacta por nombre_genero
}
