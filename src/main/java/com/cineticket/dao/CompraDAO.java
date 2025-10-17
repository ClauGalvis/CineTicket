package com.cineticket.dao;

import com.cineticket.modelo.Compra;

import java.time.LocalDateTime;
import java.util.List;

public interface CompraDAO {

    Integer crear(Compra compra);

    Compra buscarPorId(Integer id);

    List<Compra> listarPorUsuario(Integer usuarioId);

    boolean actualizar(Compra compra);

    /** Marca la compra como CANCELADA y setea fecha_cancelacion = now() */
    boolean cancelarCompra(Integer idCompra);

    /** Compras cuyo timestamp está entre [inicio, fin] (para reportes) */
    List<Compra> obtenerComprasEntreFechas(LocalDateTime inicio, LocalDateTime fin);
}
