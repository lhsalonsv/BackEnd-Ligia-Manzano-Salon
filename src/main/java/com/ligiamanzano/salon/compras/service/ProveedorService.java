package com.ligiamanzano.salon.compras.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.util.Texto;
import com.ligiamanzano.salon.compras.dto.ProveedorFichaDTO;
import com.ligiamanzano.salon.compras.dto.ProveedorListaDTO;
import com.ligiamanzano.salon.compras.dto.ProveedorRequest;
import com.ligiamanzano.salon.compras.entity.ProveedorEntity;
import com.ligiamanzano.salon.compras.repository.ProveedorConsultaRepository;
import com.ligiamanzano.salon.compras.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProveedorService {

    private static final String ENTIDAD = "PROVEEDOR";
    private static final int ULTIMAS_COMPRAS = 3;

    private final ProveedorRepository repo;
    private final ProveedorConsultaRepository consultas;
    private final BitacoraService bitacora;

    public List<ProveedorListaDTO> listar() {
        return consultas.lista();
    }

    public ProveedorFichaDTO obtener(int idProveedor) {
        return convertirAFicha(buscar(idProveedor));
    }

    @Transactional
    public ProveedorFichaDTO crear(ProveedorRequest dto) {
        validar(dto, null);
        ProveedorEntity p = new ProveedorEntity();
        convertirAEntity(dto, p);
        p.setActivo(true);
        repo.save(p);
        bitacora.registrarCambios(BitacoraService.COMPRAS, "CREAR", ENTIDAD, p.getIdProveedor(),
                "Agregó el proveedor " + nombre(p), null, datosBitacora(p));
        return convertirAFicha(p);
    }

    @Transactional
    public ProveedorFichaDTO actualizar(int idProveedor, ProveedorRequest dto) {
        ProveedorEntity p = buscar(idProveedor);
        validar(dto, idProveedor);
        Map<String, Object> antes = datosBitacora(p);
        convertirAEntity(dto, p);
        bitacora.registrarCambios(BitacoraService.COMPRAS, "EDITAR", ENTIDAD, idProveedor,
                "Editó el proveedor " + nombre(p), antes, datosBitacora(p));
        return convertirAFicha(p);
    }

    @Transactional
    public ProveedorFichaDTO cambiarActivo(int idProveedor, boolean activo) {
        ProveedorEntity p = buscar(idProveedor);
        if (p.isActivo() != activo) {
            p.setActivo(activo);
            bitacora.registrarCambios(BitacoraService.COMPRAS, activo ? "ACTIVAR" : "DESACTIVAR", ENTIDAD, idProveedor,
                    (activo ? "Activó el proveedor " : "Desactivó el proveedor ") + nombre(p),
                    Map.of("activo", !activo), Map.of("activo", activo));
        }
        return convertirAFicha(p);
    }

    //entrega la entidad (activo y días de crédito) a CompraService
    public Optional<ProveedorEntity> buscarParaCompra(Integer idProveedor) {
        return idProveedor == null ? Optional.empty() : repo.findById(idProveedor);
    }

    public static String nombre(ProveedorEntity p) {
        return p.getNombreComercial() != null ? p.getNombreComercial() : p.getRazonSocial();
    }

    private ProveedorEntity buscar(int idProveedor) {
        return repo.findById(idProveedor)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor."));
    }

    private void validar(ProveedorRequest dto, Integer idActual) {
        repo.findFirstByNit(dto.nit().trim())
                .filter(otro -> !otro.getIdProveedor().equals(idActual))
                .ifPresent(otro -> {
                    throw duplicado("Ese NIT ya es de " + nombre(otro) + ".", "nit", "Ya hay un proveedor con este NIT.", otro);
                });
        repo.findFirstByNrc(dto.nrc().trim())
                .filter(otro -> !otro.getIdProveedor().equals(idActual))
                .ifPresent(otro -> {
                    throw duplicado("Ese NRC ya es de " + nombre(otro) + ".", "nrc", "Ya hay un proveedor con este NRC.", otro);
                });
    }

    private static ConflictoException duplicado(String mensaje, String campo, String textoCampo, ProveedorEntity otro) {
        return new ConflictoException(mensaje, "PROVEEDOR_DUPLICADO", Map.of(campo, textoCampo),
                Map.of("idProveedorExistente", otro.getIdProveedor()));
    }

    private static void convertirAEntity(ProveedorRequest dto, ProveedorEntity p) {
        p.setNombreComercial(Texto.limpio(dto.nombreComercial()));
        p.setRazonSocial(Texto.limpio(dto.razonSocial()));
        p.setNit(dto.nit().trim());
        p.setNrc(dto.nrc().trim());
        p.setNacionalidad(Texto.limpio(dto.nacionalidad()));
        p.setContactoNombre(Texto.limpio(dto.contactoNombre()));
        p.setTelefono(Texto.limpio(dto.telefono()));
        p.setCorreo(Texto.minusculas(dto.correo()));
        p.setDireccion(Texto.limpio(dto.direccion()));
        p.setDiasCredito(dto.diasCredito());
        p.setNotas(Texto.limpio(dto.notas()));
    }

    private ProveedorFichaDTO convertirAFicha(ProveedorEntity p) {
        int id = p.getIdProveedor();
        return new ProveedorFichaDTO(id, p.getRazonSocial(), p.getNombreComercial(), p.getNit(), p.getNrc(), p.getNacionalidad(),
                p.getContactoNombre(), p.getTelefono(), p.getCorreo(), p.getDireccion(), p.getDiasCredito(),
                p.getNotas(), p.isActivo(),
                consultas.ultimasCompras(id, ULTIMAS_COMPRAS),
                consultas.comprasDelAnio(id));
    }

    private static Map<String, Object> datosBitacora(ProveedorEntity p) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombreComercial", p.getNombreComercial());
        datos.put("razonSocial", p.getRazonSocial());
        datos.put("nit", p.getNit());
        datos.put("nrc", p.getNrc());
        datos.put("nacionalidad", p.getNacionalidad());
        datos.put("contactoNombre", p.getContactoNombre());
        datos.put("telefono", p.getTelefono());
        datos.put("correo", p.getCorreo());
        datos.put("direccion", p.getDireccion());
        datos.put("diasCredito", p.getDiasCredito());
        datos.put("notas", p.getNotas());
        return datos;
    }
}