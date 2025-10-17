package com.cineticket.dao;

import com.cineticket.modelo.Entrada;

import java.util.List;

public interface EntradaDAO {

    Integer crear(Entrada entrada);

    Entrada buscarPorId(Integer id);

    List<Entrada> listarPorCompra(Integer compraId);

    List<Entrada> listarPorFuncion(Integer funcionId);

    boolean actualizar(Entrada entrada);

    /** Marca todas las entradas de una compra como CANCELADA */
    boolean cancelarEntradasDeCompra(Integer compraId);

    /** Cantidad de entradas ACTIVAS de una función */
    int contarEntradasActivasPorFuncion(Integer funcionId);

    /** true si NO existe una entrada ACTIVA para ese (funcion, asiento) */
    boolean verificarAsientoDisponible(Integer funcionId, Integer asientoId);
}
