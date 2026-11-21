package com.ligiamanzano.salon.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.ErrorResponse;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> credenciales(CredencialesInvalidasException e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage(), "CREDENCIALES_INVALIDAS", null);
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<Map<String, Object>> operacion(OperacionInvalidaException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), e.getCodigo(), e.getCampos());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, Object>> conflicto(ConflictoException e) {
        ResponseEntity<Map<String, Object>> respuesta = error(HttpStatus.CONFLICT, e.getMessage(), e.getCodigo(), e.getCampos());
        if (e.getExtra() != null) respuesta.getBody().putAll(e.getExtra());
        return respuesta;
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), "NO_ENCONTRADO", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Revisa los campos marcados", "DATOS_INVALIDOS", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "La petición no tiene el formato esperado", "JSON_INVALIDO", null);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> sinPermiso(AuthorizationDeniedException e) {
        return error(HttpStatus.FORBIDDEN, "No tienes permiso para esta acción", "SIN_PERMISO", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "Revisa los campos marcados", "DATOS_INVALIDOS",
                Map.of(e.getName(), "Valor no válido"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        if (e instanceof ErrorResponse respuestaDeSpring) {
            return errorDeSpring(respuestaDeSpring);
        }
        log.error("Error no controlado", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error. Intenta de nuevo.", "ERROR_INTERNO", null);
    }

    private ResponseEntity<Map<String, Object>> errorDeSpring(ErrorResponse e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null || status.is5xxServerError()) {
            log.error("Error no controlado", (Exception) e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error. Intenta de nuevo.", "ERROR_INTERNO", null);
        }
        return switch (status) {
            case NOT_FOUND -> error(status, "No existe", "NO_ENCONTRADO", null);
            case METHOD_NOT_ALLOWED -> error(status, "Esta acción no está disponible en esa dirección", "METODO_NO_PERMITIDO", null);
            case UNSUPPORTED_MEDIA_TYPE -> error(status, "La petición tiene que ser JSON", "TIPO_NO_SOPORTADO", null);
            default -> error(status, "La petición no es válida", "PETICION_INVALIDA", null);
        };
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String mensaje, String codigo,
                                                      Map<String, String> campos) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("codigo", codigo);
        if (campos != null && !campos.isEmpty()) cuerpo.put("campos", campos);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
