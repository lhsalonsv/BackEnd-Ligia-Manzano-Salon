package com.ligiamanzano.salon;

import com.jayway.jsonpath.JsonPath;
import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.inventario.entity.CategoriaEntity;
import com.ligiamanzano.salon.inventario.repository.CategoriaRepository;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PruebaSprint2IT {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired CategoriaRepository categorias;
    @Autowired EntityManager em;

    Authentication sesionAdmin;
    int idCategoria;
    String sufijo;
    String hoy;
    String mes;
    String periodoIva;

    @BeforeEach
    void preparar() {
        sufijo = String.valueOf(System.nanoTime() % 100000);
        hoy = Fechas.hoy().toString();
        mes = hoy.substring(0, 7);
        periodoIva = Fechas.hoy().withDayOfMonth(1).toString();
        UsuarioEntity admin = new UsuarioEntity();
        admin.setNombreUsuario("prueba.inv" + sufijo);
        admin.setNombreCompleto("Prueba Inventario");
        admin.setContrasenaHash("sin-clave");
        admin.setIdRol(Rol.ADMINISTRADOR.getId());
        admin.setActivo(true);
        admin = usuarios.save(admin);
        UsuarioAutenticado u = new UsuarioAutenticado(admin.getIdUsuario(), admin.getNombreUsuario(),
                admin.getNombreCompleto(), Rol.ADMINISTRADOR, false, UUID.randomUUID());
        sesionAdmin = UsernamePasswordAuthenticationToken.authenticated(u, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")));

        CategoriaEntity c = new CategoriaEntity();
        c.setNombreCategoria("Cabello " + sufijo);
        idCategoria = categorias.save(c).getIdCategoria();
    }

    RequestPostProcessor comoAdmin() {
        return authentication(sesionAdmin);
    }

    ResultActions enviar(MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        return mvc.perform(peticion.with(comoAdmin()).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    ResultActions leer(String ruta, String... parametros) throws Exception {
        MockHttpServletRequestBuilder peticion = get(ruta).with(comoAdmin());
        for (int i = 0; i < parametros.length; i += 2) {
            peticion.param(parametros[i], parametros[i + 1]);
        }
        return mvc.perform(peticion);
    }

    <T> T valor(ResultActions r, String ruta) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), ruta);
    }

    String producto(String nombre, String codigo, int idMarca, String precio, String comision, int minimo) {
        return """
                { "nombreProducto": "%s", "codigo": "%s", "descripcion": null, "idMarca": %d, "idCategoria": %d,
                  "codUnidadMedida": 99, "precioVenta": %s, "comisionFija": %s, "stockMinimo": %d, "activo": true }
                """.formatted(nombre, codigo, idMarca, idCategoria, precio, comision, minimo);
    }

    String proveedor(String nombre, String nit, String nrc, int diasCredito) {
        return """
                { "nombreComercial": "%s", "razonSocial": "%s, S.A. de C.V.", "nit": "%s", "nrc": "%s",
                  "nacionalidad": "Guatemala", "contactoNombre": null, "telefono": null, "correo": "Ventas@Ejemplo.com",
                  "direccion": null, "diasCredito": %d, "notas": null }
                """.formatted(nombre, nombre, nit, nrc, diasCredito);
    }

    String compra(int idProveedor, String tipo, String numero, String codigo, Integer idRelacionada, String lineas) {
        return """
                { "idProveedor": %d, "fechaCompra": "%s", "tipoDocumentoProveedor": "%s",
                  "numeroDocumentoProveedor": %s, "claseDocumento": 4, "codigoGeneracionProveedor": %s,
                  "idCompraRelacionada": %s, "periodoIva": "%s", "comprasExentas": 0, "percepcion": false,
                  "tipoOperacionRenta": 1, "clasificacionRenta": 1, "sectorRenta": 4, "tipoCostoGasto": 5,
                  "detalle": [ %s ] }
                """.formatted(idProveedor, hoy, tipo, texto(numero), texto(codigo), idRelacionada, periodoIva, lineas);
    }

    static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    static String codigo(String sufijo, int n) {
        return "0A1B2C3D-0000-4000-8000-%07d%05d".formatted(Integer.parseInt(sufijo), n);
    }

    ResultActions sinDejarRastro(MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        em.flush();
        jdbc.sql("savepoint antes_del_error").update();
        ResultActions r = enviar(peticion, cuerpo);
        jdbc.sql("rollback to savepoint antes_del_error").update();
        em.clear();
        return r;
    }

    int crearMarca() throws Exception {
        return valor(enviar(post("/api/marcas"), "{ \"nombreMarca\": \"Aurora Prueba " + sufijo + "\" }")
                .andExpect(status().isCreated()), "$.idMarca");
    }

    @Test
    void inventarioYComprasConPeps() throws Exception {
        int idMarca = crearMarca();
        enviar(post("/api/marcas"), "{ \"nombreMarca\": \"ÁURORA prueba " + sufijo + "\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MARCA_DUPLICADA"))
                .andExpect(jsonPath("$.campos.nombreMarca").exists())
                .andExpect(jsonPath("$.idMarcaExistente").value(idMarca));

        int idA = valor(enviar(post("/api/productos"), producto("Shampoo hidratante prueba", "SH-" + sufijo, idMarca, "13.00", "2.00", 6))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.existencia").value(0))
                .andExpect(jsonPath("$.costoPeps").value(nullValue()))
                .andExpect(jsonPath("$.ultimoCosto").value(nullValue()))
                .andExpect(jsonPath("$.lotes", hasSize(0)))
                .andExpect(jsonPath("$.nombreMarca").value("Aurora Prueba " + sufijo)), "$.idProducto");
        int idB = valor(enviar(post("/api/productos"), producto("Tinte rubio prueba", "TR-" + sufijo, idMarca, "9.50", "1.00", 10))
                .andExpect(status().isCreated()), "$.idProducto");

        enviar(post("/api/productos"), producto("Otro", "sh-" + sufijo, idMarca, "13.00", "2.00", 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PRODUCTO_DUPLICADO"))
                .andExpect(jsonPath("$.idProductoExistente").value(idA));
        enviar(post("/api/productos"), producto("Barato", "BA-" + sufijo, idMarca, "2.00", "2.00", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.comisionFija").value("No puede ser mayor que el precio sin IVA ($1.77)."));
        enviar(post("/api/productos"), producto("Sin marca", "SM-" + sufijo, 999999, "5.00", "0.00", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.idMarca").exists());

        enviar(post("/api/productos/" + idA + "/ajustes"), "{ \"tipo\": \"CONTEO\", \"cantidad\": 3, \"motivo\": \"Conteo\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cantidad", containsString("nunca se compró")));
        enviar(post("/api/productos/" + idA + "/ajustes"), "{ \"tipo\": \"DANADO\", \"cantidad\": 1, \"motivo\": \"Roto\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));

        int idProv = valor(enviar(post("/api/proveedores"), proveedor("Distribuidora Prueba", "00000000000" + "123", "123456", 30))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.nacionalidad").value("Guatemala"))
                .andExpect(jsonPath("$.correo").value("ventas@ejemplo.com"))
                .andExpect(jsonPath("$.ultimasCompras", hasSize(0)))
                .andExpect(jsonPath("$.comprasAnio.cantidad").value(0)), "$.idProveedor");
        enviar(post("/api/proveedores"), proveedor("Otra", "00000000000123", "999999", 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PROVEEDOR_DUPLICADO"))
                .andExpect(jsonPath("$.campos.nit").exists())
                .andExpect(jsonPath("$.idProveedorExistente").value(idProv));
        enviar(post("/api/proveedores"), proveedor("Otra", "00000000000999", "123456", 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos.nrc").exists());
        enviar(post("/api/proveedores"), proveedor("Otra", "123", "1", 0).replace("\"Guatemala\"", "\"\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nit").exists())
                .andExpect(jsonPath("$.campos.nrc").exists())
                .andExpect(jsonPath("$.campos.nacionalidad").exists());
        int idInactivo = valor(enviar(post("/api/proveedores"), proveedor("Inactivo Prueba", "00000000000777", "777777", 0))
                .andExpect(status().isCreated()), "$.idProveedor");
        enviar(patch("/api/proveedores/" + idInactivo + "/activo"), "{ \"activo\": false }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        String compra1 = compra(idProv, "CCF", "DTE-03-PRUEBA-" + sufijo, codigo(sufijo, 1), null, """
                { "idProducto": %d, "cantidad": 12, "costoUnitario": 6.50 },
                { "idProducto": %d, "cantidad": 6, "costoUnitario": 4.4567 }""".formatted(idA, idB));
        ResultActions r1 = enviar(post("/api/compras"), compra1)
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroCompra", matchesPattern("^C-\\d{4,}$")))
                .andExpect(jsonPath("$.subtotalSinIva").value(104.74))
                .andExpect(jsonPath("$.iva").value(13.62))
                .andExpect(jsonPath("$.total").value(118.36))
                .andExpect(jsonPath("$.claseDocumento").value(4))
                .andExpect(jsonPath("$.codigoGeneracionProveedor").value(codigo(sufijo, 1)))
                .andExpect(jsonPath("$.periodoIva").value(periodoIva))
                .andExpect(jsonPath("$.tipoCostoGasto").value(5))
                .andExpect(jsonPath("$.fechaVencimientoPago").value(Fechas.hoy().plusDays(30).toString()))
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.nombreUsuario").value("Prueba Inventario"))
                .andExpect(jsonPath("$.detalle[0].subtotal").value(78.00))
                .andExpect(jsonPath("$.detalle[0].cantidadDisponible").value(12))
                .andExpect(jsonPath("$.detalle[1].subtotal").value(26.74));
        int idCompra1 = valor(r1, "$.idCompra");
        String numero1 = valor(r1, "$.numeroCompra");

        enviar(post("/api/compras"), compra1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("COMPRA_DUPLICADA"))
                .andExpect(jsonPath("$.idCompraExistente").value(idCompra1));
        enviar(post("/api/compras"), compra1.replace("DTE-03-PRUEBA-", "DTE-03-OTRO-"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("COMPRA_DUPLICADA"));

        String compra2 = compra(idProv, "FACTURA", "FAC-" + sufijo, null, null,
                "{ \"idProducto\": %d, \"cantidad\": 12, \"costoUnitario\": 6.80 }".formatted(idA));
        enviar(post("/api/compras"), compra2)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.iva").value(0.00))
                .andExpect(jsonPath("$.total").value(81.60));

        enviar(post("/api/compras"), compra2.replace("\"FAC-" + sufijo + "\"", "null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.numeroDocumentoProveedor").exists());
        enviar(post("/api/compras"), compra1.replace("PRUEBA-" + sufijo, "OTRO-" + sufijo).replace(codigo(sufijo, 1), codigo(sufijo, 9))
                        .replace("\"idProducto\": " + idB, "\"idProducto\": " + idA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("La línea 2: ese producto ya está en la línea 1."));
        enviar(post("/api/compras"), compra2.replace("\"cantidad\": 12", "\"cantidad\": 0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("La línea 1: la cantidad tiene que ser mayor que 0."));
        enviar(post("/api/compras"), compra2.replace("\"idProveedor\": " + idProv, "\"idProveedor\": " + idInactivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.idProveedor").value("Ese proveedor está inactivo."));
        enviar(post("/api/compras"), compra2.replace("\"fechaCompra\": \"" + hoy, "\"fechaCompra\": \"" + Fechas.hoy().plusDays(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.fechaCompra").exists());
        enviar(post("/api/compras"), compra1.replace("\"" + codigo(sufijo, 1) + "\"", "null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigoGeneracionProveedor").exists());
        enviar(post("/api/compras"), compra1.replace(periodoIva, Fechas.hoy().withDayOfMonth(1).plusMonths(4).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.periodoIva").exists());

        mvc.perform(get("/api/productos/" + idA).with(comoAdmin()))
                .andDo(print())
                .andExpect(jsonPath("$.existencia").value(24))
                .andExpect(jsonPath("$.costoPeps").value(6.5))
                .andExpect(jsonPath("$.ultimoCosto").value(6.8))
                .andExpect(jsonPath("$.lotes", hasSize(2)))
                .andExpect(jsonPath("$.lotes[0].numeroCompra").value(numero1))
                .andExpect(jsonPath("$.ultimosMovimientos[0].tipo").value("ENTRADA_COMPRA"));

        enviar(post("/api/productos/" + idA + "/ajustes"), "{ \"tipo\": \"DANADO\", \"cantidad\": 13, \"motivo\": \"Caja mojada\" }")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existencia").value(11))
                .andExpect(jsonPath("$.costoPeps").value(6.8))
                .andExpect(jsonPath("$.lotes", hasSize(1)))
                .andExpect(jsonPath("$.lotes[0].cantidadDisponible").value(11))
                .andExpect(jsonPath("$.ultimosMovimientos[0].tipo").value("SALIDA_AJUSTE"))
                .andExpect(jsonPath("$.ultimosMovimientos[0].cantidad").value(-1))
                .andExpect(jsonPath("$.ultimosMovimientos[1].cantidad").value(-12))
                .andExpect(jsonPath("$.ultimosMovimientos[1].motivo").value("Caja mojada"));

        long avisosAntes = avisos(idA);
        enviar(post("/api/productos/" + idA + "/ajustes"), "{ \"tipo\": \"USO_SALON\", \"cantidad\": 6, \"motivo\": \"Lavados\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existencia").value(5));
        assertEquals(avisosAntes + 1, avisos(idA));

        enviar(post("/api/productos/" + idA + "/ajustes"), "{ \"tipo\": \"CONTEO\", \"cantidad\": 8, \"motivo\": \"Conteo de fin de mes\" }")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existencia").value(8))
                .andExpect(jsonPath("$.lotes", hasSize(2)))
                .andExpect(jsonPath("$.lotes[1].numeroCompra").value(nullValue()))
                .andExpect(jsonPath("$.lotes[1].costoUnitario").value(6.8))
                .andExpect(jsonPath("$.ultimosMovimientos[0].tipo").value("ENTRADA_AJUSTE"));
        assertEquals(avisosAntes + 1, avisos(idA));

        enviar(post("/api/productos/" + idA + "/ajustes"), "{ \"tipo\": \"CONTEO\", \"cantidad\": 8, \"motivo\": \"Otra vez\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cantidad").value("Ya hay 8: no hay nada que ajustar."));
        enviar(post("/api/productos/" + idA + "/ajustes"), "{ \"tipo\": \"DANADO\", \"cantidad\": 100, \"motivo\": \"x\" }")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Solo hay 8 en existencia."));
        enviar(post("/api/productos/" + idA + "/ajustes"), "{ \"tipo\": \"DANADO\", \"cantidad\": 0, \"motivo\": \"x\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cantidad").exists());

        leer("/api/compras/" + idCompra1)
                .andExpect(jsonPath("$.detalle[0].cantidadDisponible").value(0))
                .andExpect(jsonPath("$.detalle[1].cantidadDisponible").value(6));

        leer("/api/productos", "buscar", "TINTE RUBIO", "filtro", "bajo-minimo")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.idProducto == " + idB + ")].existencia").value(contains(6)))
                .andExpect(jsonPath("$.conteos['bajo-minimo']").value(1))
                .andExpect(jsonPath("$.conteos.todos").value(1))
                .andExpect(jsonPath("$.comisionProducto").value(2.00))
                .andExpect(jsonPath("$.ivaPct").value(13))
                .andExpect(jsonPath("$.valorTotal").isNumber());
        leer("/api/productos", "idCategoria", String.valueOf(idCategoria))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[0].nombreProducto").value("Shampoo hidratante prueba"))
                .andExpect(jsonPath("$.contenido[0].valorCosto").value(54.40));

        leer("/api/compras", "buscar", "distribuidora prueba", "filtro", "este-mes")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[1].numeroCompra").value(numero1))
                .andExpect(jsonPath("$.contenido[1].productos").value(2))
                .andExpect(jsonPath("$.contenido[1].unidades").value(18))
                .andExpect(jsonPath("$.contenido[1].gastos").value(0))
                .andExpect(jsonPath("$.mes.cantidad", greaterThanOrEqualTo(2)));

        leer("/api/proveedores/" + idProv)
                .andExpect(jsonPath("$.ultimasCompras", hasSize(2)))
                .andExpect(jsonPath("$.ultimasCompras[0].tipoDocumentoProveedor").value("FACTURA"))
                .andExpect(jsonPath("$.comprasAnio.cantidad").value(2))
                .andExpect(jsonPath("$.comprasAnio.total").value(199.96));
        leer("/api/proveedores")
                .andExpect(jsonPath("$[?(@.idProveedor == " + idProv + ")].ultimaCompra.total").value(contains(81.60)))
                .andExpect(jsonPath("$[?(@.idProveedor == " + idProv + ")].nacionalidad").value(contains("Guatemala")));

        leer("/api/bitacora", "fecha", hoy, "buscar", numero1)
                .andExpect(jsonPath("$.contenido[0].modulo").value("COMPRAS"))
                .andExpect(jsonPath("$.contenido[0].accion").value("REGISTRAR"));
        leer("/api/bitacora", "fecha", hoy, "modulo", "INVENTARIO", "buscar", "Ajustó Shampoo hidratante prueba")
                .andExpect(jsonPath("$.contenido", hasSize(3)));
    }

    @Test
    void gastosNotasLibrosRegistroEInventarioFisico() throws Exception {
        int idMarca = crearMarca();
        int idA = valor(enviar(post("/api/productos"), producto("Champú libro prueba", "CL-" + sufijo, idMarca, "12.00", "2.00", 0))
                .andExpect(status().isCreated()), "$.idProducto");
        int idSinCompra = valor(enviar(post("/api/productos"), producto("Nunca comprado prueba", "NC-" + sufijo, idMarca, "5.00", "0.00", 0))
                .andExpect(status().isCreated()), "$.idProducto");
        int idProv = valor(enviar(post("/api/proveedores"), proveedor("Libros Prueba", "00000000000456", "456456", 0))
                .andExpect(status().isCreated()), "$.idProveedor");

        ResultActions rc = enviar(post("/api/compras"), compra(idProv, "CCF", "DTE-03-LIB-" + sufijo, codigo(sufijo, 1), null, """
                { "idProducto": %d, "cantidad": 10, "costoUnitario": 5.00 },
                { "descripcion": "  Bolsas para clientas ", "cantidad": 1, "costoUnitario": 12.00 }""".formatted(idA))
                        .replace("\"comprasExentas\": 0, \"percepcion\": false", "\"comprasExentas\": 3.00, \"percepcion\": true"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotalSinIva").value(62.00))
                .andExpect(jsonPath("$.comprasExentas").value(3.00))
                .andExpect(jsonPath("$.iva").value(8.06))
                .andExpect(jsonPath("$.ivaPercibido").value(0.62))
                .andExpect(jsonPath("$.total").value(73.68))
                .andExpect(jsonPath("$.fechaVencimientoPago").value(nullValue()))
                .andExpect(jsonPath("$.detalle[1].idProducto").value(nullValue()))
                .andExpect(jsonPath("$.detalle[1].descripcion").value("Bolsas para clientas"))
                .andExpect(jsonPath("$.detalle[1].cantidadDisponible").value(nullValue()));
        int idCcf = valor(rc, "$.idCompra");
        String numeroCcf = valor(rc, "$.numeroCompra");

        String nota = compra(idProv, "NC", "DTE-05-LIB-" + sufijo, codigo(sufijo, 2), idCcf,
                "{ \"descripcion\": \"Descuento por pronto pago\", \"cantidad\": 1, \"costoUnitario\": 5.00 }");
        enviar(post("/api/compras"), nota.replace("\"idCompraRelacionada\": " + idCcf, "\"idCompraRelacionada\": null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.idCompraRelacionada").value("Elige la compra que corrige."));
        enviar(post("/api/compras"), nota.replace("\"NC\"", "\"ND\"").replace("{ \"descripcion\": \"Descuento por pronto pago\"", "{ \"idProducto\": " + idA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("La línea 1: una nota de débito no lleva productos."));
        enviar(post("/api/compras"), nota.replace("\"cantidad\": 1", "\"cantidad\": 2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("La línea 1: un gasto lleva cantidad 1."));
        ResultActions rn = enviar(post("/api/compras"), nota)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoDocumentoProveedor").value("NC"))
                .andExpect(jsonPath("$.iva").value(0.65))
                .andExpect(jsonPath("$.total").value(5.65))
                .andExpect(jsonPath("$.idCompraRelacionada").value(idCcf))
                .andExpect(jsonPath("$.numeroCompraRelacionada").value(numeroCcf));
        int idNota = valor(rn, "$.idCompra");

        String devolucion = compra(idProv, "NC", "DTE-05-DEV-" + sufijo, codigo(sufijo, 3), idCcf,
                "{ \"idProducto\": " + idA + ", \"cantidad\": 2, \"costoUnitario\": 5.00 }");
        enviar(post("/api/compras"), devolucion.replace("\"idProducto\": " + idA, "\"idProducto\": " + idSinCompra))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("La línea 1: Nunca comprado prueba no viene en la compra " + numeroCcf + "."));
        enviar(post("/api/compras"), devolucion.replace("\"cantidad\": 2", "\"cantidad\": 11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.detalle").value("La línea 1: Champú libro prueba: la compra " + numeroCcf + " trajo 10, no se pueden devolver 11."));
        enviar(post("/api/compras"), devolucion)
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detalle[0].idProducto").value(idA))
                .andExpect(jsonPath("$.detalle[0].cantidadDisponible").value(8))
                .andExpect(jsonPath("$.total").value(11.30));
        sinDejarRastro(post("/api/compras"), compra(idProv, "NC", "DTE-05-DEV2-" + sufijo, codigo(sufijo, 4), idCcf,
                        "{ \"idProducto\": " + idA + ", \"cantidad\": 9, \"costoUnitario\": 5.00 }"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"))
                .andExpect(jsonPath("$.mensaje").value("De Champú libro prueba solo quedan 8 unidades del lote de esa compra: no se pueden devolver 9"));
        leer("/api/productos/" + idA).andExpect(jsonPath("$.existencia").value(8));
        leer("/api/proveedores/" + idProv)
                .andExpect(jsonPath("$.ultimasCompras[0].tipoDocumentoProveedor").value("NC"))
                .andExpect(jsonPath("$.comprasAnio.cantidad").value(3))
                .andExpect(jsonPath("$.comprasAnio.total").value(56.73));

        leer("/api/compras", "idProveedor", String.valueOf(idProv))
                .andExpect(jsonPath("$.contenido[?(@.idCompra == " + idCcf + ")].gastos").value(contains(1)))
                .andExpect(jsonPath("$.contenido[?(@.idCompra == " + idCcf + ")].primerGasto").value(contains("Bolsas para clientas")))
                .andExpect(jsonPath("$.contenido[?(@.idCompra == " + idNota + ")].productos").value(contains(0)));

        ResultActions libros = leer("/api/libros-iva", "periodo", mes)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value(mes))
                .andExpect(jsonPath("$.pendientes.sinSello").isNumber());
        List<Map<String, Object>> compras = valor(libros, "$.compras");
        Map<Integer, Map<String, Object>> porCompra = compras.stream()
                .collect(Collectors.toMap(f -> (Integer) f.get("idCompra"), f -> f));
        assertEquals("03", porCompra.get(idCcf).get("tipoDocumento"));
        assertEquals(-1, porCompra.get(idNota).get("signo"));
        assertEquals(1, compras.stream().filter(f -> f.get("idCompra").equals(idCcf)).count());
        String linea = (String) porCompra.get(idCcf).get("lineaCsv");
        assertEquals(true, linea.startsWith(LocalDate.parse(hoy).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ";4;03;"));
        leer("/api/libros-iva", "periodo", "2026-13").andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.periodo").exists());
        leer("/api/libros-iva", "periodo", Fechas.hoy().plusMonths(1).toString().substring(0, 7))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.periodo").value("No puede ser un mes futuro."));
        leer("/api/libros-iva").andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.periodo").exists());

        enviar(post("/api/productos/" + idA + "/ajustes"), "{ \"tipo\": \"DANADO\", \"cantidad\": 1, \"motivo\": \"Frasco roto\" }")
                .andExpect(status().isOk());
        leer("/api/inventario/registro", "periodo", mes, "idProducto", String.valueOf(idA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo").value(mes))
                .andExpect(jsonPath("$.productos", hasSize(1)))
                .andExpect(jsonPath("$.productos[0].saldoInicial.unidades").value(0))
                .andExpect(jsonPath("$.productos[0].movimientos", hasSize(3)))
                .andExpect(jsonPath("$.productos[0].movimientos[?(@.tipo == 'ENTRADA_COMPRA')].tipoDocumento").value(contains("CCF")))
                .andExpect(jsonPath("$.productos[0].movimientos[?(@.tipo == 'ENTRADA_COMPRA')].nacionalidadProveedor").value(contains("Guatemala")))
                .andExpect(jsonPath("$.productos[0].movimientos[?(@.tipo == 'ENTRADA_COMPRA')].referenciaCosto").value(contains(containsString(numeroCcf))))
                .andExpect(jsonPath("$.productos[0].movimientos[?(@.tipo == 'ENTRADA_COMPRA')].entradaValor").value(contains(50.00)))
                .andExpect(jsonPath("$.productos[0].movimientos[?(@.tipo == 'DEVOLUCION_PROVEEDOR')].descripcionOperacion").value(contains("Devolución a proveedor")))
                .andExpect(jsonPath("$.productos[0].movimientos[?(@.tipo == 'DEVOLUCION_PROVEEDOR')].tipoDocumento").value(contains("NC")))
                .andExpect(jsonPath("$.productos[0].movimientos[?(@.tipo == 'DEVOLUCION_PROVEEDOR')].salidaUnidades").value(contains(2)))
                .andExpect(jsonPath("$.productos[0].movimientos[?(@.tipo == 'SALIDA_AJUSTE')].descripcionOperacion").value(contains("Ajuste: Frasco roto")))
                .andExpect(jsonPath("$.productos[0].movimientos[2].saldoUnidades").value(7))
                .andExpect(jsonPath("$.productos[0].movimientos[2].saldoValor").value(35.00));
        leer("/api/inventario/registro", "periodo", mes)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos[?(@.idProducto == " + idA + ")]", hasSize(1)))
                .andExpect(jsonPath("$.productos[?(@.idProducto == " + idSinCompra + ")]", hasSize(0)));
        leer("/api/inventario/registro", "periodo", mes, "idProducto", "999999").andExpect(status().isNotFound());

        jdbc.sql("delete from tb_inventario_fisico_conteo where id_inventario_fisico in (select id_inventario_fisico from tb_inventarios_fisicos where estado = 'BORRADOR')").update();
        jdbc.sql("delete from tb_inventarios_fisicos where estado = 'BORRADOR'").update();
        LocalDate corte = Fechas.hoy();
        boolean corteLibre = jdbc.sql("select count(*) from tb_inventarios_fisicos where fecha_corte = :f")
                .param("f", corte).query(Long.class).single() == 0;
        assertEquals(true, corteLibre, "Ya hay un inventario físico de hoy en development");

        ResultActions creado = enviar(post("/api/inventario-fisico"), "{ \"fechaCorte\": \"" + corte + "\" }")
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.acta").value(nullValue()))
                .andExpect(jsonPath("$.conteo[?(@.idProducto == " + idA + ")].existenciaSistema").value(contains(7)))
                .andExpect(jsonPath("$.conteo[?(@.idProducto == " + idA + ")].valorSistema").value(contains(35.00)))
                .andExpect(jsonPath("$.conteo[?(@.idProducto == " + idA + ")].cantidadContada").value(contains(nullValue())));
        int idInv = valor(creado, "$.idInventarioFisico");
        enviar(post("/api/inventario-fisico"), "{ \"fechaCorte\": \"" + corte + "\" }")
                .andExpect(status().isConflict());
        enviar(post("/api/inventario-fisico"), "{ \"fechaCorte\": \"" + corte.minusDays(400) + "\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.fechaCorte", containsString("conteo abierto")));
        enviar(post("/api/inventario-fisico"), "{ \"fechaCorte\": \"" + corte.plusDays(1) + "\" }")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.fechaCorte").value("La fecha de corte no puede ser futura."));

        enviar(post("/api/inventario-fisico/" + idInv + "/cerrar"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.conteo[?(@.idProducto == " + idA + ")].nombreProducto").value(contains("Champú libro prueba")));
        enviar(put("/api/inventario-fisico/" + idInv + "/conteo"), "[ { \"idProducto\": " + idA + ", \"cantidadContada\": -1 } ]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.conteo").exists());

        List<Map<String, Object>> conteo = valor(leer("/api/inventario-fisico/" + idInv), "$.conteo");
        String filas = conteo.stream().map(f -> {
            int id = (Integer) f.get("idProducto");
            int contada = id == idA ? 5 : id == idSinCompra ? 2 : (Integer) f.get("existenciaSistema");
            String notaFila = id == idA ? "\"Dos rotos en bodega\"" : "null";
            return "{ \"idProducto\": %d, \"cantidadContada\": %d, \"nota\": %s }".formatted(id, contada, notaFila);
        }).collect(Collectors.joining(", ", "[", "]"));
        enviar(put("/api/inventario-fisico/" + idInv + "/conteo"), filas)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteo[?(@.idProducto == " + idA + ")].cantidadContada").value(contains(5)));
        sinDejarRastro(post("/api/inventario-fisico/" + idInv + "/cerrar"), "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REGLA_DE_LA_BASE"))
                .andExpect(jsonPath("$.mensaje").value("Nunca comprado prueba: sobran unidades pero nunca se compró antes del corte, no hay costo para valuarlas"));
        enviar(put("/api/inventario-fisico/" + idInv + "/conteo"), "[ { \"idProducto\": " + idSinCompra + ", \"cantidadContada\": 0 } ]")
                .andExpect(status().isOk());

        enviar(post("/api/inventario-fisico/" + idInv + "/cerrar"), "{ \"observaciones\": \"Conteo con la contadora\" }")
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.cerradoEn").exists())
                .andExpect(jsonPath("$.observaciones").value("Conteo con la contadora"))
                .andExpect(jsonPath("$.ajustes", hasSize(1)))
                .andExpect(jsonPath("$.ajustes[0].idProducto").value(idA))
                .andExpect(jsonPath("$.ajustes[0].cantidad").value(-2))
                .andExpect(jsonPath("$.ajustes[0].valor").value(-10.00))
                .andExpect(jsonPath("$.conteo[?(@.idProducto == " + idA + ")].existenciaSistema").value(contains(7)))
                .andExpect(jsonPath("$.conteo[?(@.idProducto == " + idA + ")].valorSistema").value(contains(35.00)))
                .andExpect(jsonPath("$.acta[?(@.codigo == 'CL-" + sufijo + "')].cantidad").value(contains(5)))
                .andExpect(jsonPath("$.acta[?(@.codigo == 'CL-" + sufijo + "')].referenciaCosto").value(contains(containsString(numeroCcf))));
        enviar(post("/api/inventario-fisico/" + idInv + "/cerrar"), "{}").andExpect(status().isConflict());
        enviar(put("/api/inventario-fisico/" + idInv + "/conteo"), "[ { \"idProducto\": " + idA + ", \"cantidadContada\": 1 } ]")
                .andExpect(status().isConflict());

        leer("/api/inventario/registro", "periodo", mes, "idProducto", String.valueOf(idA))
                .andExpect(jsonPath("$.productos[0].movimientos", hasSize(4)))
                .andExpect(jsonPath("$.productos[0].movimientos[3].descripcionOperacion").value("Ajuste por inventario físico: Dos rotos en bodega"))
                .andExpect(jsonPath("$.productos[0].movimientos[3].saldoUnidades").value(5));
        leer("/api/productos/" + idA).andExpect(jsonPath("$.existencia").value(5));
        leer("/api/inventario-fisico")
                .andExpect(jsonPath("$[?(@.idInventarioFisico == " + idInv + ")].estado").value(contains("CERRADO")));
    }

    long avisos(int idProducto) {
        return jdbc.sql("select count(*) from tb_notificaciones where tipo = 'STOCK_BAJO' and entidad_id = :id")
                .param("id", String.valueOf(idProducto)).query(Long.class).single();
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionVePeroNoModifica() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        mvc.perform(get("/api/categorias")).andExpect(status().isOk());
        mvc.perform(post("/api/marcas").contentType(MediaType.APPLICATION_JSON).content("{ \"nombreMarca\": \"X\" }"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content("""
                        { "idProveedor": 1, "fechaCompra": "2026-01-01", "tipoDocumentoProveedor": "OTRO", "claseDocumento": 1,
                          "periodoIva": "2026-01-01", "tipoOperacionRenta": 1, "clasificacionRenta": 2, "sectorRenta": 4,
                          "tipoCostoGasto": 2, "detalle": [ { "descripcion": "Luz", "cantidad": 1, "costoUnitario": 1.00 } ] }
                        """))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.tipoDocumentoProveedor").value("Elige el tipo de documento"));
        mvc.perform(post("/api/productos/1/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"tipo\": \"DANADO\", \"cantidad\": 1, \"motivo\": \"x\" }"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/libros-iva").param("periodo", "2026-11")).andExpect(status().isForbidden());
        mvc.perform(get("/api/inventario/registro").param("periodo", "2026-11")).andExpect(status().isForbidden());
        mvc.perform(get("/api/inventario-fisico")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void auditorLeeLosLibrosPeroNoCuenta() throws Exception {
        mvc.perform(get("/api/libros-iva").param("periodo", "2026-11")).andExpect(status().isOk());
        mvc.perform(get("/api/inventario/registro").param("periodo", "2026-11")).andExpect(status().isOk());
        mvc.perform(get("/api/inventario-fisico")).andExpect(status().isOk());
        mvc.perform(post("/api/inventario-fisico").contentType(MediaType.APPLICATION_JSON).content("{ \"fechaCorte\": \"2025-12-31\" }"))
                .andExpect(status().isForbidden());
    }
}
