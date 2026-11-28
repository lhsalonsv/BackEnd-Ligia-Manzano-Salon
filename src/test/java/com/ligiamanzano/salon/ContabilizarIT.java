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
class ContabilizarIT {

    static final String RUTA = "/api/contabilidad/contabilizar/COMPRAS";

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
        sesionAdmin = sesion("prueba.cza" + sufijo, Rol.ADMINISTRADOR);
        sesionContador = sesion("prueba.czc" + sufijo, Rol.CONTADOR);
    }

    Authentication sesion(String nombre, Rol rol) {
        UsuarioEntity u = new UsuarioEntity();
        u.setNombreUsuario(nombre);
        u.setNombreCompleto("Prueba Contabilizar");
        u.setContrasenaHash("sin-clave");
        u.setIdRol(rol.getId());
        u.setActivo(true);
        u = usuarios.save(u);
        return UsernamePasswordAuthenticationToken.authenticated(
                new UsuarioAutenticado(u.getIdUsuario(), u.getNombreUsuario(), u.getNombreCompleto(), rol, false, UUID.randomUUID()),
                null, List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
    }

    ResultActions enviar(Authentication quien, MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        ResultActions r = mvc.perform(peticion.with(authentication(quien)).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
        em.clear();
        return r;
    }

    ResultActions sinDejarRastro(Authentication quien, MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        em.flush();
        jdbc.sql("savepoint antes_del_error").update();
        ResultActions r = enviar(quien, peticion, cuerpo);
        jdbc.sql("rollback to savepoint antes_del_error").update();
        em.clear();
        return r;
    }

    ResultActions vistaPrevia(String desde, String hasta) throws Exception {
        return mvc.perform(get(RUTA).param("desde", desde).param("hasta", hasta).with(authentication(sesionContador)));
    }

    static <T> T valor(ResultActions r, String ruta) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), ruta);
    }

    int gasto(int idProveedor, String descripcion, String costo, String fecha) throws Exception {
        String hoy = Fechas.hoy().toString();
        int id = valor(enviar(sesionAdmin, post("/api/compras"), """
                { "idProveedor": %d, "fechaCompra": "%s", "tipoDocumentoProveedor": "OTRO", "numeroDocumentoProveedor": null,
                  "claseDocumento": 1, "codigoGeneracionProveedor": null, "idCompraRelacionada": null, "periodoIva": "%s",
                  "comprasExentas": 0, "percepcion": false, "tipoOperacionRenta": 1, "clasificacionRenta": 2, "sectorRenta": 4,
                  "tipoCostoGasto": 2, "codigoCuentaExentas": null,
                  "detalle": [ { "descripcion": "%s", "codigoCuenta": "51010201", "cantidad": 1, "costoUnitario": %s } ] }
                """.formatted(idProveedor, hoy, hoy.substring(0, 8) + "01", descripcion, costo)).andExpect(status().isCreated()), "$.idCompra");
        jdbc.sql("update tb_compras set fecha_compra = cast(:fecha as date), periodo_iva = date_trunc('month', cast(:fecha as date)) where id_compra = :id")
                .param("fecha", fecha).param("id", id).update();
        return id;
    }

    String numero(int idCompra) {
        return jdbc.sql("select numero_compra from tb_compras where id_compra = :id").param("id", idCompra).query(String.class).single();
    }

    long contabilizaciones() {
        return jdbc.sql("select count(*) from tb_bitacora where modulo = 'CONTABILIDAD' and accion = 'CONTABILIZAR' and descripcion like '%/2099%'")
                .query(Long.class).single();
    }

    @Test
    void contabilizarLasComprasEnBorrador() throws Exception {
        assumeTrue(jdbc.sql("select count(*) from tb_ejercicios where anio = 2099").query(Long.class).single() == 0,
                "Ya existe el ejercicio 2099 en development");
        enviar(sesionContador, post("/api/contabilidad/ejercicios"), "{ \"anio\": 2099 }").andExpect(status().isCreated());
        int idProv = valor(enviar(sesionAdmin, post("/api/proveedores"), """
                { "nombreComercial": "Contabilizar %s", "razonSocial": "Contabilizar, S.A. de C.V.", "nit": "%014d", "nrc": "8%s",
                  "nacionalidad": "El Salvador", "contactoNombre": null, "telefono": null, "correo": null,
                  "direccion": null, "diasCredito": 0, "notas": null }
                """.formatted(sufijo, Long.parseLong(sufijo), sufijo)).andExpect(status().isCreated()), "$.idProveedor");
        int c1 = gasto(idProv, "Papel", "4.00", "2099-01-10");
        int c2 = gasto(idProv, "Tinta", "6.50", "2099-01-11");
        int c3 = gasto(idProv, "Agua", "3.00", "2099-01-12");
        jdbc.sql("update tb_detalle_compras set codigo_cuenta = null where id_compra = :id").param("id", c3).update();
        String faltaC3 = "Falta la cuenta contable de la línea \"Agua\" de la compra " + numero(c3);

        jdbc.sql("update tb_config_contable set fecha_inicio = null where id = 1").update();
        sinDejarRastro(sesionContador, post(RUTA), "{ \"desde\": \"2099-01-01\", \"hasta\": \"2099-01-31\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_DE_LA_BASE"))
                .andExpect(jsonPath("$.mensaje").value("Configure en Contabilidad > Parámetros la fecha de inicio de la contabilidad"));
        jdbc.sql("""
                update tb_config_contable
                   set fecha_inicio = date '2099-01-01', modo_compras = 'DETALLE', tipo_partida_compras = 'COM', cta_proveedores = '21020101'
                 where id = 1
                """).update();

        vistaPrevia("2099-01-01", "2099-01-31")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas", hasSize(6)))
                .andExpect(jsonPath("$.lineas[0].grupo").value("2099-01-10"))
                .andExpect(jsonPath("$.lineas[0].fecha").value("2099-01-10"))
                .andExpect(jsonPath("$.lineas[0].tipoDocumento").value("COMPRA"))
                .andExpect(jsonPath("$.lineas[0].idDocumento").value(c1))
                .andExpect(jsonPath("$.lineas[0].documento").value(numero(c1) + " OTRO"))
                .andExpect(jsonPath("$.lineas[0].codigoCuenta").value("51010201"))
                .andExpect(jsonPath("$.lineas[0].nombreCuenta").value("Papelería y Útiles"))
                .andExpect(jsonPath("$.lineas[0].falta").value(nullValue()))
                .andExpect(jsonPath("$.lineas[0].monto").value(4.00))
                .andExpect(jsonPath("$.lineas[1].codigoCuenta").value("21020101"))
                .andExpect(jsonPath("$.lineas[1].monto").value(-4.00))
                .andExpect(jsonPath("$.lineas[4].codigoCuenta").value(nullValue()))
                .andExpect(jsonPath("$.lineas[4].falta").value(faltaC3));

        sinDejarRastro(sesionContador, post(RUTA), "{ \"desde\": \"2099-01-01\", \"hasta\": \"2099-01-31\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(faltaC3));

        List<Integer> ids = valor(enviar(sesionContador, post(RUTA), "{ \"desde\": \"2099-01-10\", \"hasta\": \"2099-01-11\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids", hasSize(2))), "$.ids");
        mvc.perform(get("/api/contabilidad/partidas/" + ids.get(0)).with(authentication(sesionContador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.origen").value("COMPRAS"))
                .andExpect(jsonPath("$.fecha").value("2099-01-10"))
                .andExpect(jsonPath("$.concepto").value("Compras del 10/01/2099 (1 documento)"))
                .andExpect(jsonPath("$.lineas[0].codigoCuenta").value("51010201"))
                .andExpect(jsonPath("$.lineas[0].concepto").value(numero(c1) + " OTRO"))
                .andExpect(jsonPath("$.lineas[0].debe").value(4.00))
                .andExpect(jsonPath("$.lineas[1].haber").value(4.00));
        assertEquals(1L, contabilizaciones());

        vistaPrevia("2099-01-01", "2099-01-31")
                .andExpect(jsonPath("$.lineas", hasSize(2)))
                .andExpect(jsonPath("$.lineas[*].idDocumento", everyItem(is(c3))));
        enviar(sesionContador, post(RUTA), "{ \"desde\": \"2099-01-10\", \"hasta\": \"2099-01-11\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids", hasSize(0)));
        assertEquals(1L, contabilizaciones());

        enviar(sesionContador, patch("/api/compras/" + c1 + "/cuentas"), "{ \"detalle\": [], \"codigoCuentaExentas\": null }")
                .andExpect(status().isConflict());
        enviar(sesionContador, delete("/api/contabilidad/partidas/" + ids.get(0)), "").andExpect(status().isNoContent());
        vistaPrevia("2099-01-01", "2099-01-31")
                .andExpect(jsonPath("$.lineas", hasSize(4)))
                .andExpect(jsonPath("$.lineas[0].idDocumento").value(c1));
    }

    @Test
    void revisaElOrigenYLasFechas() throws Exception {
        mvc.perform(get("/api/contabilidad/contabilizar/INVENTARIO").param("desde", "2099-01-01").param("hasta", "2099-01-31")
                        .with(authentication(sesionContador)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.origen").value("Elige VENTAS, COMPRAS, PLANILLA o TARJETAS."));
        vistaPrevia("2099-01-31", "2099-01-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.hasta").value("Tiene que ser igual o después de la fecha inicial."));
        enviar(sesionContador, post(RUTA), "{ \"desde\": null, \"hasta\": \"2099-01-31\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.desde").value("Escribe la fecha inicial"));
        mvc.perform(get("/api/contabilidad/contabilizar/ventas").param("desde", "2099-01-01").param("hasta", "2099-01-31")
                        .with(authentication(sesionContador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionNoContabiliza() throws Exception {
        mvc.perform(get(RUTA).param("desde", "2099-01-01").param("hasta", "2099-01-31")).andExpect(status().isForbidden());
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{ \"desde\": \"2099-01-01\", \"hasta\": \"2099-01-31\" }"))
                .andExpect(status().isForbidden());
    }
}
