// com/cineticket/util/SeleccionPeliculaContext.java
package com.cineticket.util;

public final class SeleccionPeliculaContext {

    private static Integer peliculaActualId;

    private SeleccionPeliculaContext() { }

    public static void setPeliculaActualId(Integer id) {
        peliculaActualId = id;
    }

    public static Integer getPeliculaActualId() {
        return peliculaActualId;
    }
}
