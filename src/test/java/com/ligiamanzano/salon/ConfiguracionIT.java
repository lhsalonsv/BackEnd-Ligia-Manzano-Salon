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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfiguracionIT {

    private static final String[] DIAS = {"", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};

    record Dia(short dia, boolean abierto, LocalTime abre, LocalTime cierra) {}

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired EntityManager em;

    Authentication sesionAdmin;
    int idAdmin;

    @BeforeEach
    void preparar() {
        String sufijo = String.valueOf(System.nanoTime() % 100000);
        UsuarioEntity admin = new UsuarioEntity();
        admin.setNombreUsuario("prueba.admin" + sufijo);
        admin.setNombreCompleto("Prueba Admin");
        admin.setContrasenaHash("sin-clave");
        admin.setIdRol(Rol.ADMINISTRADOR.getId());
        admin.setActivo(true);
        admin = usuarios.save(admin);
        idAdmin = admin.getIdUsuario();
        sesionAdmin = sesion(idAdmin, admin.getNombreUsuario(), Rol.ADMINISTRADOR);
    }

    static Authentication sesion(int id, String usuario, Rol rol) {
        UsuarioAutenticado u = new UsuarioAutenticado(id, usuario, "Prueba " + rol.name(), rol, false, UUID.randomUUID());
        return UsernamePasswordAuthenticationToken.authenticated(u, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
    }

    ResultActions enviar(MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
        ResultActions r = mvc.perform(peticion.with(authentication(sesionAdmin)).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
        em.clear();
        return r;
    }

    ResultActions leer(String ruta) throws Exception {
        return mvc.perform(get(ruta).with(authentication(sesionAdmin)));
    }

    static String negocio(String codActividad, int idDistrito, String nit, String codEstable) {
        return """
                { "nombreComercial": "  Salón de Prueba  ", "nombre": "Prueba Emisora", "nit": "%s", "nrc": "1234567",
                  "codActividad": "%s", "tipoEstablecimiento": "02", "idDistrito": %d,
                  "complemento": "Calle de Prueba 1", "telefono": "7000-0000", "correo": "Prueba@Salon.example",
                  "codEstable": "%s", "codPuntoVenta": "P001" }
                """.formatted(nit, codActividad, idDistrito, codEstable);
    }

    List<Dia> horarioActual() {
        return jdbc.sql("select dia_semana, abierto, hora_abre, hora_cierra from tb_horarios order by dia_semana")
                .query((rs, n) -> new Dia(rs.getShort(1), rs.getBoolean(2),
                        rs.getObject(3, LocalTime.class), rs.getObject(4, LocalTime.class)))
                .list();
    }

    static String horarios(List<Dia> dias) {
        List<String> partes = new ArrayList<>();
        for (Dia d : dias) {
            partes.add("{ \"diaSemana\": %d, \"abierto\": %s, \"horaAbre\": %s, \"horaCierra\": %s }".formatted(
                    d.dia(), d.abierto(), hora(d.abierto() ? d.abre() : null), hora(d.abierto() ? d.cierra() : null)));
        }
        return "[" + String.join(", ", partes) + "]";
    }

    static String hora(LocalTime t) {
        return t == null ? "null" : "\"" + t.toString().substring(0, 5) + "\"";
    }

    static String reglas(String horarios, String pctServicio, String formasPago) {
        return """
                { "horarios": %s, "comisionServicioPct": %s, "comisionProducto": 2.5,
                  "isssEmpleadoPct": 3, "isssTopeMensual": 1000, "afpEmpleadoPct": 7.25,
                  "formasPago": %s }
                """.formatted(horarios, pctServicio, formasPago);
    }

    static List<Dia> cambiar(List<Dia> dias, Dia nuevo) {
        return dias.stream().map(d -> d.dia() == nuevo.dia() ? nuevo : d).toList();
    }

    static String hora12(LocalTime t) {
        int h = t.getHour();
        int m = t.getMinute();
        if (h == 12 && m == 0) return "12:00 m.";
        return (h % 12 == 0 ? 12 : h % 12) + ":" + String.format("%02d", m) + (h < 12 ? " a. m." : " p. m.");
    }

    long bitacora(String entidad) {
        return jdbc.sql("select count(*) from tb_bitacora where modulo = 'CONFIGURACION' and entidad = :e and id_usuario = :u")
                .param("e", entidad).param("u", idAdmin).query(Long.class).single();
    }

    @Test
    void negocio() throws Exception {
        boolean habia = jdbc.sql("select count(*) from tb_emisores").query(Long.class).single() > 0;
        if (!habia) {
            leer("/api/configuracion/negocio")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value(nullValue()))
                    .andExpect(jsonPath("$.ambiente").value("00"))
                    .andExpect(jsonPath("$.ultimoCambio").value(nullValue()));
        }

        jdbc.sql("insert into tb_cat_actividades (cod_actividad, descripcion) values ('Z9999', 'Actividad de prueba')").update();
        int idMunicipio = jdbc.sql("""
                        insert into tb_cat_municipios (cod_departamento, cod_municipio, nombre)
                        values ('06', 'Z9', 'Municipio de prueba') returning id_municipio
                        """).query(Integer.class).single();
        int idDistrito = jdbc.sql("""
                        insert into tb_cat_distritos (id_municipio, cod_distrito, nombre)
                        values (:m, 'Z9', 'Distrito de prueba') returning id_distrito
                        """).param("m", idMunicipio).query(Integer.class).single();

        enviar(put("/api/configuracion/negocio"), negocio("ZZZZZ", 999999999, "06141234567890", "M001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codActividad").value("No está en el catálogo de actividades."))
                .andExpect(jsonPath("$.campos.idDistrito").value("No está en el catálogo de distritos."));
        enviar(put("/api/configuracion/negocio"), negocio("Z9999", idDistrito, "0614-123456-789-0", "X001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nit").exists())
                .andExpect(jsonPath("$.campos.codEstable").value("Usa una letra M, B, S o P y 3 dígitos (M001)"));

        enviar(put("/api/configuracion/negocio"), negocio("Z9999", idDistrito, "06141234567890", "M001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreComercial").value("Salón de Prueba"))
                .andExpect(jsonPath("$.codEstable").value("M001"))
                .andExpect(jsonPath("$.codPuntoVenta").value("P001"))
                .andExpect(jsonPath("$.correo").value("prueba@salon.example"))
                .andExpect(jsonPath("$.idDistrito").value(idDistrito))
                .andExpect(jsonPath("$.ambiente").value(notNullValue()))
                .andExpect(jsonPath("$.ultimoCambio.nombreUsuario").value("Prueba Admin"));

        String accion = jdbc.sql("""
                        select accion from tb_bitacora where modulo = 'CONFIGURACION' and entidad = 'NEGOCIO' and id_usuario = :u
                        """).param("u", idAdmin).query(String.class).single();
        assertEquals(habia ? "EDITAR" : "CREAR", accion);

        leer("/api/configuracion/negocio")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nit").value("06141234567890"))
                .andExpect(jsonPath("$.codActividad").value("Z9999"));
    }

    @Test
    void reglas() throws Exception {
        List<Dia> actual = horarioActual();
        assumeTrue(actual.size() == 7, "tb_horarios no tiene los 7 días");
        Dia abierto = actual.stream().filter(Dia::abierto).findFirst().orElse(null);
        assumeTrue(abierto != null, "El salón no abre ningún día");

        leer("/api/configuracion/reglas")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios", hasSize(7)))
                .andExpect(jsonPath("$.comisionServicioPct").isNumber())
                .andExpect(jsonPath("$.formasPago[0].nombre").value("Efectivo"));

        String formas = "[{ \"idFormaPago\": 1, \"activo\": true }, { \"idFormaPago\": 4, \"activo\": false }]";
        enviar(put("/api/configuracion/reglas"), reglas(horarios(actual), "12.5", formas))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comisionServicioPct").value(12.5))
                .andExpect(jsonPath("$.comisionProducto").value(2.5))
                .andExpect(jsonPath("$.formasPago[?(@.idFormaPago == 4)].activo").value(contains(false)))
                .andExpect(jsonPath("$.ultimoCambio.nombreUsuario").value("Prueba Admin"));
        assertEquals("12.5", jdbc.sql("select valor from tb_parametros where clave = 'COMISION_SERVICIO_PCT'").query(String.class).single());
        assertEquals("2.50", jdbc.sql("select valor from tb_parametros where clave = 'COMISION_PRODUCTO'").query(String.class).single());
        assertEquals(1, bitacora("REGLAS"));

        enviar(put("/api/configuracion/reglas"), reglas(horarios(actual), "12.50", formas)).andExpect(status().isOk());
        assertEquals(1, bitacora("REGLAS"), "Sin cambios no se escribe otra entrada en la bitácora");

        enviar(put("/api/configuracion/reglas"), reglas(horarios(actual), "10", "[{ \"idFormaPago\": 1, \"activo\": false }]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.formasPago").value(containsString("Efectivo")));
        enviar(put("/api/configuracion/reglas"), reglas(horarios(actual), "10", "[{ \"idFormaPago\": 99, \"activo\": true }]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.formasPago").value("La forma de pago 99 no existe."));
        enviar(put("/api/configuracion/reglas"), reglas(horarios(actual), "101", formas))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.comisionServicioPct").value("De 0 a 100"));

        List<Dia> cerrados = actual.stream().map(d -> new Dia(d.dia(), false, null, null)).toList();
        enviar(put("/api/configuracion/reglas"), reglas(horarios(cerrados), "10", formas))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['campos']['abre-1']").value("El salón tiene que abrir al menos un día."));

        Dia alReves = new Dia(abierto.dia(), true, LocalTime.of(18, 0), LocalTime.of(8, 0));
        enviar(put("/api/configuracion/reglas"), reglas(horarios(cambiar(actual, alReves)), "10", formas))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$['campos']['abre-" + abierto.dia() + "']")
                        .value("La hora de cierre tiene que ser después de la de apertura."));

        enviar(put("/api/configuracion/reglas"), reglas(horarios(actual.subList(0, 6)), "10", formas))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.horarios").exists());
        List<Dia> repetido = new ArrayList<>(actual.subList(0, 6));
        repetido.add(actual.get(0));
        enviar(put("/api/configuracion/reglas"), reglas(horarios(repetido), "10", formas))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.horarios").value("Manda los 7 días de la semana, una vez cada uno."));
    }

    @Test
    void horarioFueraDeUnaEmpleada() throws Exception {
        List<Dia> actual = horarioActual();
        List<Dia> abiertos = actual.stream().filter(Dia::abierto).toList();
        assumeTrue(actual.size() == 7 && abiertos.size() >= 2, "Hacen falta al menos 2 días abiertos");
        Dia dia = abiertos.get(0);
        assumeTrue(dia.cierra().minusHours(1).isAfter(dia.abre()), "El día es demasiado corto");

        int idEmpleado = jdbc.sql("""
                        insert into tb_empleados (nombre_empleado, apellido, puesto, fecha_ingreso, sueldo_base_mensual)
                        values ('Prueba', 'Horario', 'Estilista', :f, 400) returning id_empleado
                        """).param("f", LocalDate.now().minusYears(1)).query(Integer.class).single();
        jdbc.sql("insert into tb_horarios_empleado (id_empleado, dia_semana, hora_inicio, hora_fin) values (:e, :d, :i, :f)")
                .param("e", idEmpleado).param("d", dia.dia()).param("i", dia.abre()).param("f", dia.cierra())
                .update();
        long otras = jdbc.sql("""
                        select count(*) from tb_horarios_empleado h join tb_empleados e on e.id_empleado = h.id_empleado
                         where e.activo and e.fecha_baja is null and h.dia_semana = :d and h.id_empleado <> :e
                        """).param("d", dia.dia()).param("e", idEmpleado).query(Long.class).single();
        String formas = "[{ \"idFormaPago\": 1, \"activo\": true }]";

        Dia masCorto = new Dia(dia.dia(), true, dia.abre(), dia.cierra().minusHours(1));
        ResultActions r = enviar(put("/api/configuracion/reglas"), reglas(horarios(cambiar(actual, masCorto)), "10", formas))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("HORARIO_FUERA"))
                .andExpect(jsonPath("$.mensaje").value(endsWith("Cambie primero su horario.")));
        if (otras == 0) {
            r.andExpect(jsonPath("$.mensaje").value("Prueba Horario trabaja el " + DIAS[dia.dia()]
                    + " hasta las " + hora12(dia.cierra()) + " Cambie primero su horario."));
        }

        Dia cerrado = new Dia(dia.dia(), false, null, null);
        r = enviar(put("/api/configuracion/reglas"), reglas(horarios(cambiar(actual, cerrado)), "10", formas))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("HORARIO_FUERA"));
        if (otras == 0) {
            r.andExpect(jsonPath("$.mensaje").value("Prueba Horario trabaja el " + DIAS[dia.dia()] + ". Cambie primero su horario."));
        }
        assertEquals(0, bitacora("REGLAS"));
    }

    @Test
    void soloLaAdministradora() throws Exception {
        for (Rol rol : List.of(Rol.CONTADOR, Rol.RECEPCIONISTA, Rol.AUDITOR)) {
            Authentication otra = sesion(999999, "prueba.otra", rol);
            mvc.perform(get("/api/configuracion/negocio").with(authentication(otra))).andExpect(status().isForbidden());
            mvc.perform(get("/api/configuracion/reglas").with(authentication(otra))).andExpect(status().isForbidden());
            mvc.perform(put("/api/configuracion/negocio").with(authentication(otra))
                            .contentType(MediaType.APPLICATION_JSON).content(negocio("Z9999", 1, "06141234567890", "M001")))
                    .andExpect(status().isForbidden());
        }
    }
}
