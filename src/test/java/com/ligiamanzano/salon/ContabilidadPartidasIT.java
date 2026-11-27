package com.ligiamanzano.salon;

import com.jayway.jsonpath.JsonPath;
import com.ligiamanzano.salon.security.Rol;
import com.ligiamanzano.salon.security.UsuarioAutenticado;
import com.ligiamanzano.salon.usuarios.entity.UsuarioEntity;
import com.ligiamanzano.salon.usuarios.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContabilidadPartidasIT {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired EntityManager em;

    Authentication sesionContador;

    @BeforeEach
    void preparar() {
        String sufijo = String.valueOf(System.nanoTime() % 100000);
        UsuarioEntity contador = new UsuarioEntity();
        contador.setNombreUsuario("prueba.conta" + sufijo);
        contador.setNombreCompleto("Prueba Contadora");
        contador.setContrasenaHash("sin-clave");
        contador.setIdRol(Rol.CONTADOR.getId());
        contador.setActivo(true);
        contador = usuarios.save(contador);
        UsuarioAutenticado u = new UsuarioAutenticado(contador.getIdUsuario(), contador.getNombreUsuario(),
                contador.getNombreCompleto(), Rol.CONTADOR, false, UUID.randomUUID());
        sesionContador = UsernamePasswordAuthenticationToken.authenticated(u, null,
                List.of(new SimpleGrantedAuthority("ROLE_CONTADOR")));
    }

    ResultActions enviar(MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        ResultActions r = mvc.perform(peticion.with(authentication(sesionContador)).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
        em.clear();
        return r;
    }

    ResultActions sinDejarRastro(MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        em.flush();
        jdbc.sql("savepoint antes_del_error").update();
        ResultActions r = enviar(peticion, cuerpo);
        jdbc.sql("rollback to savepoint antes_del_error").update();
        em.clear();
        return r;
    }

    ResultActions leer(String ruta, String... parametros) throws Exception {
        MockHttpServletRequestBuilder peticion = get(ruta).with(authentication(sesionContador));
        for (int i = 0; i < parametros.length; i += 2) {
            peticion.param(parametros[i], parametros[i + 1]);
        }
        return mvc.perform(peticion);
    }

    static <T> T valor(ResultActions r, String ruta) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), ruta);
    }

    static String cuenta(String codigo, String nombre, int nivel, String padre, boolean movimiento) {
        return """
                { "codigo": "%s", "nombre": "%s", "tipo": "ORDEN_DEUDORA", "naturaleza": "D", "nivel": %d,
                  "codigoPadre": %s, "aceptaMovimiento": %s }
                """.formatted(codigo, nombre, nivel, padre == null ? "null" : "\"" + padre + "\"", movimiento);
    }

    static String partida(String fecha, String concepto, String lineas) {
        return """
                { "fecha": "%s", "codigoTipo": "PDA", "concepto": "%s", "lineas": [ %s ] }
                """.formatted(fecha, concepto, lineas);
    }

    static String linea(String cuenta, String debe, String haber) {
        return "{ \"codigoCuenta\": \"%s\", \"concepto\": null, \"debe\": %s, \"haber\": %s }".formatted(cuenta, debe, haber);
    }

    @Test
    void partidasEjerciciosYPeriodos() throws Exception {
        assumeTrue(jdbc.sql("select count(*) from tb_ejercicios where anio in (2098, 2099)").query(Long.class).single() == 0,
                "Ya existe el ejercicio 2099 en development");
        enviar(post("/api/contabilidad/cuentas"), cuenta("9", "Cuentas de prueba", 0, null, false)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuenta("91", "Grupo de prueba", 1, "9", false)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuenta("9101", "Caja de prueba", 2, "91", true)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuenta("9102", "Ingresos de prueba", 2, "91", true)).andExpect(status().isCreated());

        enviar(post("/api/contabilidad/ejercicios"), "{ \"anio\": 2099 }")
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anio").value(2099))
                .andExpect(jsonPath("$.fechaFin").value("2099-12-31"))
                .andExpect(jsonPath("$.periodos", hasSize(12)))
                .andExpect(jsonPath("$.periodos[0].estado").value("ABIERTO"));
        enviar(post("/api/contabilidad/ejercicios"), "{ \"anio\": 2099 }").andExpect(status().isConflict());
        enviar(post("/api/contabilidad/ejercicios"), "{ \"anio\": 1999 }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.anio").exists());

        int id = valor(enviar(post("/api/contabilidad/partidas"),
                        partida("2099-01-15", "Venta de prueba", linea("9101", "10.00", "0") + ", " + linea("9102", "0", "9.00")))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.numero").value(nullValue()))
                .andExpect(jsonPath("$.origen").value("MANUAL"))
                .andExpect(jsonPath("$.nombreTipo").value("Partida de diario"))
                .andExpect(jsonPath("$.totalDebe").value(10.00))
                .andExpect(jsonPath("$.totalHaber").value(9.00))
                .andExpect(jsonPath("$.creadoPor").value("Prueba Contadora"))
                .andExpect(jsonPath("$.lineas", hasSize(2)))
                .andExpect(jsonPath("$.lineas[0].nombreCuenta").value("Caja de prueba"))
                .andExpect(jsonPath("$.lineas[1].linea").value(2)), "$.idPartida");

        sinDejarRastro(post("/api/contabilidad/partidas/" + id + "/aplicar"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La partida no cuadra: debe 10.00, haber 9.00 (2 líneas)"));
        enviar(post("/api/contabilidad/partidas"), partida("2099-01-15", "Mala", linea("9101", "5.00", "5.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.lineas").value("Línea 1: pon el monto en el debe o en el haber, no en los dos ni en cero."));
        enviar(post("/api/contabilidad/partidas"), partida("2099-01-15", "Mala", linea("0000", "5.00", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.lineas").value("Línea 1: elige una cuenta del plan."));
        sinDejarRastro(post("/api/contabilidad/partidas"), partida("2099-01-15", "Agrupadora", linea("91", "5.00", "0")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La cuenta 91 no acepta movimiento o está inactiva"));
        sinDejarRastro(post("/api/contabilidad/partidas"), partida("2098-01-15", "Sin ejercicio", linea("9101", "5.00", "0")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El mes 1/2098 está cerrado o no existe"));

        enviar(put("/api/contabilidad/partidas/" + id),
                        partida("2099-01-16", "Venta de prueba corregida", linea("9101", "10.00", "0") + ", " + linea("9102", "0", "10.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2099-01-16"))
                .andExpect(jsonPath("$.totalHaber").value(10.00));
        enviar(post("/api/contabilidad/partidas/" + id + "/aplicar"), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("010001"));
        enviar(put("/api/contabilidad/partidas/" + id), partida("2099-01-16", "Otra", linea("9101", "1.00", "0")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La partida 010001 está aplicada: no se modifica (art. 139 del Código Tributario)"));
        enviar(delete("/api/contabilidad/partidas/" + id), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("La partida 010001 está aplicada: no se borra")));
        sinDejarRastro(post("/api/contabilidad/partidas/" + id + "/aplicar"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La partida " + id + " no existe o ya está aplicada"));

        int idInversa = valor(enviar(post("/api/contabilidad/partidas/" + id + "/anular"), "{ \"motivo\": \"Registrada por error\", \"fecha\": \"2099-01-20\" }")
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("ANULACION"))
                .andExpect(jsonPath("$.estado").value("APLICADA"))
                .andExpect(jsonPath("$.numero").value("010002"))
                .andExpect(jsonPath("$.idPartidaAnula").value(id))
                .andExpect(jsonPath("$.numeroAnula").value("010001"))
                .andExpect(jsonPath("$.concepto").value("Anulación de la partida PDA 010001: Registrada por error"))
                .andExpect(jsonPath("$.lineas[0].haber").value(10.00)), "$.idPartida");
        leer("/api/contabilidad/partidas/" + id)
                .andExpect(jsonPath("$.idPartidaAnuladaPor").value(idInversa))
                .andExpect(jsonPath("$.numeroAnuladaPor").value("010002"));
        sinDejarRastro(post("/api/contabilidad/partidas/" + id + "/anular"), "{ \"motivo\": \"Otra vez\", \"fecha\": \"2099-01-20\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La partida 010001 ya fue anulada"));
        enviar(post("/api/contabilidad/partidas/" + id + "/anular"), "{ \"motivo\": \"x\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.fecha").exists());

        leer("/api/contabilidad/partidas", "anio", "2099", "buscar", "anulacion")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].idPartida").value(idInversa))
                .andExpect(jsonPath("$.contenido[0].lineas").doesNotExist());
        leer("/api/contabilidad/partidas", "anio", "2099", "estado", "aplicada")
                .andExpect(jsonPath("$.totalElementos").value(2));

        int idBorrador = valor(enviar(post("/api/contabilidad/partidas"), partida("2099-01-25", "Pendiente", linea("9101", "1.00", "0")))
                .andExpect(status().isCreated()), "$.idPartida");

        boolean hayMesesAnteriores = jdbc.sql("select count(*) from tb_periodos_contables where estado = 'ABIERTO' and anio < 2099")
                .query(Long.class).single() > 0;
        if (!hayMesesAnteriores) {
            sinDejarRastro(post("/api/contabilidad/periodos/2099/1/cerrar"), "")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensaje").value("El mes 1/2099 tiene partidas en borrador: aplíquelas o bórrelas antes de cerrarlo"));
            sinDejarRastro(post("/api/contabilidad/periodos/2099/2/cerrar"), "")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensaje").value("Solo se puede cerrar el mes abierto más antiguo (1/2099)"));
        }
        enviar(delete("/api/contabilidad/partidas/" + idBorrador), "").andExpect(status().isNoContent());
        leer("/api/contabilidad/partidas/" + idBorrador).andExpect(status().isNotFound());

        if (!hayMesesAnteriores) {
            enviar(post("/api/contabilidad/periodos/2099/1/cerrar"), "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodos[0].estado").value("CERRADO"))
                    .andExpect(jsonPath("$.periodos[0].cerradoEn").exists());
            sinDejarRastro(post("/api/contabilidad/partidas"), partida("2099-01-28", "Mes cerrado", linea("9101", "1.00", "0")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensaje").value("El mes 1/2099 está cerrado o no existe"));
            enviar(post("/api/contabilidad/periodos/2099/1/reabrir"), "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodos[0].estado").value("ABIERTO"));
            assertEquals(1L, jdbc.sql("""
                    select count(*) from tb_bitacora
                     where modulo = 'CONTABILIDAD' and accion = 'REABRIR_MES' and descripcion = 'Reabrió el mes 1/2099'
                    """).query(Long.class).single());
        }
        enviar(post("/api/contabilidad/periodos/2099/13/cerrar"), "").andExpect(status().isNotFound());
        leer("/api/contabilidad/ejercicios").andExpect(jsonPath("$[?(@.anio == 2099)].periodos[0].mes").value(contains(1)));
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void auditorSoloLee() throws Exception {
        mvc.perform(get("/api/contabilidad/partidas")).andExpect(status().isOk());
        mvc.perform(get("/api/contabilidad/partidas/999999999")).andExpect(status().isNotFound());
        mvc.perform(post("/api/contabilidad/partidas").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"fecha\": \"2099-01-15\", \"codigoTipo\": \"PDA\", \"concepto\": \"x\", \"lineas\": [] }"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/contabilidad/partidas/1")).andExpect(status().isForbidden());
        mvc.perform(post("/api/contabilidad/partidas/1/aplicar")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionNoEntra() throws Exception {
        mvc.perform(get("/api/contabilidad/partidas")).andExpect(status().isForbidden());
    }
}
