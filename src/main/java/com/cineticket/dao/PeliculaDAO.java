package com.cineticket.dao;

import com.cineticket.modelo.Genero;
import com.cineticket.modelo.Pelicula;

import java.util.List;

public interface PeliculaDAO {

    // CRUD
    Integer crear(Pelicula pelicula);                 // retorna id generado
    Pelicula buscarPorId(Integer id);                 // null si no existe (según tu doc)
    List<Pelicula> listarTodas();                     // ORDER BY titulo
    List<Pelicula> listarActivas();                   // activa=TRUE ORDER BY fecha_estreno DESC
    boolean actualizar(Pelicula pelicula);
    boolean eliminar(Integer id);                     // soft-delete (activa=false) o DELETE, ver impl

    // Búsquedas
    List<Pelicula> buscarPorTitulo(String titulo);    // ILIKE %titulo%

    // Relación N:M con géneros
    boolean asignarGeneros(Integer peliculaId, List<Integer> generoIds);
    List<Genero> obtenerGenerosDePelicula(Integer peliculaId);
}
