package com.ligiamanzano.salon;

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
class ContabilidadParametrosIT {

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

    ResultActions leer(String ruta) throws Exception {
        return mvc.perform(get(ruta).with(authentication(sesionContador)));
    }

    static String cuenta(String codigo, String nombre, int nivel, String padre, boolean movimiento) {
        return """
                { "codigo": "%s", "nombre": "%s", "tipo": "ORDEN_DEUDORA", "naturaleza": "D", "nivel": %d,
                  "codigoPadre": %s, "aceptaMovimiento": %s }
                """.formatted(codigo, nombre, nivel, padre == null ? "null" : "\"" + padre + "\"", movimiento);
    }

    @Test
    void catalogoTiposYConfiguracion() throws Exception {
        assumeTrue(jdbc.sql("select count(*) from tb_cuentas_contables where codigo like '9%'").query(Long.class).single() == 0,
                "Ya hay cuentas que empiezan con 9 en la base");

        enviar(post("/api/contabilidad/cuentas"), cuenta("9", "Cuentas de prueba", 0, null, false))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activa").value(true));
        enviar(post("/api/contabilidad/cuentas"), cuenta("91", "Grupo de prueba", 1, "9", false)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuenta("9101", "Detalle uno", 2, "91", true)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuenta("9102", "Detalle dos", 2, "91", true)).andExpect(status().isCreated());

        enviar(post("/api/contabilidad/cuentas"), cuenta("9101", "Repetida", 2, "91", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo").value("Ya existe una cuenta con ese código."));
        enviar(post("/api/contabilidad/cuentas"), cuenta("8101", "Sin prefijo", 2, "91", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo").value("El código debe empezar con 91 y ser más largo."));
        enviar(post("/api/contabilidad/cuentas"), cuenta("910101", "Hija de detalle", 3, "9101", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigoPadre").value("La cuenta 9101 acepta movimiento: no puede tener subcuentas"));
        enviar(post("/api/contabilidad/cuentas"), cuenta("9103", "Nivel malo", 1, "91", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nivel").value("Tiene que ser mayor que el nivel del padre (1)."));
        enviar(post("/api/contabilidad/cuentas"), cuenta("9x", "Letras", 1, "9", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo").exists());

        leer("/api/contabilidad/cuentas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == '9101')].codigoPadre").value(contains("91")))
                .andExpect(jsonPath("$[?(@.codigo == '9101')].nivel").value(contains(2)));

        enviar(put("/api/contabilidad/cuentas/91"), "{ \"nombre\": \"Grupo\", \"aceptaMovimiento\": true, \"activa\": true }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.aceptaMovimiento").value("La cuenta 91 tiene subcuentas: no puede aceptar movimiento"));
        enviar(put("/api/contabilidad/cuentas/9102"), "{ \"nombre\": \"Detalle dos editado\", \"aceptaMovimiento\": true, \"activa\": false }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Detalle dos editado"))
                .andExpect(jsonPath("$.activa").value(false));
        enviar(delete("/api/contabilidad/cuentas/91"), "").andExpect(status().isConflict());
        enviar(delete("/api/contabilidad/cuentas/9102"), "").andExpect(status().isNoContent());
        enviar(put("/api/contabilidad/cuentas/9999999"), "{ \"nombre\": \"x\", \"aceptaMovimiento\": true, \"activa\": true }")
                .andExpect(status().isNotFound());

        leer("/api/contabilidad/tipos-partida")
                .andExpect(jsonPath("$[?(@.codigo == 'PDA')].predeterminado").value(contains(true)));
        enviar(post("/api/contabilidad/tipos-partida"), "{ \"codigo\": \"TST\", \"nombre\": \"Prueba\", \"predeterminado\": true, \"activo\": true }")
                .andExpect(status().isCreated());
        leer("/api/contabilidad/tipos-partida")
                .andExpect(jsonPath("$[?(@.codigo == 'PDA')].predeterminado").value(contains(false)))
                .andExpect(jsonPath("$[?(@.predeterminado == true)].codigo").value(contains("TST")));
        enviar(put("/api/contabilidad/tipos-partida/TST"), "{ \"codigo\": \"TST\", \"nombre\": \"Prueba\", \"predeterminado\": false, \"activo\": true }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.predeterminado").exists());
        enviar(put("/api/contabilidad/tipos-partida/TST"), "{ \"codigo\": \"TST\", \"nombre\": \"Prueba\", \"predeterminado\": true, \"activo\": false }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.activo").value("El tipo predeterminado no se puede desactivar."));
        enviar(post("/api/contabilidad/tipos-partida"), "{ \"codigo\": \"TST\", \"nombre\": \"Otra\", \"predeterminado\": false, \"activo\": true }")
                .andExpect(status().isConflict());
        enviar(post("/api/contabilidad/tipos-partida"), "{ \"codigo\": \"tst\", \"nombre\": \"Otra\", \"predeterminado\": false, \"activo\": true }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo").value("Son 3 letras mayúsculas, por ejemplo PDA"));

        String config = """
                { "ctaUtilidadEjercicio": null, "ctaUtilidadesAnteriores": null, "ctaPerdidaEjercicio": null, "ctaPerdidasAnteriores": null,
                  "ctaPerdidasGanancias": null, "ctaReservaLegal": null, "ctaClientes": "9101", "ctaProveedores": null,
                  "tipoPartidaVentas": "ING", "tipoPartidaCompras": "COM", "modoVentas": "RESUMEN", "modoCompras": "DETALLE" }
                """;
        enviar(put("/api/contabilidad/configuracion"), config)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ctaClientes").value("9101"))
                .andExpect(jsonPath("$.modoVentas").value("RESUMEN"));
        enviar(put("/api/contabilidad/configuracion"), config.replace("\"9101\"", "\"91\"").replace("\"COM\"", "\"XXX\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.ctaClientes").value("La cuenta 91 no acepta movimiento."))
                .andExpect(jsonPath("$.campos.tipoPartidaCompras").value("Elige un tipo de partida activo."));
        enviar(put("/api/contabilidad/configuracion"), config.replace("\"RESUMEN\"", "\"OTRO\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.modoVentas").value("Elige Detalle o Resumen"));
        leer("/api/contabilidad/configuracion").andExpect(jsonPath("$.ctaClientes").value("9101"));
    }

    @Test
    void ejerciciosYMeses() throws Exception {
        assumeTrue(jdbc.sql("select count(*) from tb_ejercicios where anio = 2099").query(Long.class).single() == 0,
                "Ya existe el ejercicio 2099 en la base");

        enviar(post("/api/contabilidad/ejercicios"), "{ \"anio\": 2099 }")
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.anio").value(2099))
                .andExpect(jsonPath("$.fechaInicio").value("2099-01-01"))
                .andExpect(jsonPath("$.fechaFin").value("2099-12-31"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.periodos", hasSize(12)))
                .andExpect(jsonPath("$.periodos[0].estado").value("ABIERTO"))
                .andExpect(jsonPath("$.periodos[0].cerradoEn").value(nullValue()));
        enviar(post("/api/contabilidad/ejercicios"), "{ \"anio\": 2099 }").andExpect(status().isConflict());
        enviar(post("/api/contabilidad/ejercicios"), "{ \"anio\": 1999 }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.anio").exists());
        leer("/api/contabilidad/ejercicios").andExpect(jsonPath("$[?(@.anio == 2099)].periodos[11].mes").value(contains(12)));

        boolean hayMesesAnteriores = jdbc.sql("select count(*) from tb_periodos_contables where estado = 'ABIERTO' and anio < 2099")
                .query(Long.class).single() > 0;
        if (!hayMesesAnteriores) {
            sinDejarRastro(post("/api/contabilidad/periodos/2099/2/cerrar"), "")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("REGLA_DE_LA_BASE"))
                    .andExpect(jsonPath("$.mensaje").value("Solo se puede cerrar el mes abierto más antiguo (1/2099)"));
            enviar(post("/api/contabilidad/periodos/2099/1/cerrar"), "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodos[0].estado").value("CERRADO"))
                    .andExpect(jsonPath("$.periodos[0].cerradoEn").exists());
            enviar(post("/api/contabilidad/periodos/2099/2/cerrar"), "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodos[1].estado").value("CERRADO"));
            sinDejarRastro(post("/api/contabilidad/periodos/2099/1/reabrir"), "")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.mensaje").value("Solo se puede reabrir el último mes cerrado (2/2099)"));
            enviar(post("/api/contabilidad/periodos/2099/2/reabrir"), "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.periodos[1].estado").value("ABIERTO"))
                    .andExpect(jsonPath("$.periodos[1].cerradoEn").value(nullValue()));
            assertEquals(1L, jdbc.sql("""
                    select count(*) from tb_bitacora
                     where modulo = 'CONTABILIDAD' and accion = 'REABRIR_MES' and descripcion = 'Reabrió el mes 2/2099'
                    """).query(Long.class).single());
        }
        enviar(post("/api/contabilidad/periodos/2099/13/cerrar"), "").andExpect(status().isNotFound());
        enviar(post("/api/contabilidad/periodos/2098/1/cerrar"), "").andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void auditorSoloLee() throws Exception {
        mvc.perform(get("/api/contabilidad/cuentas")).andExpect(status().isOk());
        mvc.perform(get("/api/contabilidad/ejercicios")).andExpect(status().isOk());
        mvc.perform(get("/api/contabilidad/tipos-partida")).andExpect(status().isOk());
        mvc.perform(get("/api/contabilidad/configuracion")).andExpect(status().isOk());
        mvc.perform(post("/api/contabilidad/ejercicios").contentType(MediaType.APPLICATION_JSON).content("{ \"anio\": 2099 }"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/contabilidad/periodos/2099/1/cerrar")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionNoEntra() throws Exception {
        mvc.perform(get("/api/contabilidad/cuentas")).andExpect(status().isForbidden());
        mvc.perform(get("/api/contabilidad/configuracion")).andExpect(status().isForbidden());
    }
}
