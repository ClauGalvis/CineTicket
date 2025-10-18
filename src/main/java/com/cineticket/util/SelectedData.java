package com.cineticket.util;

import com.cineticket.modelo.Funcion;
import com.cineticket.modelo.Pelicula;

import java.util.*;

public final class SelectedData {
    private static Pelicula pelicula;
    private static Funcion funcion;
    private static List<Integer> asientosSeleccionados = new ArrayList<>();
    private static Map<Integer, Integer> combosSeleccionados = new LinkedHashMap<>();

    private SelectedData(){}

    public static Pelicula getPelicula() { return pelicula; }
    public static void setPelicula(Pelicula p) { pelicula = p; }

    public static Funcion getFuncion() { return funcion; }
    public static void setFuncion(Funcion f) { funcion = f; }

    public static List<Integer> getAsientosSeleccionados() { return asientosSeleccionados; }
    public static void setAsientosSeleccionados(List<Integer> ids) {
        asientosSeleccionados = (ids == null) ? new ArrayList<>() : new ArrayList<>(ids);
    }

    public static Map<Integer, Integer> getCombosSeleccionados() { return combosSeleccionados; }
    public static void setCombosSeleccionados(Map<Integer, Integer> combos) {
        combosSeleccionados = (combos == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(combos);
    }

    /** Limpia todo al finalizar una compra o al cancelar. */
    public static void clear() {
        pelicula = null;
        funcion = null;
        asientosSeleccionados.clear();
        combosSeleccionados.clear();
    }
}
