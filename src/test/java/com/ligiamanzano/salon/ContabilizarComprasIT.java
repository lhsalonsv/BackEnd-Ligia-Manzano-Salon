package com.ligiamanzano.salon;

import com.jayway.jsonpath.JsonPath;
import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.compras.dto.CompraDetalleDTO;
import com.ligiamanzano.salon.compras.dto.CompraRequest;
import com.ligiamanzano.salon.compras.service.CompraService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ContabilizarComprasIT {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CompraService compras;
    @Autowired JsonMapper json;
    @Autowired EntityManager em;

    String sufijo;
    String hoy;
    String periodoIva;
    UsuarioAutenticado admin;
    Authentication sesionAdmin;
    Authentication sesionContador;

    @BeforeEach
    void preparar() {
        sufijo = String.valueOf(System.nanoTime() % 100000);
        hoy = Fechas.hoy().toString();
        periodoIva = Fechas.hoy().withDayOfMonth(1).toString();
        admin = usuario("prueba.cta" + sufijo, Rol.ADMINISTRADOR);
        sesionAdmin = sesion(admin);
        sesionContador = sesion(usuario("prueba.ctc" + sufijo, Rol.CONTADOR));
    }

    UsuarioAutenticado usuario(String nombre, Rol rol) {
        UsuarioEntity u = new UsuarioEntity();
        u.setNombreUsuario(nombre);
        u.setNombreCompleto("Prueba Cuentas");
        u.setContrasenaHash("sin-clave");
        u.setIdRol(rol.getId());
        u.setActivo(true);
        u = usuarios.save(u);
        return new UsuarioAutenticado(u.getIdUsuario(), u.getNombreUsuario(), u.getNombreCompleto(), rol, false, UUID.randomUUID());
    }

    static Authentication sesion(UsuarioAutenticado u) {
        return UsernamePasswordAuthenticationToken.authenticated(u, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.rol().name())));
    }

    ResultActions enviar(Authentication quien, MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        ResultActions r = mvc.perform(peticion.with(authentication(quien)).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
        em.clear();
        return r;
    }

    ResultActions leer(Authentication quien, String ruta) throws Exception {
        return mvc.perform(get(ruta).with(authentication(quien)));
    }

    static <T> T valor(ResultActions r, String ruta) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), ruta);
    }

    int proveedor(String nombre, int n) throws Exception {
        String nit = "%014d".formatted(Long.parseLong(sufijo) * 10 + n);
        return valor(enviar(sesionAdmin, post("/api/proveedores"), """
                { "nombreComercial": "%s %s", "razonSocial": "%s, S.A. de C.V.", "nit": "%s", "nrc": "%s",
                  "nacionalidad": "El Salvador", "contactoNombre": null, "telefono": null, "correo": null,
                  "direccion": null, "diasCredito": 0, "notas": null }
                """.formatted(nombre, sufijo, nombre, nit, n + sufijo)).andExpect(status().isCreated()), "$.idProveedor");
    }

    String gasto(int idProveedor, String exentas, String cuentaExentas, String lineas) {
        return """
                { "idProveedor": %d, "fechaCompra": "%s", "tipoDocumentoProveedor": "OTRO", "numeroDocumentoProveedor": null,
                  "claseDocumento": 1, "codigoGeneracionProveedor": null, "idCompraRelacionada": null, "periodoIva": "%s",
                  "comprasExentas": %s, "percepcion": false, "tipoOperacionRenta": 1, "clasificacionRenta": 2, "sectorRenta": 4,
                  "tipoCostoGasto": 2, "detalle": [ %s ], "codigoCuentaExentas": %s }
                """.formatted(idProveedor, hoy, periodoIva, exentas, lineas, cuentaExentas == null ? "null" : "\"" + cuentaExentas + "\"");
    }

    static String linea(String descripcion, String cuenta, String costo) {
        return "{ \"descripcion\": \"%s\", \"codigoCuenta\": %s, \"cantidad\": 1, \"costoUnitario\": %s }"
                .formatted(descripcion, cuenta == null ? "null" : "\"" + cuenta + "\"", costo);
    }

    @Test
    void cadaGastoLlevaSuCuenta() throws Exception {
        int idProv = proveedor("Papelería Cuentas", 1);
        int idOtro = proveedor("Sin Cuentas", 2);

        enviar(sesionAdmin, post("/api/compras"), gasto(idProv, "0", null, linea("Papel", null, "4.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("La línea 1: elige la cuenta contable del gasto."));
        enviar(sesionAdmin, post("/api/compras"), gasto(idProv, "0", null, linea("Papel", "5101", "4.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("La línea 1: la cuenta 5101 no acepta movimiento."));
        enviar(sesionAdmin, post("/api/compras"), gasto(idProv, "0", null, linea("Papel", "99999999", "4.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("La línea 1: no existe la cuenta 99999999."));
        enviar(sesionAdmin, post("/api/compras"), gasto(idProv, "2.00", null, linea("Papel", "51010201", "4.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigoCuentaExentas").value("Elige la cuenta contable de las compras exentas."));

        ResultActions r = enviar(sesionAdmin, post("/api/compras"),
                gasto(idProv, "2.00", "51010205", linea("Papel", "51010201", "4.00")))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(6.00))
                .andExpect(jsonPath("$.detalle[0].codigoCuenta").value("51010201"))
                .andExpect(jsonPath("$.codigoCuentaExentas").value("51010205"));
        int idDetalle = valor(r, "$.detalle[0].idDetalleCompra");
        assertEquals("51010201", jdbc.sql("select codigo_cuenta from tb_detalle_compras where id_detalle_compra = :id")
                .param("id", idDetalle).query(String.class).single());

        enviar(sesionAdmin, post("/api/compras"), gasto(idProv, "0", "51010205", linea("Luz", "51010204", "9.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoCuentaExentas").value(nullValue()));

        leer(sesionContador, "/api/proveedores/" + idProv + "/ultima-cuenta")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoCuenta").value("51010204"));
        leer(sesionAdmin, "/api/proveedores/" + idOtro + "/ultima-cuenta")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoCuenta").value(nullValue()));
        leer(sesionContador, "/api/proveedores/0/ultima-cuenta").andExpect(status().isNotFound());
    }

    @Test
    void recepcionRegistraSinCuentaYLaContadoraLaCompleta() throws Exception {
        assumeTrue(jdbc.sql("select count(*) from tb_ejercicios where anio = 2099").query(Long.class).single() == 0,
                "Ya existe el ejercicio 2099 en development");
        int idProv = proveedor("Recepción Cuentas", 3);
        CompraRequest pedido = json.readValue(
                gasto(idProv, "1.50", null, linea("Agua", null, "3.00") + ", " + linea("Jabón", null, "2.00")), CompraRequest.class);
        UsuarioAutenticado recepcion = new UsuarioAutenticado(admin.idUsuario(), admin.nombreUsuario(), admin.nombreCompleto(),
                Rol.RECEPCIONISTA, false, UUID.randomUUID());
        CompraDetalleDTO registrada;
        try {
            SecurityContextHolder.getContext().setAuthentication(sesion(recepcion));
            registrada = compras.registrar(pedido);
        } finally {
            SecurityContextHolder.clearContext();
        }
        em.flush();
        em.clear();
        int idCompra = registrada.idCompra();
        int idAgua = registrada.detalle().get(0).idDetalleCompra();
        int idJabon = registrada.detalle().get(1).idDetalleCompra();
        assertNull(registrada.detalle().get(0).codigoCuenta());
        assertNull(registrada.codigoCuentaExentas());

        String ruta = "/api/compras/" + idCompra + "/cuentas";
        enviar(sesionContador, patch(ruta), """
                { "detalle": [ { "idDetalleCompra": %d, "codigoCuenta": "51010205" } ], "codigoCuentaExentas": "51010205" }
                """.formatted(idAgua))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("Falta la cuenta contable de la línea \"Jabón\"."));
        enviar(sesionContador, patch(ruta), """
                { "detalle": [ { "idDetalleCompra": %d, "codigoCuenta": "51010205" }, { "idDetalleCompra": %d, "codigoCuenta": "5101" } ],
                  "codigoCuentaExentas": null }
                """.formatted(idAgua, idJabon))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("La línea \"Jabón\": la cuenta 5101 no acepta movimiento."))
                .andExpect(jsonPath("$.campos.codigoCuentaExentas").value("Elige la cuenta contable de las compras exentas."));
        enviar(sesionContador, patch(ruta), """
                { "detalle": [ { "idDetalleCompra": 0, "codigoCuenta": "51010205" } ], "codigoCuentaExentas": "51010205" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("Una de las líneas no es un gasto de la compra " + registrada.numeroCompra() + "."));

        enviar(sesionContador, patch(ruta), """
                { "detalle": [ { "idDetalleCompra": %d, "codigoCuenta": "51010205" }, { "idDetalleCompra": %d, "codigoCuenta": "51010204" } ],
                  "codigoCuentaExentas": "51010201" }
                """.formatted(idAgua, idJabon))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalle[0].codigoCuenta").value("51010205"))
                .andExpect(jsonPath("$.detalle[1].codigoCuenta").value("51010204"))
                .andExpect(jsonPath("$.codigoCuentaExentas").value("51010201"));
        assertEquals(1L, jdbc.sql("""
                        select count(*) from tb_bitacora
                         where modulo = 'COMPRAS' and accion = 'EDITAR' and entidad = 'COMPRA' and entidad_id = :numero
                        """).param("numero", registrada.numeroCompra()).query(Long.class).single());

        enviar(sesionContador, post("/api/contabilidad/ejercicios"), "{ \"anio\": 2099 }").andExpect(status().isCreated());
        int idPartida = valor(enviar(sesionContador, post("/api/contabilidad/partidas"), """
                { "fecha": "2099-01-10", "codigoTipo": "PDA", "concepto": "Borrador de prueba", "lineas": [
                  { "codigoCuenta": "110101", "concepto": null, "debe": 1.00, "haber": 0 },
                  { "codigoCuenta": "61010107", "concepto": null, "debe": 0, "haber": 1.00 } ] }
                """).andExpect(status().isCreated()), "$.idPartida");
        jdbc.sql("insert into tb_partida_documentos (id_partida, tipo_documento, id_documento) values (:p, 'COMPRA', :c)")
                .param("p", idPartida).param("c", idCompra).update();
        enviar(sesionContador, patch(ruta), "{ \"detalle\": [], \"codigoCuentaExentas\": \"51010201\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_DE_LA_BASE"))
                .andExpect(jsonPath("$.mensaje").value("La compra " + registrada.numeroCompra()
                        + " ya está contabilizada: borre el borrador o anule la partida primero."));
        enviar(sesionContador, delete("/api/contabilidad/partidas/" + idPartida), "").andExpect(status().isNoContent());
        enviar(sesionContador, patch(ruta), "{ \"detalle\": [], \"codigoCuentaExentas\": \"51010205\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoCuentaExentas").value("51010205"));
    }

    @Test
    void configuracionConLasCuentasNuevas() throws Exception {
        leer(sesionContador, "/api/contabilidad/configuracion")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ctaCaja").value("110101"))
                .andExpect(jsonPath("$.ctaIvaCredito").value("110401"));

        String config = """
                { "ctaUtilidadEjercicio": null, "ctaUtilidadesAnteriores": null, "ctaPerdidaEjercicio": null, "ctaPerdidasAnteriores": null,
                  "ctaPerdidasGanancias": null, "ctaReservaLegal": null, "ctaClientes": null, "ctaProveedores": null,
                  "tipoPartidaVentas": "ING", "tipoPartidaCompras": "COM", "modoVentas": "DETALLE", "modoCompras": "DETALLE",
                  "fechaInicio": "2026-12-01", "ctaCaja": "110101", "ctaIngresoVentas": "61010107", "ctaComprasProductos": null,
                  "ctaIvaDebitoCcf": "210701", "ctaIvaDebitoCf": "210702", "ctaIvaCredito": "110401", "ctaIvaPercibido": null,
                  "ctaIvaRetenido": null, "ctaAnticipoIva": null, "ctaRemanenteIva": null, "ctaIvaPorPagar": null, "ctaPagoCuenta": null,
                  "ctaPagoCuentaPorPagar": null, "ctaSueldos": null, "ctaComisiones": null, "ctaIsssLaboral": null, "ctaAfpLaboral": null,
                  "ctaRentaRetenida": null, "ctaOtrosDescuentos": null, "ctaSalariosPorPagar": null,
                  "ctaCapitalSocial": "3101", "ctaGastoIsr": "53" }
                """;
        enviar(sesionContador, put("/api/contabilidad/configuracion"), config.replace("\"110101\"", "\"1101\"").replace("\"53\"", "\"5399\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.ctaCaja").value("La cuenta 1101 no acepta movimiento."))
                .andExpect(jsonPath("$.campos.ctaGastoIsr").value("No existe esa cuenta."));
        enviar(sesionContador, put("/api/contabilidad/configuracion"), config)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaInicio").value("2026-12-01"))
                .andExpect(jsonPath("$.ctaCapitalSocial").value("3101"))
                .andExpect(jsonPath("$.ctaGastoIsr").value("53"))
                .andExpect(jsonPath("$.ctaSueldos").value(nullValue()));
        assertEquals("53", jdbc.sql("select cta_gasto_isr from tb_config_contable where id = 1").query(String.class).single());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionNoCambiaCuentas() throws Exception {
        mvc.perform(patch("/api/compras/1/cuentas").contentType(MediaType.APPLICATION_JSON).content("{ \"detalle\": [] }"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/proveedores/1/ultima-cuenta")).andExpect(status().isForbidden());
    }
}
