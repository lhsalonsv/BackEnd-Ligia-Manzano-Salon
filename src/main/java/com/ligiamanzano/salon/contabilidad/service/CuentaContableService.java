package com.ligiamanzano.salon.contabilidad.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.contabilidad.dto.CuentaContableDTO;
import com.ligiamanzano.salon.contabilidad.dto.CuentaEdicionRequest;
import com.ligiamanzano.salon.contabilidad.dto.CuentaNuevaRequest;
import com.ligiamanzano.salon.contabilidad.entity.CuentaContableEntity;
import com.ligiamanzano.salon.contabilidad.repository.CuentaContableRepository;
import com.ligiamanzano.salon.contabilidad.repository.PartidaDetalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CuentaContableService {

    private static final String ENTIDAD = "CUENTA_CONTABLE";

    private final CuentaContableRepository repo;
    private final PartidaDetalleRepository detalleRepo;
    private final BitacoraService bitacora;

    public List<CuentaContableDTO> listar() {
        return repo.findAllByOrderByCodigoAsc().stream().map(CuentaContableService::convertirADTO).toList();
    }

    @Transactional
    public CuentaContableDTO crear(CuentaNuevaRequest dto) {
        validarNueva(dto);
        CuentaContableEntity c = new CuentaContableEntity();
        c.setCodigo(dto.codigo().trim());
        c.setNombre(Texto.limpio(dto.nombre()));
        c.setTipo(dto.tipo());
        c.setNaturaleza(dto.naturaleza());
        c.setNivel(dto.nivel());
        c.setCodigoPadre(Texto.limpio(dto.codigoPadre()));
        c.setAceptaMovimiento(dto.aceptaMovimiento());
        c.setActiva(true);
        repo.saveAndFlush(c);
        bitacora.registrarCambios(BitacoraService.CONTABILIDAD, "CREAR", ENTIDAD, c.getCodigo(),
                "Agregó la cuenta " + c.getCodigo() + " " + c.getNombre(), null, datosBitacora(c));
        return convertirADTO(c);
    }

    @Transactional
    public CuentaContableDTO actualizar(String codigo, CuentaEdicionRequest dto) {
        CuentaContableEntity c = buscar(codigo);
        if (dto.aceptaMovimiento() && repo.existsByCodigoPadre(codigo)) {
            throw errorDeCampo("aceptaMovimiento", "La cuenta " + codigo + " tiene subcuentas: no puede aceptar movimiento");
        }
        Map<String, Object> antes = datosBitacora(c);
        c.setNombre(Texto.limpio(dto.nombre()));
        c.setAceptaMovimiento(dto.aceptaMovimiento());
        c.setActiva(dto.activa());
        repo.saveAndFlush(c);
        bitacora.registrarCambios(BitacoraService.CONTABILIDAD, "EDITAR", ENTIDAD, codigo,
                "Editó la cuenta " + codigo + " " + c.getNombre(), antes, datosBitacora(c));
        return convertirADTO(c);
    }

    @Transactional
    public void eliminar(String codigo) {
        CuentaContableEntity c = buscar(codigo);
        if (repo.existsByCodigoPadre(codigo)) {
            throw new ConflictoException("La cuenta " + codigo + " tiene subcuentas: desactívela en vez de borrarla.", "CUENTA_EN_USO", null);
        }
        if (detalleRepo.existsByCodigoCuenta(codigo)) {
            throw new ConflictoException("La cuenta " + codigo + " tiene partidas: desactívela en vez de borrarla.", "CUENTA_EN_USO", null);
        }
        repo.delete(c);
        repo.flush();
        bitacora.registrarCambios(BitacoraService.CONTABILIDAD, "BORRAR", ENTIDAD, codigo,
                "Borró la cuenta " + codigo + " " + c.getNombre(), datosBitacora(c), null);
    }

    public String problemaParaUsar(String codigo) {
        CuentaContableEntity c = repo.findById(codigo).orElse(null);
        if (c == null) return "No existe esa cuenta.";
        if (!c.isAceptaMovimiento()) return "La cuenta " + codigo + " no acepta movimiento.";
        return null;
    }

    public boolean existe(String codigo) {
        return codigo != null && repo.existsById(codigo);
    }

    private void validarNueva(CuentaNuevaRequest dto) {
        Map<String, String> campos = new LinkedHashMap<>();
        String codigo = dto.codigo().trim();
        String codigoPadre = Texto.limpio(dto.codigoPadre());
        if (repo.existsById(codigo)) campos.put("codigo", "Ya existe una cuenta con ese código.");
        CuentaContableEntity padre = codigoPadre == null ? null : repo.findById(codigoPadre).orElse(null);
        if (codigoPadre != null && padre == null) {
            campos.put("codigoPadre", "No existe esa cuenta padre.");
        } else if (padre != null && padre.isAceptaMovimiento()) {
            campos.put("codigoPadre", "La cuenta " + padre.getCodigo() + " acepta movimiento: no puede tener subcuentas");
        } else if (padre != null && !(codigo.length() > codigoPadre.length() && codigo.startsWith(codigoPadre))) {
            campos.putIfAbsent("codigo", "El código debe empezar con " + codigoPadre + " y ser más largo.");
        }
        if (codigoPadre == null && dto.nivel() != 0) {
            campos.put("nivel", "Una cuenta sin padre es de nivel 0.");
        } else if (padre != null && dto.nivel() <= padre.getNivel()) {
            campos.put("nivel", "Tiene que ser mayor que el nivel del padre (" + padre.getNivel() + ").");
        }
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }
    }

    private CuentaContableEntity buscar(String codigo) {
        return repo.findById(codigo).orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la cuenta."));
    }

    private static CuentaContableDTO convertirADTO(CuentaContableEntity c) {
        return new CuentaContableDTO(c.getCodigo(), c.getNombre(), c.getTipo(), c.getNaturaleza(), c.getNivel(),
                c.getCodigoPadre(), c.isAceptaMovimiento(), c.isActiva());
    }

    private static Map<String, Object> datosBitacora(CuentaContableEntity c) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", c.getNombre());
        datos.put("tipo", c.getTipo());
        datos.put("naturaleza", c.getNaturaleza());
        datos.put("nivel", c.getNivel());
        datos.put("codigoPadre", c.getCodigoPadre());
        datos.put("aceptaMovimiento", c.isAceptaMovimiento());
        datos.put("activa", c.isActiva());
        return datos;
    }

    private static OperacionInvalidaException errorDeCampo(String campo, String mensaje) {
        return new OperacionInvalidaException(mensaje, "DATOS_INVALIDOS", Map.of(campo, mensaje));
    }
}
