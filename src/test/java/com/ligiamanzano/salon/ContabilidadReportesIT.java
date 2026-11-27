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
class ContabilidadReportesIT {

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

    static String cuentaDe(String codigo, String nombre, String tipo, String naturaleza, int nivel, String padre, boolean movimiento) {
        return """
                { "codigo": "%s", "nombre": "%s", "tipo": "%s", "naturaleza": "%s", "nivel": %d,
                  "codigoPadre": %s, "aceptaMovimiento": %s }
                """.formatted(codigo, nombre, tipo, naturaleza, nivel, padre == null ? "null" : "\"" + padre + "\"", movimiento);
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
    void reportesYCierreDelEjercicio() throws Exception {
        assumeTrue(jdbc.sql("select count(*) from tb_ejercicios where anio in (2099, 2100)").query(Long.class).single() == 0,
                "Ya existe el ejercicio 2099 o 2100 en development");
        assumeTrue(jdbc.sql("select count(*) from tb_periodos_contables where estado = 'ABIERTO' and anio < 2099").query(Long.class).single() == 0,
                "Hay meses abiertos de años anteriores: no se puede cerrar 2099 en la prueba");
        enviar(post("/api/contabilidad/cuentas"), cuenta("9", "Cuentas de prueba", 0, null, false)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuenta("91", "Grupo de prueba", 1, "9", false)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuentaDe("9101", "Caja de prueba", "ACTIVO", "D", 2, "91", true)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuentaDe("9102", "Ingresos de prueba", "INGRESO", "A", 2, "91", true)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuentaDe("9103", "Gastos de prueba", "COSTO_GASTO", "D", 2, "91", true)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuentaDe("9104", "Pérdidas y ganancias de prueba", "LIQUIDADORA", "A", 2, "91", true)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuentaDe("9105", "Utilidad del ejercicio de prueba", "CAPITAL", "A", 2, "91", true)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuentaDe("9106", "Pérdida del ejercicio de prueba", "CAPITAL", "A", 2, "91", true)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/cuentas"), cuentaDe("9107", "Utilidades anteriores de prueba", "CAPITAL", "A", 2, "91", true)).andExpect(status().isCreated());
        enviar(post("/api/contabilidad/ejercicios"), "{ \"anio\": 2099 }").andExpect(status().isCreated());
        enviar(post("/api/contabilidad/ejercicios"), "{ \"anio\": 2100 }").andExpect(status().isCreated());

        int venta = valor(enviar(post("/api/contabilidad/partidas"),
                partida("2099-03-10", "Ventas de prueba", linea("9101", "113.00", "0") + ", " + linea("9102", "0", "113.00")))
                .andExpect(status().isCreated()), "$.idPartida");
        enviar(post("/api/contabilidad/partidas/" + venta + "/aplicar"), "").andExpect(status().isOk());
        int gasto = valor(enviar(post("/api/contabilidad/partidas"),
                partida("2099-03-15", "Gasto de prueba", linea("9103", "40.00", "0") + ", " + linea("9101", "0", "40.00")))
                .andExpect(status().isCreated()), "$.idPartida");
        enviar(post("/api/contabilidad/partidas/" + gasto + "/aplicar"), "").andExpect(status().isOk());

        leer("/api/contabilidad/reportes/saldos", "anio", "2099", "mes", "3")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filas[?(@.codigo == '9101')].debe").value(contains(113.00)))
                .andExpect(jsonPath("$.filas[?(@.codigo == '9101')].haber").value(contains(40.00)))
                .andExpect(jsonPath("$.filas[?(@.codigo == '9101')].saldoFinal").value(contains(73.00)))
                .andExpect(jsonPath("$.filas[?(@.codigo == '9102')].saldoFinal").value(contains(113.00)))
                .andExpect(jsonPath("$.filas[?(@.codigo == '9')].debe").value(contains(153.00)))
                .andExpect(jsonPath("$.filas[?(@.codigo == '9')].haber").value(contains(153.00)));
        leer("/api/contabilidad/reportes/saldos", "anio", "2099", "mes", "4")
                .andExpect(jsonPath("$.filas[?(@.codigo == '9101')].saldoInicial").value(contains(73.00)))
                .andExpect(jsonPath("$.filas[?(@.codigo == '9101')].debe").value(contains(0.00)));
        leer("/api/contabilidad/reportes/saldos", "anio", "2100", "mes", "1")
                .andExpect(jsonPath("$.filas[?(@.codigo == '9101')].saldoInicial").value(contains(73.00)))
                .andExpect(jsonPath("$.filas[?(@.codigo == '9102')].saldoInicial").value(contains(0.00)));
        leer("/api/contabilidad/reportes/saldos", "anio", "2099", "mes", "3", "nivel", "1")
                .andExpect(jsonPath("$.filas[?(@.codigo == '9101')]", hasSize(0)))
                .andExpect(jsonPath("$.filas[?(@.codigo == '91')]", hasSize(1)));
        leer("/api/contabilidad/reportes/saldos", "anio", "2099", "mes", "13", "nivel", "7")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.mes").exists())
                .andExpect(jsonPath("$.campos.nivel").exists());
        leer("/api/contabilidad/reportes/saldos", "mes", "3")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.anio").exists());

        leer("/api/contabilidad/reportes/mayor", "cuenta", "9101", "desde", "2099-03-01", "hasta", "2099-03-31")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.nombre").value("Caja de prueba"))
                .andExpect(jsonPath("$.filas", hasSize(3)))
                .andExpect(jsonPath("$.filas[0].fila").value(0))
                .andExpect(jsonPath("$.filas[0].concepto").value("Saldo anterior"))
                .andExpect(jsonPath("$.filas[0].saldo").value(0.00))
                .andExpect(jsonPath("$.filas[1].debe").value(113.00))
                .andExpect(jsonPath("$.filas[2].saldo").value(73.00));
        leer("/api/contabilidad/reportes/mayor", "cuenta", "91", "desde", "2099-03-01", "hasta", "2099-03-31")
                .andExpect(jsonPath("$.cuenta.aceptaMovimiento").value(false))
                .andExpect(jsonPath("$.filas", hasSize(5)));
        sinDejarRastro(get("/api/contabilidad/reportes/mayor").param("cuenta", "999999").param("desde", "2099-03-01").param("hasta", "2099-03-31"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La cuenta 999999 no existe"));
        sinDejarRastro(get("/api/contabilidad/reportes/mayor").param("cuenta", "9101").param("desde", "2099-03-31").param("hasta", "2099-03-01"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("La fecha final es anterior a la inicial"));

        leer("/api/contabilidad/reportes/diario", "desde", "2099-03-01", "hasta", "2099-03-31")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partidas", hasSize(2)))
                .andExpect(jsonPath("$.partidas[0].tipo").value("Partida de diario"))
                .andExpect(jsonPath("$.partidas[0].lineas", hasSize(2)))
                .andExpect(jsonPath("$.partidas[0].totalDebe").value(113.00))
                .andExpect(jsonPath("$.totalDebe").value(153.00))
                .andExpect(jsonPath("$.totalHaber").value(153.00));
        leer("/api/contabilidad/reportes/diario", "desde", "2099-03-01", "hasta", "2099-03-31", "tipo", "ING")
                .andExpect(jsonPath("$.partidas", hasSize(0)))
                .andExpect(jsonPath("$.totalDebe").value(0.00));

        sinDejarRastro(post("/api/contabilidad/ejercicios/2099/partida-cierre"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("Configure en Contabilidad > Parámetros")));
        enviar(put("/api/contabilidad/configuracion"), """
                { "ctaUtilidadEjercicio": "9105", "ctaUtilidadesAnteriores": "9107", "ctaPerdidaEjercicio": "9106", "ctaPerdidasAnteriores": null,
                  "ctaPerdidasGanancias": "9104", "ctaReservaLegal": null, "ctaClientes": null, "ctaProveedores": null,
                  "tipoPartidaVentas": "ING", "tipoPartidaCompras": "COM", "modoVentas": "DETALLE", "modoCompras": "DETALLE" }
                """).andExpect(status().isOk());
        int borrador = valor(enviar(post("/api/contabilidad/partidas"), partida("2099-12-20", "Pendiente", linea("9101", "1.00", "0")))
                .andExpect(status().isCreated()), "$.idPartida");
        sinDejarRastro(post("/api/contabilidad/ejercicios/2099/partida-cierre"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El ejercicio 2099 tiene partidas en borrador: aplíquelas o bórrelas antes del cierre"));
        enviar(delete("/api/contabilidad/partidas/" + borrador), "").andExpect(status().isNoContent());

        int cierre = valor(enviar(post("/api/contabilidad/ejercicios/2099/partida-cierre"), "")
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.origen").value("CIERRE"))
                .andExpect(jsonPath("$.fecha").value("2099-12-31"))
                .andExpect(jsonPath("$.totalDebe").value(186.00))
                .andExpect(jsonPath("$.totalHaber").value(186.00))
                .andExpect(jsonPath("$.lineas[?(@.codigoCuenta == '9102')].debe").value(contains(113.00)))
                .andExpect(jsonPath("$.lineas[?(@.codigoCuenta == '9103')].haber").value(contains(40.00)))
                .andExpect(jsonPath("$.lineas[?(@.codigoCuenta == '9105')].haber").value(contains(73.00))), "$.idPartida");
        sinDejarRastro(post("/api/contabilidad/ejercicios/2099/partida-cierre"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", startsWith("El ejercicio 2099 ya tiene su partida de cierre")));
        sinDejarRastro(post("/api/contabilidad/ejercicios/2099/cerrar"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Cierre primero los 12 meses del ejercicio 2099"));

        enviar(post("/api/contabilidad/partidas/" + cierre + "/aplicar"), "").andExpect(status().isOk());
        leer("/api/contabilidad/reportes/saldos", "anio", "2099", "mes", "12")
                .andExpect(jsonPath("$.filas[?(@.codigo == '9102')].saldoFinal").value(contains(0.00)))
                .andExpect(jsonPath("$.filas[?(@.codigo == '9105')].saldoFinal").value(contains(73.00)));
        for (int mes = 1; mes <= 12; mes++) {
            enviar(post("/api/contabilidad/periodos/2099/" + mes + "/cerrar"), "").andExpect(status().isOk());
        }
        int traslado = valor(enviar(post("/api/contabilidad/ejercicios/2099/cerrar"), "")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPartidaTraslado").isNumber()), "$.idPartidaTraslado");
        leer("/api/contabilidad/partidas/" + traslado)
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.origen").value("APERTURA"))
                .andExpect(jsonPath("$.fecha").value("2100-01-01"))
                .andExpect(jsonPath("$.lineas[0].codigoCuenta").value("9105"))
                .andExpect(jsonPath("$.lineas[0].debe").value(73.00))
                .andExpect(jsonPath("$.lineas[1].codigoCuenta").value("9107"));
        leer("/api/contabilidad/ejercicios").andExpect(jsonPath("$[?(@.anio == 2099)].estado").value(contains("CERRADO")));
        sinDejarRastro(post("/api/contabilidad/periodos/2099/12/reabrir"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El ejercicio 2099 está cerrado"));
        sinDejarRastro(post("/api/contabilidad/ejercicios/2099/cerrar"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El ejercicio 2099 no existe o ya está cerrado"));
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void auditorSoloLee() throws Exception {
        mvc.perform(get("/api/contabilidad/reportes/saldos").param("anio", "2099").param("mes", "1")).andExpect(status().isOk());
        mvc.perform(get("/api/contabilidad/reportes/diario").param("desde", "2099-01-01").param("hasta", "2099-01-31")).andExpect(status().isOk());
        mvc.perform(get("/api/contabilidad/reportes/mayor").param("cuenta", "1").param("desde", "2099-01-01").param("hasta", "2099-01-31")).andExpect(status().isOk());
        mvc.perform(post("/api/contabilidad/ejercicios/2099/partida-cierre")).andExpect(status().isForbidden());
        mvc.perform(post("/api/contabilidad/ejercicios/2099/cerrar")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionNoEntra() throws Exception {
        mvc.perform(get("/api/contabilidad/reportes/saldos").param("anio", "2099").param("mes", "1")).andExpect(status().isForbidden());
        mvc.perform(post("/api/contabilidad/ejercicios/2099/cerrar")).andExpect(status().isForbidden());
    }
}
