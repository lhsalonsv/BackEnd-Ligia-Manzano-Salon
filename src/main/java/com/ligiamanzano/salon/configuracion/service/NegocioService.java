package com.ligiamanzano.salon.configuracion.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.catalogos.service.CatalogoService;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.configuracion.dto.NegocioDTO;
import com.ligiamanzano.salon.configuracion.dto.NegocioRequest;
import com.ligiamanzano.salon.configuracion.dto.UltimoCambioDTO;
import com.ligiamanzano.salon.configuracion.entity.EmisorEntity;
import com.ligiamanzano.salon.configuracion.repository.ConfiguracionConsultaRepository;
import com.ligiamanzano.salon.configuracion.repository.EmisorRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NegocioService {

    public static final String ENTIDAD = "NEGOCIO";
    private static final String AMBIENTE_PRUEBAS = "00";

    private final EmisorRepository repo;
    private final ConfiguracionConsultaRepository consultas;
    private final CatalogoService catalogos;
    private final BitacoraService bitacora;
    private final EntityManager entityManager;

    public NegocioDTO obtener() {
        return repo.findFirstByOrderByIdEmisorAsc()
                .map(this::convertirADTO)
                .orElseGet(() -> new NegocioDTO(null, null, null, null, null, null, null, null, null, null,
                        null, null, AMBIENTE_PRUEBAS, null, consultas.ultimoEnvio(), ultimoCambio()));
    }

    @Transactional
    public NegocioDTO actualizar(NegocioRequest dto) {
        validarCatalogos(dto);
        EmisorEntity emisor = repo.findFirstByOrderByIdEmisorAsc().orElse(null);
        boolean nuevo = emisor == null;
        Map<String, Object> antes = nuevo ? null : datos(emisor);
        if (nuevo) {
            emisor = new EmisorEntity();
            emisor.setActivo(true);
        }
        convertirAEntity(dto, emisor);
        repo.saveAndFlush(emisor);
        entityManager.refresh(emisor);

        bitacora.registrarCambios(BitacoraService.CONFIGURACION, nuevo ? "CREAR" : "EDITAR", ENTIDAD, emisor.getIdEmisor(),
                (nuevo ? "Registró" : "Editó") + " los datos del negocio: " + emisor.getNombre(),
                antes, datos(emisor));
        entityManager.flush();
        return convertirADTO(emisor);
    }

    private void validarCatalogos(NegocioRequest dto) {
        Map<String, String> campos = new LinkedHashMap<>();
        if (!catalogos.existeActividad(Texto.limpio(dto.codActividad()))) {
            campos.put("codActividad", "No está en el catálogo de actividades.");
        }
        if (!catalogos.existeDistrito(dto.idDistrito())) {
            campos.put("idDistrito", "No está en el catálogo de distritos.");
        }
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }
    }

    private void convertirAEntity(NegocioRequest dto, EmisorEntity e) {
        e.setNombreComercial(Texto.limpio(dto.nombreComercial()));
        e.setNombre(Texto.limpio(dto.nombre()));
        e.setNit(dto.nit().trim());
        e.setNrc(dto.nrc().trim());
        e.setCodActividad(Texto.limpio(dto.codActividad()));
        e.setTipoEstablecimiento(dto.tipoEstablecimiento().trim());
        e.setIdDistrito(dto.idDistrito());
        e.setComplemento(Texto.limpio(dto.complemento()));
        e.setTelefono(Texto.limpio(dto.telefono()));
        e.setCorreo(Texto.limpio(dto.correo()).toLowerCase(Locale.ROOT));
        e.setCodEstable(dto.codEstable().trim().toUpperCase(Locale.ROOT));
        e.setCodPuntoVenta(dto.codPuntoVenta().trim().toUpperCase(Locale.ROOT));
    }

    private NegocioDTO convertirADTO(EmisorEntity e) {
        return new NegocioDTO(
                e.getNombreComercial(),
                e.getNombre(),
                e.getNit(),
                e.getNrc(),
                e.getCodActividad(),
                e.getTipoEstablecimiento(),
                e.getIdDistrito(),
                e.getComplemento(),
                e.getTelefono(),
                e.getCorreo(),
                e.getCodEstable(),
                e.getCodPuntoVenta(),
                e.getAmbiente() == null ? AMBIENTE_PRUEBAS : e.getAmbiente(),
                e.getCertificadoVence(),
                consultas.ultimoEnvio(),
                ultimoCambio());
    }

    private Map<String, Object> datos(EmisorEntity e) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombreComercial", e.getNombreComercial());
        datos.put("nombre", e.getNombre());
        datos.put("nit", e.getNit());
        datos.put("nrc", e.getNrc());
        datos.put("codActividad", e.getCodActividad());
        datos.put("tipoEstablecimiento", e.getTipoEstablecimiento());
        datos.put("idDistrito", e.getIdDistrito());
        datos.put("complemento", e.getComplemento());
        datos.put("telefono", e.getTelefono());
        datos.put("correo", e.getCorreo());
        datos.put("codEstable", e.getCodEstable());
        datos.put("codPuntoVenta", e.getCodPuntoVenta());
        return datos;
    }

    private UltimoCambioDTO ultimoCambio() {
        return consultas.ultimoCambio(BitacoraService.CONFIGURACION, ENTIDAD);
    }
}
