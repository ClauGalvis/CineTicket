package com.cineticket.dao;

import com.cineticket.modelo.ComboConfiteria;

import java.util.List;

public interface ComboConfiteriaDAO {

    Integer crear(ComboConfiteria combo);

    ComboConfiteria buscarPorId(Integer id);

    List<ComboConfiteria> listarTodos();

    List<ComboConfiteria> listarDisponibles();

    boolean actualizar(ComboConfiteria combo);

    /** Hard delete (si prefieres soft, lo cambiamos por disponible=false) */
    boolean eliminar(Integer id);
}
