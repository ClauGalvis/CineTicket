package com.cineticket.servicio.dto;

import com.cineticket.modelo.Compra;
import com.cineticket.modelo.CompraConfiteria;
import com.cineticket.modelo.Entrada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompraPreparada {

    private final Compra compra;
    private final List<Entrada> entradas;
    private final List<CompraConfiteria> itemsConfiteria;

    public CompraPreparada(Compra compra,
                           List<Entrada> entradas,
                           List<CompraConfiteria> itemsConfiteria) {

        this.compra = compra;

        // listas inmutables seguras (nunca null)
        this.entradas = entradas != null
                ? Collections.unmodifiableList(new ArrayList<>(entradas))
                : Collections.emptyList();

        this.itemsConfiteria = itemsConfiteria != null
                ? Collections.unmodifiableList(new ArrayList<>(itemsConfiteria))
                : Collections.emptyList();
    }

    public Compra getCompra() { return compra; }

    public List<Entrada> getEntradas() { return entradas; }

    public List<CompraConfiteria> getItemsConfiteria() { return itemsConfiteria; }
}
