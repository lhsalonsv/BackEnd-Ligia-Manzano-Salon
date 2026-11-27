package com.ligiamanzano.salon.configuracion.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.configuracion.dto.FormaPagoActivoRequest;
import com.ligiamanzano.salon.configuracion.dto.FormaPagoDTO;
import com.ligiamanzano.salon.configuracion.dto.HorarioSalonDTO;
import com.ligiamanzano.salon.configuracion.dto.ReglasDTO;
import com.ligiamanzano.salon.configuracion.dto.ReglasRequest;
import com.ligiamanzano.salon.configuracion.entity.FormaPagoEntity;
import com.ligiamanzano.salon.configuracion.entity.HorarioSalonEntity;
import com.ligiamanzano.salon.configuracion.repository.ConfiguracionConsultaRepository;
import com.ligiamanzano.salon.configuracion.repository.ConfiguracionConsultaRepository.Turno;
import com.ligiamanzano.salon.configuracion.repository.FormaPagoRepository;
import com.ligiamanzano.salon.configuracion.repository.HorarioSalonRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ligiamanzano.salon.configuracion.service.ParametroService.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConfiguracionService {

    public static final String ENTIDAD_REGLAS = "REGLAS";
    private static final String[] DIAS = {"", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final String REVISA = "Revisa los campos marcados";

    private final HorarioSalonRepository horarioRepo;
    private final FormaPagoRepository formaPagoRepo;
    private final ParametroService parametros;
    private final ConfiguracionConsultaRepository consultas;
    private final BitacoraService bitacora;
    private final EntityManager entityManager;

    public List<HorarioSalonDTO> listarHorarioSalon() {
        return horarioRepo.findAllByOrderByDiaSemanaAsc().stream()
                .map(h -> new HorarioSalonDTO(h.getDiaSemana(), h.isAbierto(), h.getHoraAbre(), h.getHoraCierra()))
                .toList();
    }

    public ReglasDTO obtenerReglas() {
        return new ReglasDTO(
                listarHorarioSalon(),
                parametros.numero(COMISION_SERVICIO_PCT),
                monto(parametros.numero(COMISION_PRODUCTO)),
                parametros.numero(ISSS_EMPLEADO_PCT),
                monto(parametros.numero(ISSS_TOPE_MENSUAL)),
                parametros.numero(AFP_EMPLEADO_PCT),
                formaPagoRepo.findAllByOrderByIdFormaPagoAsc().stream().map(this::convertirADTO).toList(),
                consultas.ultimoCambio(BitacoraService.CONFIGURACION, ENTIDAD_REGLAS));
    }

    @Transactional
    public ReglasDTO actualizarReglas(ReglasRequest dto) {
        Map<Short, HorarioSalonDTO> horarios = revisarHorarios(dto.horarios());
        List<FormaPagoEntity> formas = formaPagoRepo.findAllByOrderByIdFormaPagoAsc();
        Map<Short, Boolean> activas = revisarFormasPago(dto.formasPago(), formas);
        exigirTurnosDentro(horarios);

        Map<String, Object> antes = datos(obtenerReglas());

        for (HorarioSalonEntity h : horarioRepo.findAllByOrderByDiaSemanaAsc()) {
            HorarioSalonDTO nuevo = horarios.remove(h.getDiaSemana());
            convertirAEntity(nuevo, h);
        }
        for (HorarioSalonDTO nuevo : horarios.values()) {
            HorarioSalonEntity h = new HorarioSalonEntity();
            h.setDiaSemana(nuevo.diaSemana());
            convertirAEntity(nuevo, h);
            horarioRepo.save(h);
        }
        parametros.guardar(COMISION_SERVICIO_PCT, porcentaje(dto.comisionServicioPct()));
        parametros.guardar(COMISION_PRODUCTO, monto(dto.comisionProducto()));
        parametros.guardar(ISSS_EMPLEADO_PCT, porcentaje(dto.isssEmpleadoPct()));
        parametros.guardar(ISSS_TOPE_MENSUAL, monto(dto.isssTopeMensual()));
        parametros.guardar(AFP_EMPLEADO_PCT, porcentaje(dto.afpEmpleadoPct()));
        for (FormaPagoEntity f : formas) {
            f.setActivo(activas.getOrDefault(f.getIdFormaPago(), f.isActivo()));
        }
        entityManager.flush();

        bitacora.registrarCambios(BitacoraService.CONFIGURACION, "EDITAR", ENTIDAD_REGLAS, null,
                "Cambió las reglas del salón (horario, comisiones, ISSS, AFP y formas de pago)",
                antes, datos(obtenerReglas()));
        entityManager.flush();
        return obtenerReglas();
    }

    private Map<Short, HorarioSalonDTO> revisarHorarios(List<HorarioSalonDTO> horarios) {
        Map<String, String> campos = new LinkedHashMap<>();
        Map<Short, HorarioSalonDTO> porDia = new LinkedHashMap<>();
        for (HorarioSalonDTO h : horarios) {
            porDia.put(h.diaSemana(), h);
        }
        if (porDia.size() != 7) {
            throw new OperacionInvalidaException(REVISA, "DATOS_INVALIDOS",
                    Map.of("horarios", "Manda los 7 días de la semana, una vez cada uno."));
        }
        for (HorarioSalonDTO h : porDia.values()) {
            if (!h.abierto()) continue;
            if (h.horaAbre() == null || h.horaCierra() == null) {
                campos.put("abre-" + h.diaSemana(), "Escribe a qué hora abre y a qué hora cierra.");
            } else if (!h.horaCierra().isAfter(h.horaAbre())) {
                campos.put("abre-" + h.diaSemana(), "La hora de cierre tiene que ser después de la de apertura.");
            }
        }
        if (porDia.values().stream().noneMatch(HorarioSalonDTO::abierto)) {
            campos.putIfAbsent("abre-1", "El salón tiene que abrir al menos un día.");
        }
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException(REVISA, "DATOS_INVALIDOS", campos);
        }
        return porDia;
    }

    private static Map<Short, Boolean> revisarFormasPago(List<FormaPagoActivoRequest> pedidas, List<FormaPagoEntity> formas) {
        Map<Short, FormaPagoEntity> porId = formas.stream()
                .collect(Collectors.toMap(FormaPagoEntity::getIdFormaPago, Function.identity()));
        Map<Short, Boolean> activas = new LinkedHashMap<>();
        for (FormaPagoActivoRequest f : pedidas) {
            if (!porId.containsKey(f.idFormaPago())) {
                throw new OperacionInvalidaException(REVISA, "DATOS_INVALIDOS",
                        Map.of("formasPago", "La forma de pago " + f.idFormaPago() + " no existe."));
            }
            activas.put(f.idFormaPago(), f.activo());
        }
        for (FormaPagoEntity f : formas) {
            boolean activa = activas.getOrDefault(f.getIdFormaPago(), f.isActivo());
            if (f.esEfectivo() && !activa) {
                throw new OperacionInvalidaException(REVISA, "DATOS_INVALIDOS",
                        Map.of("formasPago", "Efectivo no se puede desactivar: con él se abre y se cierra la caja."));
            }
        }
        boolean algunaActiva = formas.stream().anyMatch(f -> activas.getOrDefault(f.getIdFormaPago(), f.isActivo()));
        if (!algunaActiva) {
            throw new OperacionInvalidaException(REVISA, "DATOS_INVALIDOS",
                    Map.of("formasPago", "Deja al menos una forma de pago activa."));
        }
        return activas;
    }

    private void exigirTurnosDentro(Map<Short, HorarioSalonDTO> horarios) {
        for (Turno t : consultas.turnosDeEmpleadasActivas()) {
            HorarioSalonDTO salon = horarios.get(t.diaSemana());
            String dia = DIAS[t.diaSemana()];
            String cuando = null;
            if (salon == null || !salon.abierto()) {
                cuando = "";
            } else if (t.horaFin().isAfter(salon.horaCierra())) {
                cuando = " hasta las " + hora12(t.horaFin());
            } else if (t.horaInicio().isBefore(salon.horaAbre())) {
                cuando = " desde las " + hora12(t.horaInicio());
            }
            if (cuando != null) {
                String frase = t.nombreEmpleada() + " trabaja el " + dia + cuando;
                throw new ConflictoException((frase.endsWith(".") ? frase : frase + ".")
                        + " Cambie primero su horario.", "HORARIO_FUERA", null);
            }
        }
    }

    private static void convertirAEntity(HorarioSalonDTO dto, HorarioSalonEntity h) {
        h.setAbierto(dto.abierto());
        h.setHoraAbre(dto.abierto() ? dto.horaAbre() : null);
        h.setHoraCierra(dto.abierto() ? dto.horaCierra() : null);
    }

    private FormaPagoDTO convertirADTO(FormaPagoEntity f) {
        return new FormaPagoDTO(f.getIdFormaPago(), f.getNombre(), f.isPideReferencia(), f.isActivo());
    }

    private static Map<String, Object> datos(ReglasDTO r) {
        Map<String, Object> datos = new LinkedHashMap<>();
        for (HorarioSalonDTO h : r.horarios()) {
            datos.put(DIAS[h.diaSemana()], h.abierto() ? h.horaAbre() + " a " + h.horaCierra() : "Cerrado");
        }
        datos.put("comisionServicioPct", r.comisionServicioPct());
        datos.put("comisionProducto", r.comisionProducto());
        datos.put("isssEmpleadoPct", r.isssEmpleadoPct());
        datos.put("isssTopeMensual", r.isssTopeMensual());
        datos.put("afpEmpleadoPct", r.afpEmpleadoPct());
        for (FormaPagoDTO f : r.formasPago()) {
            datos.put(f.nombre(), f.activo() ? "Activa" : "Inactiva");
        }
        return datos;
    }

    private static BigDecimal porcentaje(BigDecimal valor) {
        BigDecimal limpio = valor.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0) : limpio;
    }

    private static BigDecimal monto(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    static String hora12(LocalTime hora) {
        int h = hora.getHour();
        int m = hora.getMinute();
        if (h == 12 && m == 0) return "12:00 m.";
        return (h % 12 == 0 ? 12 : h % 12) + ":" + String.format("%02d", m) + (h < 12 ? " a. m." : " p. m.");
    }
}
