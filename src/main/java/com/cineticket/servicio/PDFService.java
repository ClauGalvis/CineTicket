package com.cineticket.servicio;

import com.cineticket.modelo.Compra;
import com.cineticket.modelo.Entrada;
import com.cineticket.modelo.CompraConfiteria;
import java.util.List;
import java.util.Map;

public interface PDFService {
    /**
     * Genera el comprobante PDF y devuelve la ruta absoluta del archivo creado.
     * El mapa datosAdicionales es opcional (por ej.: "peliculaNombre", "funcionTexto", "clienteNombre").
     */
    String generarComprobantePDF(Compra compra,
                                 List<Entrada> entradas,
                                 List<CompraConfiteria> combos,
                                 Map<String, Object> datosAdicionales);

    /**
     * “mover/guardar” el PDF a un destino específico (descargas, etc.).
     * Devuelve true si copió correctamente.
     */
    boolean guardarComprobante(Compra compra, String rutaDestino);
}
