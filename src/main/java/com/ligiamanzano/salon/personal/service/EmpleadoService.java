package com.ligiamanzano.salon.personal.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.configuracion.dto.HorarioSalonDTO;
import com.ligiamanzano.salon.configuracion.service.ConfiguracionService;
import com.ligiamanzano.salon.personal.dto.*;
import com.ligiamanzano.salon.personal.entity.EmpleadoEntity;
import com.ligiamanzano.salon.personal.entity.EmpleadoEspecialidadEntity;
import com.ligiamanzano.salon.personal.entity.EspecialidadEntity;
import com.ligiamanzano.salon.personal.entity.HorarioEmpleadoEntity;
import com.ligiamanzano.salon.personal.repository.EmpleadoConsultaRepository;
import com.ligiamanzano.salon.personal.repository.EmpleadoEspecialidadRepository;
import com.ligiamanzano.salon.personal.repository.EmpleadoRepository;
import com.ligiamanzano.salon.personal.repository.EspecialidadRepository;
import com.ligiamanzano.salon.personal.repository.HorarioEmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EmpleadoService {

    private static final String[] DIAS = {"", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final int BLOQUES_POR_DIA = 2;
    private static final String ENTIDAD = "EMPLEADO";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d/M/yyyy");

    private final EmpleadoRepository repo;
    private final EspecialidadRepository especialidadRepo;
    private final EmpleadoEspecialidadRepository empleadoEspecialidadRepo;
    private final HorarioEmpleadoRepository horarioRepo;
    private final EmpleadoConsultaRepository consultas;
    private final ConfiguracionService configuracion;
    private final BitacoraService bitacora;

    public EmpleadosRespuestaDTO listar() {
        QuincenaDTO quincena = quincenaActual();
        return new EmpleadosRespuestaDTO(
                quincena,
                configuracion.listarHorarioSalon(),
                consultas.lista(quincena.inicio(), quincena.fin()));
    }

    public EmpleadoFichaDTO obtener(int idEmpleado) {
        return convertirADTO(buscar(idEmpleado));
    }

    @Transactional
    public EmpleadoFichaDTO crear(EmpleadoRequest dto) {
        validar(dto, null, true);
        EmpleadoEntity e = new EmpleadoEntity();
        convertirAEntity(dto, e);
        e.setActivo(true);
        repo.save(e);
        reemplazarEspecialidadesYHorarios(e.getIdEmpleado(), dto);
        EmpleadoFichaDTO ficha = convertirADTO(e);
        String puesto = e.getPuesto() == null ? "" : " como " + e.getPuesto();
        bitacora.registrarCambios(BitacoraService.EMPLEADOS, "CREAR", ENTIDAD, e.getIdEmpleado(),
                "Registró a " + nombreCompleto(e) + puesto, null, datosBitacora(ficha));
        return ficha;
    }

    @Transactional
    public EmpleadoFichaDTO actualizar(int idEmpleado, EmpleadoRequest dto) {
        EmpleadoEntity e = buscar(idEmpleado);
        validar(dto, idEmpleado, e.isActivo());
        Map<String, Object> antes = datosBitacora(convertirADTO(e));
        convertirAEntity(dto, e);
        reemplazarEspecialidadesYHorarios(idEmpleado, dto);
        EmpleadoFichaDTO ficha = convertirADTO(e);
        bitacora.registrarCambios(BitacoraService.EMPLEADOS, "EDITAR", ENTIDAD, idEmpleado,
                "Editó los datos de " + nombreCompleto(e), antes, datosBitacora(ficha));
        return ficha;
    }

    @Transactional
    public EmpleadoFichaDTO darDeBaja(int idEmpleado, LocalDate fechaBaja) {
        EmpleadoEntity e = buscar(idEmpleado);
        if (!e.isActivo()) {
            throw new OperacionInvalidaException("Este empleado ya está de baja.");
        }
        if (fechaBaja.isBefore(e.getFechaIngreso())) {
            throw errorDeCampo("fechaBaja", "No puede ser antes de la fecha de ingreso.");
        }
        if (fechaBaja.isAfter(Fechas.hoy())) {
            throw errorDeCampo("fechaBaja", "No se programan bajas a futuro.");
        }
        Map<String, Object> antes = datosBitacora(convertirADTO(e));
        e.setFechaBaja(fechaBaja);
        e.setActivo(false);
        horarioRepo.borrarDeEmpleado(idEmpleado);
        EmpleadoFichaDTO ficha = convertirADTO(e);
        bitacora.registrarCambios(BitacoraService.EMPLEADOS, "DAR_DE_BAJA", ENTIDAD, idEmpleado,
                "Dio de baja a " + nombreCompleto(e) + " desde el " + fechaBaja.format(FECHA), antes, datosBitacora(ficha));
        return ficha;
    }

    @Transactional
    public void ligarUsuario(Integer idEmpleado, int idUsuario) {
        Optional<EmpleadoEntity> anterior = repo.findFirstByIdUsuario(idUsuario);
        if (anterior.isPresent() && anterior.get().getIdEmpleado().equals(idEmpleado)) return;
        if (idEmpleado == null) {
            anterior.ifPresent(e -> e.setIdUsuario(null));
            return;
        }
        EmpleadoEntity nuevo = repo.findById(idEmpleado)
                .orElseThrow(() -> errorDeCampo("idEmpleado", "Ese empleado no existe."));
        if (!nuevo.isActivo()) throw errorDeCampo("idEmpleado", "Ese empleado está de baja.");
        if (nuevo.getIdUsuario() != null) throw errorDeCampo("idEmpleado", "Ese empleado ya tiene otro usuario.");
        anterior.ifPresent(e -> e.setIdUsuario(null));
        repo.flush();
        nuevo.setIdUsuario(idUsuario);
    }

    //Del 1 al 15, o del 16 al fin de mes, con la fecha de El Salvador.
    static QuincenaDTO quincenaActual() {
        LocalDate hoy = Fechas.hoy();
        return hoy.getDayOfMonth() <= 15
                ? new QuincenaDTO(hoy.withDayOfMonth(1), hoy.withDayOfMonth(15))
                : new QuincenaDTO(hoy.withDayOfMonth(16), hoy.withDayOfMonth(hoy.lengthOfMonth()));
    }

    private EmpleadoEntity buscar(int idEmpleado) {
        return repo.findById(idEmpleado)
                .orElseThrow(() -> new RecursoNoEncontradoException("El empleado no existe."));
    }

    /**
     * AFP y NUP juntos,
     * especialidades que existan.
     * horario y DUI repetido
     * */
    private void validar(EmpleadoRequest dto, Integer idActual, boolean activo) {
        Map<String, String> campos = new LinkedHashMap<>();
        String afp = Texto.limpio(dto.afpAdministradora());
        String nup = Texto.limpio(dto.nupAfp());
        if (afp != null && nup == null) campos.put("nupAfp", "Escribe el NUP de la AFP.");
        if (afp == null && nup != null) campos.put("afpAdministradora", "Elige la AFP de ese NUP.");

        Set<Integer> especialidades = new HashSet<>(dto.especialidades());
        if (!especialidades.isEmpty() && especialidadRepo.countByIdEspecialidadIn(especialidades) != especialidades.size()) {
            campos.put("especialidades", "Alguna especialidad no existe.");
        }

        String errorHorario = null;
        if (!dto.horarios().isEmpty()) {
            errorHorario = activo
                    ? revisarHorarios(dto.horarios(), campos)
                    : "Un empleado de baja no tiene horario.";
        }

        if (!campos.isEmpty() || errorHorario != null) {
            String mensaje = errorHorario != null ? errorHorario : "Revisa los campos marcados";
            throw new OperacionInvalidaException(mensaje, "DATOS_INVALIDOS", campos);
        }
        exigirDuiLibre(dto.dui(), idActual);
    }

    private String revisarHorarios(List<HorarioEmpleadoDTO> horarios, Map<String, String> campos) {
        Map<Short, HorarioSalonDTO> salon = configuracion.listarHorarioSalon().stream()
                .collect(Collectors.toMap(HorarioSalonDTO::diaSemana, h -> h));
        Map<Short, List<HorarioEmpleadoDTO>> porDia = horarios.stream()
                .sorted(Comparator.comparing(HorarioEmpleadoDTO::horaInicio))
                .collect(Collectors.groupingBy(HorarioEmpleadoDTO::diaSemana, TreeMap::new, Collectors.toList()));

        String primerError = null;
        for (Map.Entry<Short, List<HorarioEmpleadoDTO>> dia : porDia.entrySet()) {
            String error = revisarDia(dia.getKey(), dia.getValue(), salon.get(dia.getKey()));
            if (error != null) {
                campos.put("horario-" + dia.getKey(), error);
                if (primerError == null) primerError = error;
            }
        }
        return primerError;
    }

    private static String revisarDia(short dia, List<HorarioEmpleadoDTO> bloques, HorarioSalonDTO salon) {
        String nombre = DIAS[dia];
        if (bloques.size() > BLOQUES_POR_DIA) {
            return "El " + nombre + " tiene más de " + BLOQUES_POR_DIA + " bloques.";
        }
        if (salon == null || !salon.abierto()) {
            return "El salón cierra el " + nombre + ".";
        }
        for (HorarioEmpleadoDTO b : bloques) {
            if (!b.horaFin().isAfter(b.horaInicio())) {
                return "El " + nombre + ", la hora de fin tiene que ser después de la de inicio.";
            }
            if (b.horaInicio().isBefore(salon.horaAbre()) || b.horaFin().isAfter(salon.horaCierra())) {
                return "El " + nombre + " el salón abre de " + salon.horaAbre() + " a " + salon.horaCierra() + ".";
            }
        }
        if (bloques.size() == BLOQUES_POR_DIA && bloques.get(1).horaInicio().isBefore(bloques.get(0).horaFin())) {
            return "Los bloques del " + nombre + " se cruzan.";
        }
        return null;
    }

    private void exigirDuiLibre(String dui, Integer idActual) {
        repo.findFirstByDui(dui)
                .filter(otro -> !otro.getIdEmpleado().equals(idActual))
                .ifPresent(otro -> {
                    throw new ConflictoException(
                            "Ese DUI ya es de " + otro.getNombreEmpleado() + " " + otro.getApellido() + ".",
                            "EMPLEADO_DUPLICADO",
                            Map.of("dui", "Ya hay un empleado con este DUI."),
                            Map.of("idEmpleadoExistente", otro.getIdEmpleado()));
                });
    }

    private static void convertirAEntity(EmpleadoRequest dto, EmpleadoEntity e) {
        e.setNombreEmpleado(Texto.limpio(dto.nombreEmpleado()));
        e.setApellido(Texto.limpio(dto.apellido()));
        e.setDui(Texto.limpio(dto.dui()));
        e.setTelefono(Texto.limpio(dto.telefono()));
        e.setCorreoEmpleado(Texto.minusculas(dto.correoEmpleado()));
        e.setFechaIngreso(dto.fechaIngreso());
        e.setPuesto(Texto.limpio(dto.puesto()));
        e.setSueldoBaseMensual(dto.sueldoBaseMensual());
        e.setRecibeComision(dto.recibeComision());
        e.setAfpAdministradora(Texto.limpio(dto.afpAdministradora()));
        e.setNupAfp(Texto.limpio(dto.nupAfp()));
        e.setNumeroIsss(Texto.limpio(dto.numeroIsss()));
    }

    private void reemplazarEspecialidadesYHorarios(int idEmpleado, EmpleadoRequest dto) {
        empleadoEspecialidadRepo.borrarDeEmpleado(idEmpleado);
        horarioRepo.borrarDeEmpleado(idEmpleado);

        List<EmpleadoEspecialidadEntity> especialidades = new LinkedHashSet<>(dto.especialidades()).stream()
                .map(idEspecialidad -> {
                    EmpleadoEspecialidadEntity ee = new EmpleadoEspecialidadEntity();
                    ee.setIdEmpleado(idEmpleado);
                    ee.setIdEspecialidad(idEspecialidad);
                    return ee;
                })
                .toList();
        empleadoEspecialidadRepo.saveAll(especialidades);

        List<HorarioEmpleadoEntity> horarios = dto.horarios().stream()
                .map(h -> {
                    HorarioEmpleadoEntity he = new HorarioEmpleadoEntity();
                    he.setIdEmpleado(idEmpleado);
                    he.setDiaSemana(h.diaSemana());
                    he.setHoraInicio(h.horaInicio());
                    he.setHoraFin(h.horaFin());
                    return he;
                })
                .toList();
        horarioRepo.saveAll(horarios);
    }

    private EmpleadoFichaDTO convertirADTO(EmpleadoEntity e) {
        int id = e.getIdEmpleado();
        List<Integer> especialidades = empleadoEspecialidadRepo.findByIdEmpleadoOrderByIdEspecialidadAsc(id).stream()
                .map(EmpleadoEspecialidadEntity::getIdEspecialidad)
                .toList();
        List<HorarioEmpleadoDTO> horarios = horarioRepo.findByIdEmpleadoOrderByDiaSemanaAscHoraInicioAsc(id).stream()
                .map(h -> new HorarioEmpleadoDTO(h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin()))
                .toList();
        UsuarioLigadoDTO usuario = e.getIdUsuario() == null
                ? null
                : consultas.usuarioLigado(e.getIdUsuario()).orElse(null);
        return new EmpleadoFichaDTO(
                id, e.getNombreEmpleado(), e.getApellido(), e.getDui(), e.getTelefono(), e.getCorreoEmpleado(),
                e.getPuesto(), e.getFechaIngreso(), e.getFechaBaja(), e.getSueldoBaseMensual(), e.isRecibeComision(),
                e.getNupAfp(), e.getAfpAdministradora(), e.getNumeroIsss(), e.getIdUsuario(), e.isActivo(),
                especialidades, horarios, usuario);
    }

    private Map<String, Object> datosBitacora(EmpleadoFichaDTO f) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombreEmpleado", f.nombreEmpleado());
        datos.put("apellido", f.apellido());
        datos.put("dui", f.dui());
        datos.put("telefono", f.telefono());
        datos.put("correoEmpleado", f.correoEmpleado());
        datos.put("puesto", f.puesto());
        datos.put("fechaIngreso", Objects.toString(f.fechaIngreso(), null));
        datos.put("fechaBaja", Objects.toString(f.fechaBaja(), null));
        datos.put("sueldoBaseMensual", f.sueldoBaseMensual());
        datos.put("recibeComision", f.recibeComision());
        datos.put("afpAdministradora", f.afpAdministradora());
        datos.put("nupAfp", f.nupAfp());
        datos.put("numeroIsss", f.numeroIsss());
        datos.put("activo", f.activo());
        datos.put("especialidades", nombresEspecialidades(f.especialidades()));
        for (short dia = 1; dia < DIAS.length; dia++) {
            datos.put(DIAS[dia], horarioDelDia(f.horarios(), dia));
        }
        return datos;
    }

    private String nombresEspecialidades(List<Integer> ids) {
        if (ids.isEmpty()) return null;
        return especialidadRepo.findAllById(ids).stream()
                .map(EspecialidadEntity::getNombreEspecialidad)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private static String horarioDelDia(List<HorarioEmpleadoDTO> horarios, short dia) {
        String texto = horarios.stream()
                .filter(h -> h.diaSemana() == dia)
                .sorted(Comparator.comparing(HorarioEmpleadoDTO::horaInicio))
                .map(h -> h.horaInicio() + "–" + h.horaFin())
                .collect(Collectors.joining(" y "));
        return texto.isEmpty() ? null : texto;
    }

    private static String nombreCompleto(EmpleadoEntity e) {
        return e.getApellido() == null ? e.getNombreEmpleado() : e.getNombreEmpleado() + " " + e.getApellido();
    }

    private static OperacionInvalidaException errorDeCampo(String campo, String mensaje) {
        return new OperacionInvalidaException(mensaje, "DATOS_INVALIDOS", Map.of(campo, mensaje));
    }
}