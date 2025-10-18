package com.cineticket.servicio;

import com.cineticket.dao.UsuarioDAO;
import com.cineticket.enums.Rol;
import com.cineticket.excepcion.AutenticacionException;
import com.cineticket.excepcion.ValidacionException;
import com.cineticket.modelo.Usuario;
import com.cineticket.util.PasswordUtil;
import com.cineticket.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioDAO usuarioDAO;

    // Reglas de validación (documento de clases)
    private static final Pattern EMAIL_REGEX =
            Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USER_REGEX =
            Pattern.compile("^[A-Za-z0-9._-]{4,}$");

    public AuthService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO es requerido");
    }

    /** Login: valida credenciales y establece sesión */
    public Usuario iniciarSesion(String nombreUsuario, String contrasena) {
        if (isBlank(nombreUsuario) || isBlank(contrasena)) {
            throw new ValidacionException("Usuario y contraseña son obligatorios.");
        }

        Optional<Usuario> opt = usuarioDAO.buscarPorNombreUsuario(nombreUsuario);
        Usuario u = opt.orElseThrow(() -> {
            log.warn("Login fallido: usuario '{}' no existe", nombreUsuario);
            return new AutenticacionException("Credenciales inválidas.");
        });

        if (!validarCredenciales(u, contrasena)) {
            log.warn("Login fallido: contraseña inválida para '{}'", nombreUsuario);
            throw new AutenticacionException("Credenciales inválidas.");
        }
        if (!u.isActivo()) {
            throw new AutenticacionException("La cuenta está desactivada.");
        }

        SessionManager.getInstance().setUsuarioActual(u);
        log.info("Usuario '{}' inició sesión (rol={})", u.getNombreUsuario(), u.getRol());
        return u;
    }

    /** Registro: valida datos, evita duplicados, hashea y crea */
    public Usuario registrarUsuario(String nombreCompleto,
                                    String correo,
                                    String nombreUsuario,
                                    String contrasena) {
        validarDatosRegistro(correo, nombreUsuario, contrasena);

        if (usuarioDAO.existeCorreo(correo)) {
            throw new ValidacionException("El correo ya está registrado.");
        }
        if (usuarioDAO.existeNombreUsuario(nombreUsuario)) {
            throw new ValidacionException("El nombre de usuario ya está en uso.");
        }

        String hash = PasswordUtil.hashPassword(contrasena);

        Usuario nuevo = new Usuario();
        nuevo.setNombreCompleto(nombreCompleto);
        nuevo.setCorreoElectronico(correo);
        nuevo.setNombreUsuario(nombreUsuario);
        nuevo.setContrasenaHash(hash);
        nuevo.setRol(Rol.USUARIO);
        // fechaRegistro y activo se inicializan en el VO (según tu diseño)

        usuarioDAO.crear(nuevo); // setea id vía generated keys en el DAO
        log.info("Usuario '{}' registrado con ID {}", nuevo.getNombreUsuario(), nuevo.getIdUsuario());
        return nuevo;
    }

    /** Obtiene el usuario actualmente logueado (o null) */
    public Usuario obtenerUsuarioActual() {
        return SessionManager.getInstance().getUsuarioActual();
    }

    /** Cierra sesión (limpia SessionManager) */
    public void cerrarSesion() {
        SessionManager.getInstance().cerrarSesion();
        log.info("Sesión cerrada.");
    }

    /** Cambio de contraseña (opcional en MVP, pero listo) */
    public boolean cambiarContrasena(Integer usuarioId,
                                     String contrasenaActual,
                                     String contrasenaNueva) {
        if (usuarioId == null) throw new ValidacionException("usuarioId es obligatorio.");
        if (isBlank(contrasenaActual) || isBlank(contrasenaNueva)) {
            throw new ValidacionException("Debe indicar la contraseña actual y la nueva.");
        }
        if (!PasswordUtil.validarFortaleza(contrasenaNueva)) {
            throw new ValidacionException("La nueva contraseña no cumple los requisitos mínimos.");
        }

        Usuario u = usuarioDAO.buscarPorId(usuarioId)
                .orElseThrow(() -> new ValidacionException("Usuario no encontrado."));

        if (!PasswordUtil.verificarPassword(contrasenaActual, u.getContrasenaHash())) {
            throw new AutenticacionException("La contraseña actual no es correcta.");
        }

        u.setContrasenaHash(PasswordUtil.hashPassword(contrasenaNueva));
        usuarioDAO.actualizar(u);
        log.info("Contraseña actualizada para usuario {}", u.getNombreUsuario());
        return true;
    }

    // ---- privados ----
    private boolean validarCredenciales(Usuario usuario, String contrasena) {
        return PasswordUtil.verificarPassword(contrasena, usuario.getContrasenaHash());
    }

    private void validarDatosRegistro(String correo, String nombreUsuario, String contrasena) {
        if (isBlank(correo) || isBlank(nombreUsuario) || isBlank(contrasena)) {
            throw new ValidacionException("Todos los campos son obligatorios.");
        }
        if (!EMAIL_REGEX.matcher(correo).matches()) {
            throw new ValidacionException("Formato de correo inválido.");
        }
        if (!USER_REGEX.matcher(nombreUsuario).matches()) {
            throw new ValidacionException("Usuario inválido: mínimo 4 caracteres, alfanumérico y ._-");
        }
        if (!PasswordUtil.validarFortaleza(contrasena)) {
            throw new ValidacionException("Contraseña débil: mínimo 8, 1 mayús, 1 minús y 1 dígito.");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
