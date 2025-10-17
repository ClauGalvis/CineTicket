package com.cineticket.dao;

import com.cineticket.modelo.CompraConfiteria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CompraConfiteriaDAO {

    Integer crear(CompraConfiteria item);

    CompraConfiteria buscarPorId(Integer id);

    List<CompraConfiteria> listarPorCompra(Integer compraId);

    /** Reporte: comboId -> cantidad total vendida en el rango [inicio, fin] (por fecha de la compra) */
    Map<Integer, Integer> obtenerVentasPorCombo(LocalDateTime inicio, LocalDateTime fin);
}
