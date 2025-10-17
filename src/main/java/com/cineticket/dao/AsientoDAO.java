package com.cineticket.dao;

import com.cineticket.modelo.Asiento;

import java.util.List;

public interface AsientoDAO {

    Integer crear(Asiento asiento);

    Asiento buscarPorId(Integer id);

    List<Asiento> listarPorSala(Integer salaId); // ORDER BY fila, numero

    boolean actualizar(Asiento asiento);

    Asiento buscarPorSalaFilaNumero(Integer salaId, String fila, Integer numero);
}
