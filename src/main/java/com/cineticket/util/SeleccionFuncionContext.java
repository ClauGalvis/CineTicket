package com.cineticket.util;

public final class SeleccionFuncionContext {

    private static Integer funcionActualId;

    private SeleccionFuncionContext() {
        // util class
    }

    public static Integer getFuncionActualId() {
        return funcionActualId;
    }

    public static void setFuncionActualId(Integer id) {
        funcionActualId = id;
    }

    public static void limpiar() {
        funcionActualId = null;
    }
}
