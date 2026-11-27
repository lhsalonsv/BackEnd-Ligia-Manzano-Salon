package com.ligiamanzano.salon.contabilidad.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.response.PaginaDTO;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.configuracion.service.EmisorService;
import com.ligiamanzano.salon.contabilidad.dto.*;
import com.ligiamanzano.salon.contabilidad.entity.PartidaDetalleEntity;
import com.ligiamanzano.salon.contabilidad.entity.PartidaEntity;
import com.ligiamanzano.salon.contabilidad.repository.PartidaConsultaRepository;
import com.ligiamanzano.salon.contabilidad.repository.PartidaConsultaRepository.Encabezado;
import com.ligiamanzano.salon.contabilidad.repository.PartidaConsultaRepository.Filtro;
import com.ligiamanzano.salon.contabilidad.repository.PartidaDetalleRepository;
import com.ligiamanzano.salon.contabilidad.repository.PartidaRepository;
import com.ligiamanzano.salon.security.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PartidaService {

    private static final String ENTIDAD = "PARTIDA";
    private static final String APLICADA = "APLICADA";
    private static final int LARGO_CONCEPTO = 300;
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PartidaRepository repo;
    private final PartidaDetalleRepository detalleRepo;
    private final PartidaConsultaRepository consultas;
    private final CuentaContableService cuentas;
    private final TipoPartidaService tipos;
    private final EmisorService emisor;
    private final BitacoraService bitacora;

    public PaginaDTO<PartidaListaDTO> listar(Short anio, Short mes, String tipo, String estado, String buscar,
                                             int pagina, int tamano) {
        Filtro filtro = new Filtro(anio, mes, Texto.limpio(tipo),
                Texto.limpio(estado) == null ? null : estado.trim().toUpperCase(Locale.ROOT), Texto.limpio(buscar));
        return new PaginaDTO<>(consultas.pagina(filtro, Math.max(pagina, 0), PaginaDTO.tamanoSeguro(tamano)),
                consultas.contar(filtro));
    }

    public PartidaDetalleDTO obtener(int idPartida) {
        Encabezado e = consultas.encabezado(idPartida)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la partida."));
        return new PartidaDetalleDTO(e.partida(), e.nombreTipo(), consultas.lineas(idPartida), emisor.encabezado());
    }

    @Transactional
    public PartidaDetalleDTO crear(PartidaRequest dto) {
        validar(dto);
        PartidaEntity p = new PartidaEntity();
        p.setIdUsuarioCrea(UsuarioActual.id());
        convertirAEntity(dto, p);
        repo.saveAndFlush(p);
        guardarLineas(p.getIdPartida(), dto.lineas());
        bitacora.registrar(BitacoraService.CONTABILIDAD, "CREAR", ENTIDAD, p.getIdPartida(),
                "Creó el borrador " + p.getCodigoTipo() + " del " + DIA.format(p.getFecha()) + ": " + p.getConcepto());
        return obtener(p.getIdPartida());
    }

    @Transactional
    public PartidaDetalleDTO actualizar(int idPartida, PartidaRequest dto) {
        PartidaEntity p = buscar(idPartida);
        if (APLICADA.equals(p.getEstado())) {
            throw new ConflictoException("La partida " + p.getNumero()
                    + " está aplicada: no se modifica (art. 139 del Código Tributario)", "PARTIDA_APLICADA", null);
        }
        validar(dto);
        convertirAEntity(dto, p);
        repo.saveAndFlush(p);
        detalleRepo.borrarDeLaPartida(idPartida);
        guardarLineas(idPartida, dto.lineas());
        bitacora.registrar(BitacoraService.CONTABILIDAD, "EDITAR", ENTIDAD, idPartida,
                "Editó el borrador " + p.getCodigoTipo() + " del " + DIA.format(p.getFecha()) + ": " + p.getConcepto());
        return obtener(idPartida);
    }

    @Transactional
    public void eliminar(int idPartida) {
        PartidaEntity p = buscar(idPartida);
        if (APLICADA.equals(p.getEstado())) {
            throw new ConflictoException("La partida " + p.getNumero() + " está aplicada: no se borra (art. 139 del Código Tributario). "
                    + "Anúlela con una partida inversa", "PARTIDA_APLICADA", null);
        }
        repo.delete(p);
        repo.flush();
        bitacora.registrar(BitacoraService.CONTABILIDAD, "BORRAR", ENTIDAD, idPartida,
                "Borró el borrador " + p.getCodigoTipo() + " del " + DIA.format(p.getFecha()) + ": " + p.getConcepto());
    }

    @Transactional
    public NumeroPartidaDTO aplicar(int idPartida) {
        PartidaEntity p = buscar(idPartida);
        String numero = consultas.aplicar(idPartida, UsuarioActual.id());
        bitacora.registrar(BitacoraService.CONTABILIDAD, "APLICAR", ENTIDAD, idPartida,
                "Aplicó la partida " + p.getCodigoTipo() + " " + numero + " del " + DIA.format(p.getFecha()));
        return new NumeroPartidaDTO(numero);
    }

    @Transactional
    public PartidaDetalleDTO anular(int idPartida, AnulacionRequest dto) {
        PartidaEntity p = buscar(idPartida);
        int idInversa = consultas.anular(idPartida, UsuarioActual.id(), Texto.limpio(dto.motivo()), dto.fecha());
        PartidaDetalleDTO inversa = obtener(idInversa);
        bitacora.registrar(BitacoraService.CONTABILIDAD, "ANULAR", ENTIDAD, idPartida,
                "Anuló la partida " + p.getCodigoTipo() + " " + p.getNumero() + " con la " + inversa.partida().numero()
                        + ": " + Texto.limpio(dto.motivo()));
        return inversa;
    }

    private PartidaEntity buscar(int idPartida) {
        return repo.findById(idPartida).orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la partida."));
    }

    private void validar(PartidaRequest dto) {
        Map<String, String> campos = new LinkedHashMap<>();
        if (!tipos.existeActivo(dto.codigoTipo())) campos.put("codigoTipo", "Elige un tipo de partida activo.");
        List<LineaPartidaRequest> lineas = dto.lineas() == null ? List.of() : dto.lineas();
        for (int i = 0; i < lineas.size(); i++) {
            String problema = revisarLinea(lineas.get(i));
            if (problema != null) {
                campos.put("lineas", "Línea " + (i + 1) + ": " + problema);
                break;
            }
        }
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }
    }

    private String revisarLinea(LineaPartidaRequest l) {
        if (!cuentas.existe(Texto.limpio(l.codigoCuenta()))) return "elige una cuenta del plan.";
        BigDecimal debe = l.debe() == null ? BigDecimal.ZERO : l.debe();
        BigDecimal haber = l.haber() == null ? BigDecimal.ZERO : l.haber();
        boolean vaAlDebe = debe.signum() > 0;
        boolean vaAlHaber = haber.signum() > 0;
        if (debe.signum() < 0 || haber.signum() < 0 || vaAlDebe == vaAlHaber) {
            return "pon el monto en el debe o en el haber, no en los dos ni en cero.";
        }
        if (debe.scale() > 2 || haber.scale() > 2) return "el monto admite hasta 2 decimales.";
        if (l.concepto() != null && l.concepto().length() > LARGO_CONCEPTO) return "el concepto llega a 300 caracteres.";
        return null;
    }

    private void guardarLineas(int idPartida, List<LineaPartidaRequest> lineas) {
        if (lineas == null) return;
        short numero = 1;
        for (LineaPartidaRequest l : lineas) {
            PartidaDetalleEntity d = new PartidaDetalleEntity();
            d.setIdPartida(idPartida);
            d.setLinea(numero++);
            d.setCodigoCuenta(Texto.limpio(l.codigoCuenta()));
            d.setConcepto(Texto.limpio(l.concepto()));
            d.setDebe(l.debe() == null ? BigDecimal.ZERO : l.debe());
            d.setHaber(l.haber() == null ? BigDecimal.ZERO : l.haber());
            detalleRepo.save(d);
        }
        detalleRepo.flush();
    }

    private static void convertirAEntity(PartidaRequest dto, PartidaEntity p) {
        p.setFecha(dto.fecha());
        p.setAnio((short) dto.fecha().getYear());
        p.setMes((short) dto.fecha().getMonthValue());
        p.setCodigoTipo(dto.codigoTipo());
        p.setConcepto(Texto.limpio(dto.concepto()));
    }
}
