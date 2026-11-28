package com.ligiamanzano.salon;

import com.jayway.jsonpath.JsonPath;
import com.ligiamanzano.salon.common.util.Fechas;
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

import java.math.BigDecimal;
import java.time.YearMonth;
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
class TarjetasEImpuestosIT {

    static final String LIQUIDACIONES = "/api/liquidaciones-tarjeta";
    static final String BANCO = "1101030101";

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired EntityManager em;

    String sufijo;
    Authentication sesionAdmin;
    Authentication sesionContador;

    @BeforeEach
    void preparar() {
        sufijo = String.valueOf(System.nanoTime() % 100000);
        sesionAdmin = sesion("prueba.tja" + sufijo, Rol.ADMINISTRADOR);
        sesionContador = sesion("prueba.tjc" + sufijo, Rol.CONTADOR);
    }

    Authentication sesion(String nombre, Rol rol) {
        UsuarioEntity u = new UsuarioEntity();
        u.setNombreUsuario(nombre);
        u.setNombreCompleto("Prueba Tarjetas");
        u.setContrasenaHash("sin-clave");
        u.setIdRol(rol.getId());
        u.setActivo(true);
        u = usuarios.save(u);
        return UsernamePasswordAuthenticationToken.authenticated(
                new UsuarioAutenticado(u.getIdUsuario(), u.getNombreUsuario(), u.getNombreCompleto(), rol, false, UUID.randomUUID()),
                null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
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

    static BigDecimal monto(ResultActions r, String ruta) throws Exception {
        return new BigDecimal(String.valueOf((Object) valor(r, ruta))).setScale(2);
    }

    int banco() throws Exception {
        return valor(mvc.perform(post("/api/proveedores").with(authentication(sesionAdmin)).contentType(MediaType.APPLICATION_JSON).content("""
                { "nombreComercial": "Banco Prueba %s", "razonSocial": "Banco Prueba, S.A.", "nit": "%014d", "nrc": "9%s",
                  "nacionalidad": "El Salvador", "contactoNombre": null, "telefono": null, "correo": null,
                  "direccion": null, "diasCredito": 0, "notas": null }
                """.formatted(sufijo, Long.parseLong(sufijo) + 7, sufijo))).andExpect(status().isCreated()), "$.idProveedor");
    }

    static String liquidacion(int idBanco, String numero, String codigo, String fecha, String periodo,
                              String liquidado, String anticipo, String comision, String depositado, String cuenta) {
        return """
                { "fecha": "%s", "idProveedor": %d, "numeroDocumento": %s, "codigoGeneracion": %s, "periodoIva": "%s",
                  "montoLiquidado": %s, "anticipoIva": %s, "comisionTotal": %s, "montoDepositado": %s,
                  "codigoCuentaBanco": "%s", "idCompraComision": null }
                """.formatted(fecha, idBanco, numero == null ? "null" : "\"" + numero + "\"",
                codigo == null ? "null" : "\"" + codigo + "\"", periodo, liquidado, anticipo, comision, depositado, cuenta);
    }

    void ejercicio2099() throws Exception {
        assumeTrue(jdbc.sql("select count(*) from tb_ejercicios where anio = 2099").query(Long.class).single() == 0,
                "Ya existe el ejercicio 2099 en development");
        enviar(post("/api/contabilidad/ejercicios"), "{ \"anio\": 2099 }").andExpect(status().isCreated());
    }

    @Test
    void liquidacionesDeTarjeta() throws Exception {
        ejercicio2099();
        jdbc.sql("""
                update tb_config_contable
                   set fecha_inicio = date '2099-01-01', cta_caja = '110101', cta_anticipo_iva = '110409', cta_proveedores = '21020101'
                 where id = 1
                """).update();
        int idBanco = banco();
        String numero = "LIQ-" + sufijo;
        String codigo = "a1b2c3d4-0000-4000-8000-0000000" + "%05d".formatted(Long.parseLong(sufijo));

        enviar(post(LIQUIDACIONES), liquidacion(idBanco, null, codigo, "2099-01-15", "2099-01-01", "842.50", "14.91", "13.56", "814.03", BANCO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.numeroDocumento").value("Escribe el número del documento"));
        enviar(post(LIQUIDACIONES), liquidacion(idBanco, numero, codigo, "2099-01-15", "2099-01-15", "842.50", "14.91", "13.56", "814.03", "1101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.periodoIva").value("Es el primer día del mes en que se declara (AAAA-MM-01)."))
                .andExpect(jsonPath("$.campos.codigoCuentaBanco").value("La cuenta 1101 no acepta movimiento."));
        enviar(post(LIQUIDACIONES), liquidacion(idBanco, numero, codigo, "2099-01-15", "2099-01-01", "842.50", "14.91", "13.56", "800.00", BANCO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_DE_LA_BASE"))
                .andExpect(jsonPath("$.mensaje").value("El monto depositado tiene que ser lo liquidado menos el anticipo y la comisión: $814.03."))
                .andExpect(jsonPath("$.campos.montoDepositado").exists());

        int id = valor(enviar(post(LIQUIDACIONES), liquidacion(idBanco, numero, codigo, "2099-01-15", "2099-01-01", "842.50", "14.91", "13.56", "814.03", BANCO))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreProveedor").value("Banco Prueba " + sufijo))
                .andExpect(jsonPath("$.codigoGeneracion").value(codigo))
                .andExpect(jsonPath("$.periodoIva").value("2099-01-01"))
                .andExpect(jsonPath("$.montoDepositado").value(814.03))
                .andExpect(jsonPath("$.codigoCuentaBanco").value(BANCO))
                .andExpect(jsonPath("$.nombreCuentaBanco").isString())
                .andExpect(jsonPath("$.contabilizada").value(false)), "$.idLiquidacion");
        enviar(post(LIQUIDACIONES), liquidacion(idBanco, numero + "B", codigo, "2099-01-16", "2099-01-01", "10.00", "0", "0", "10.00", BANCO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigoGeneracion").value("Ya está registrado en la liquidación " + numero + "."));

        leer(LIQUIDACIONES, "desde", "2099-01-01", "hasta", "2099-01-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].idLiquidacion").value(id));
        enviar(put(LIQUIDACIONES + "/" + id), liquidacion(idBanco, numero, codigo, "2099-01-15", "2099-01-01", "842.50", "14.91", "20.00", "807.59", BANCO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comisionTotal").value(20.00))
                .andExpect(jsonPath("$.montoDepositado").value(807.59));
        assertEquals(1L, jdbc.sql("""
                        select count(*) from tb_bitacora
                         where modulo = 'COMPRAS' and accion = 'EDITAR' and entidad = 'LIQUIDACION_TARJETA' and entidad_id = :n
                        """).param("n", numero).query(Long.class).single());

        List<Integer> ids = valor(enviar(post("/api/contabilidad/contabilizar/TARJETAS"), "{ \"desde\": \"2099-01-01\", \"hasta\": \"2099-01-31\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids", hasSize(1))), "$.ids");
        leer("/api/contabilidad/partidas/" + ids.get(0))
                .andExpect(jsonPath("$.origen").value("TARJETAS"))
                .andExpect(jsonPath("$.concepto").value("Liquidación " + numero))
                .andExpect(jsonPath("$.lineas[?(@.codigoCuenta == '" + BANCO + "')].debe").value(contains(807.59)))
                .andExpect(jsonPath("$.lineas[?(@.codigoCuenta == '110409')].debe").value(contains(14.91)))
                .andExpect(jsonPath("$.lineas[?(@.codigoCuenta == '110101')].haber").value(contains(842.50)));
        leer(LIQUIDACIONES, "desde", "2099-01-01", "hasta", "2099-01-31").andExpect(jsonPath("$.contenido[0].contabilizada").value(true));

        String yaContabilizada = "La liquidación " + numero + " ya está contabilizada: borre el borrador o anule la partida primero";
        sinDejarRastro(put(LIQUIDACIONES + "/" + id), liquidacion(idBanco, numero, codigo, "2099-01-15", "2099-01-01", "842.50", "14.91", "13.56", "814.03", BANCO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(yaContabilizada));
        sinDejarRastro(delete(LIQUIDACIONES + "/" + id), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(yaContabilizada));

        enviar(delete("/api/contabilidad/partidas/" + ids.get(0)), "").andExpect(status().isNoContent());
        enviar(delete(LIQUIDACIONES + "/" + id), "").andExpect(status().isNoContent());
        leer(LIQUIDACIONES, "desde", "2099-01-01", "hasta", "2099-01-31").andExpect(jsonPath("$.contenido", hasSize(0)));
        enviar(put(LIQUIDACIONES + "/0"), liquidacion(idBanco, numero, null, "2099-01-15", "2099-01-01", "1.00", "0", "0", "1.00", BANCO))
                .andExpect(status().isNotFound());
    }

    @Test
    void impuestosDelMes() throws Exception {
        ejercicio2099();
        enviar(post("/api/contabilidad/impuestos/2099/13"), "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.mes").value("El mes va de 1 a 12."));
        sinDejarRastro(post("/api/contabilidad/impuestos/2099/1"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El mes 1/2099 no tiene IVA ni ingresos que liquidar"));

        int idBanco = banco();
        enviar(post(LIQUIDACIONES), liquidacion(idBanco, "LIQ-I" + sufijo, null, "2099-01-15", "2099-01-01", "842.50", "14.91", "13.56", "814.03", BANCO))
                .andExpect(status().isCreated());
        int idPartida = valor(enviar(post("/api/contabilidad/impuestos/2099/1"), "")
                .andExpect(status().isCreated()), "$.idPartida");
        leer("/api/contabilidad/partidas/" + idPartida)
                .andDo(print())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.origen").value("LIQUIDACION_IVA"))
                .andExpect(jsonPath("$.fecha").value("2099-01-31"))
                .andExpect(jsonPath("$.concepto").value("Liquidación de IVA y pago a cuenta de 01/2099"))
                .andExpect(jsonPath("$.lineas[?(@.concepto == 'Anticipo a cuenta 2 %% (casilla 161)')].haber").value(contains(14.91)))
                .andExpect(jsonPath("$.lineas[?(@.concepto == 'Remanente para el mes siguiente')].debe").value(contains(14.91)));
        sinDejarRastro(post("/api/contabilidad/impuestos/2099/1"), "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El mes 1/2099 ya tiene su partida de impuestos (bórrela si está en borrador, o anúlela)"));
        assertEquals(1L, jdbc.sql("""
                        select count(*) from tb_bitacora
                         where modulo = 'CONTABILIDAD' and accion = 'GENERAR_IMPUESTOS' and entidad_id = '01/2099'
                        """).query(Long.class).single());
    }

    @Test
    void resumenDelF07() throws Exception {
        String periodo = YearMonth.from(Fechas.hoy()).toString();
        String ruta = "/api/libros-iva/" + periodo + "/resumen-f07";
        ResultActions antes = leer(ruta)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debitoCcf").isNumber())
                .andExpect(jsonPath("$.debitoCf").isNumber())
                .andExpect(jsonPath("$.ivaPercibido").isNumber())
                .andExpect(jsonPath("$.ivaRetenido").isNumber())
                .andExpect(jsonPath("$.remanenteAnterior").isNumber())
                .andExpect(jsonPath("$.ingresosBrutos").isNumber())
                .andExpect(jsonPath("$.pagoACuenta").isNumber());
        BigDecimal anticipo = monto(antes, "$.anticipoIva");
        BigDecimal credito = monto(antes, "$.creditoFiscal");

        int idBanco = banco();
        String hoy = Fechas.hoy().toString();
        enviar(post(LIQUIDACIONES), liquidacion(idBanco, "LIQ-F" + sufijo, null, hoy, periodo + "-01", "842.50", "14.91", "13.56", "814.03", BANCO))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/compras").with(authentication(sesionAdmin)).contentType(MediaType.APPLICATION_JSON).content("""
                { "idProveedor": %d, "fechaCompra": "%s", "tipoDocumentoProveedor": "CCF", "numeroDocumentoProveedor": "DTE-03-F07-%s",
                  "claseDocumento": 4, "codigoGeneracionProveedor": "0F070000-0000-4000-8000-0000000%05d", "idCompraRelacionada": null,
                  "periodoIva": "%s-01", "comprasExentas": 0, "percepcion": false, "tipoOperacionRenta": 1, "clasificacionRenta": 2,
                  "sectorRenta": 4, "tipoCostoGasto": 2, "codigoCuentaExentas": null,
                  "detalle": [ { "descripcion": "Comisión del banco", "codigoCuenta": "51010201", "cantidad": 1, "costoUnitario": 100.00 } ] }
                """.formatted(idBanco, hoy, sufijo, Long.parseLong(sufijo), periodo))).andExpect(status().isCreated());
        em.clear();

        ResultActions despues = leer(ruta).andExpect(status().isOk());
        assertEquals(anticipo.add(new BigDecimal("14.91")), monto(despues, "$.anticipoIva"));
        assertEquals(credito.add(new BigDecimal("13.00")), monto(despues, "$.creditoFiscal"));

        leer("/api/libros-iva/2026-13/resumen-f07")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.periodo").value("Escribe el mes como AAAA-MM (por ejemplo 2026-11)."));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionNoEntra() throws Exception {
        mvc.perform(get(LIQUIDACIONES)).andExpect(status().isForbidden());
        mvc.perform(get("/api/libros-iva/2026-11/resumen-f07")).andExpect(status().isForbidden());
        mvc.perform(post("/api/contabilidad/impuestos/2026/11")).andExpect(status().isForbidden());
    }
}
