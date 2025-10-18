package com.cineticket.servicio;

import com.cineticket.dao.UsuarioDAO;
import com.cineticket.enums.Rol;
import com.cineticket.excepcion.AutenticacionException;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.Usuario;
import com.cineticket.util.PasswordUtil;
import com.cineticket.util.SessionManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioDAO usuarioDAO;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void resetSession() {
        // Asegura que ninguna sesión previa contamine las pruebas
        SessionManager.getInstance().cerrarSesion();
    }

    @Test
    void registrarUsuario_ok() {
        // Arrange
        String passPlano = "Abcdef12";
        when(usuarioDAO.existeCorreo("clau@uni.com")).thenReturn(false);
        when(usuarioDAO.existeNombreUsuario("Clau")).thenReturn(false);

        // Simula que el DAO asigna el ID por generated keys
        doAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setIdUsuario(123);
            return null;
        }).when(usuarioDAO).crear(any(Usuario.class));

        // Act
        Usuario creado = authService.registrarUsuario(
                "Claudia Galvis", "clau@uni.com", "Clau", passPlano);

        // Assert
        assertNotNull(creado);
        assertEquals(123, creado.getIdUsuario());
        assertEquals("Clau", creado.getNombreUsuario());
        assertEquals("clau@uni.com", creado.getCorreoElectronico());
        assertEquals(Rol.USUARIO, creado.getRol());
        assertNotEquals(passPlano, creado.getContrasenaHash()); // debe estar hasheada
        assertTrue(creado.isActivo());

        // Verifica que se llamó a crear con un Usuario válido
        ArgumentCaptor<Usuario> cap = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioDAO).crear(cap.capture());
        assertTrue(cap.getValue().getContrasenaHash().startsWith("$2"));
    }

    @Test
    void registrarUsuario_correoDuplicado_lanzaValidacion() {
        when(usuarioDAO.existeCorreo("dup@x.com")).thenReturn(true);

        assertThrows(ValidacionException.class, () ->
                authService.registrarUsuario("User", "dup@x.com", "user123", "Abcdef12"));
        verify(usuarioDAO, never()).crear(any());
    }

    @Test
    void registrarUsuario_usuarioDuplicado_lanzaValidacion() {
        when(usuarioDAO.existeCorreo("ok@x.com")).thenReturn(false);
        when(usuarioDAO.existeNombreUsuario("repetido")).thenReturn(true);

        assertThrows(ValidacionException.class, () ->
                authService.registrarUsuario("User", "ok@x.com", "repetido", "Abcdef12"));
        verify(usuarioDAO, never()).crear(any());
    }

    @Test
    void iniciarSesion_ok() {
        // Arrange: usuario existente con hash válido
        String passPlano = "Abcdef12";
        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setNombreUsuario("clau");
        u.setCorreoElectronico("clau@x.com");
        u.setRol(Rol.USUARIO);
        u.setContrasenaHash(PasswordUtil.hashPassword(passPlano));
        u.setActivo(true);

        when(usuarioDAO.buscarPorNombreUsuario("clau")).thenReturn(Optional.of(u));

        // Act
        Usuario logueado = authService.iniciarSesion("clau", passPlano);

        // Assert
        assertNotNull(logueado);
        assertEquals("clau", logueado.getNombreUsuario());
        assertTrue(SessionManager.getInstance().isLoggedIn());
        assertEquals("clau", SessionManager.getInstance().getUsuarioActual().getNombreUsuario());
    }

    @Test
    void iniciarSesion_usuarioNoExiste_lanzaAutenticacion() {
        when(usuarioDAO.buscarPorNombreUsuario("nope")).thenReturn(Optional.empty());
        assertThrows(AutenticacionException.class, () -> authService.iniciarSesion("nope", "x"));
        assertFalse(SessionManager.getInstance().isLoggedIn());
    }

    @Test
    void iniciarSesion_contrasenaInvalida_lanzaAutenticacion() {
        // Usuario existe, pero el hash no corresponde
        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setNombreUsuario("clau");
        u.setContrasenaHash(PasswordUtil.hashPassword("OtraPass99"));
        u.setRol(Rol.USUARIO);
        u.setActivo(true);

        when(usuarioDAO.buscarPorNombreUsuario("clau")).thenReturn(Optional.of(u));

        assertThrows(AutenticacionException.class, () -> authService.iniciarSesion("clau", "Abcdef12"));
        assertFalse(SessionManager.getInstance().isLoggedIn());
    }

    @Test
    void cerrarSesion_y_obtenerUsuarioActual() {
        // Prepara una sesión
        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setNombreUsuario("clau");
        SessionManager.getInstance().setUsuarioActual(u);
        assertTrue(SessionManager.getInstance().isLoggedIn());

        // Act + Assert
        authService.cerrarSesion();
        assertFalse(SessionManager.getInstance().isLoggedIn());
        assertNull(authService.obtenerUsuarioActual());
    }
}
