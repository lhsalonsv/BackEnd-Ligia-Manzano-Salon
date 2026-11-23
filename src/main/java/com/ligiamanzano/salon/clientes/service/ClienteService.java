package com.ligiamanzano.salon.clientes.service;

import com.ligiamanzano.salon.bitacora.service.BitacoraService;
import com.ligiamanzano.salon.catalogos.service.CatalogoService;
import com.ligiamanzano.salon.clientes.dto.ClienteFichaDTO;
import com.ligiamanzano.salon.clientes.dto.ClienteListaDTO;
import com.ligiamanzano.salon.clientes.dto.ClienteRequest;
import com.ligiamanzano.salon.clientes.dto.ClientesPaginaDTO;
import com.ligiamanzano.salon.clientes.entity.ClienteEntity;
import com.ligiamanzano.salon.clientes.repository.ClienteConsultaRepository;
import com.ligiamanzano.salon.clientes.repository.ClienteConsultaRepository.Estadisticas;
import com.ligiamanzano.salon.clientes.repository.ClienteConsultaRepository.Filtro;
import com.ligiamanzano.salon.clientes.repository.ClienteRepository;
import com.ligiamanzano.salon.common.exception.ConflictoException;
import com.ligiamanzano.salon.common.exception.OperacionInvalidaException;
import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.response.PaginaDTO;
import com.ligiamanzano.salon.common.util.Fechas;
import com.ligiamanzano.salon.common.util.Texto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClienteService {

    private static final String ENTIDAD = "CLIENTE";
    private static final String TIPO_DUI = "13";
    private static final int ANIO_BISIESTO = 2024;

    private final ClienteRepository repo;
    private final ClienteConsultaRepository consultas;
    private final CatalogoService catalogos;
    private final BitacoraService bitacora;

    public ClientesPaginaDTO listar(String buscar, String filtro, int pagina, int tamano) {
        String texto = Texto.limpio(buscar);
        String digitos = Texto.soloDigitos(texto);
        Filtro elegido = Filtro.desde(filtro);
        Map<String, Long> conteos = consultas.conteos(texto, digitos);
        List<ClienteListaDTO> contenido = consultas.pagina(
                texto, digitos, elegido, Math.max(pagina, 0), PaginaDTO.tamanoSeguro(tamano));
        return new ClientesPaginaDTO(contenido, conteos.get(elegido.clave()), conteos);
    }

    public ClienteFichaDTO obtener(int idCliente) {
        return convertirADTO(buscar(idCliente));
    }

    @Transactional
    public ClienteFichaDTO crear(ClienteRequest dto) {
        validar(dto, null);
        ClienteEntity c = new ClienteEntity();
        convertirAEntity(dto, c);
        c.setActivo(true);
        c.setCreadoEn(Instant.now());
        repo.save(c);
        bitacora.registrarCambios(BitacoraService.CLIENTES, "CREAR", ENTIDAD, c.getIdCliente(),
                "Registró como cliente a " + nombreCompleto(c), null, datosBitacora(c));
        return convertirADTO(c);
    }

    @Transactional
    public ClienteFichaDTO actualizar(int idCliente, ClienteRequest dto) {
        ClienteEntity c = buscar(idCliente);
        validar(dto, idCliente);
        Map<String, Object> antes = datosBitacora(c);
        convertirAEntity(dto, c);
        bitacora.registrarCambios(BitacoraService.CLIENTES, "EDITAR", ENTIDAD, idCliente,
                "Editó los datos de " + nombreCompleto(c), antes, datosBitacora(c));
        return convertirADTO(c);
    }

    private ClienteEntity buscar(int idCliente) {
        return repo.findById(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("El cliente no existe."));
    }

    private void validar(ClienteRequest dto, Integer idActual) {
        Map<String, String> campos = new LinkedHashMap<>();
        validarCumple(dto, campos);
        String numDocumento = Texto.limpio(dto.numDocumento());
        String tipoDocumento = Texto.limpio(dto.tipoDocumento());
        if (numDocumento != null && tipoDocumento != null && !TIPO_DUI.equals(tipoDocumento)) {
            campos.put("numDocumento", "Por ahora solo se acepta DUI.");
        }
        validarCreditoFiscal(dto, campos);
        if (!campos.isEmpty()) {
            throw new OperacionInvalidaException("Revisa los campos marcados", "DATOS_INVALIDOS", campos);
        }
        if (numDocumento != null) {
            exigirDocumentoLibre(numDocumento, idActual);
        }
    }

    private static void validarCumple(ClienteRequest dto, Map<String, String> campos) {
        Short dia = dto.diaCumple();
        Short mes = dto.mesCumple();
        if ((dia == null) != (mes == null)) {
            campos.put("diaCumple", "Elige el día y el mes, o deja los dos vacíos.");
        } else if (dia != null && dia > YearMonth.of(ANIO_BISIESTO, mes).lengthOfMonth()) {
            campos.put("diaCumple", "Ese día no existe en ese mes.");
        }
    }

    private void validarCreditoFiscal(ClienteRequest dto, Map<String, String> campos) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("razonSocial", Texto.limpio(dto.razonSocial()));
        datos.put("nit", Texto.limpio(dto.nit()));
        datos.put("nrc", Texto.limpio(dto.nrc()));
        datos.put("codActividad", Texto.limpio(dto.codActividad()));
        datos.put("idDistrito", dto.idDistrito());
        datos.put("complemento", Texto.limpio(dto.complemento()));

        long presentes = datos.values().stream().filter(Objects::nonNull).count();
        if (presentes > 0 && presentes < datos.size()) {
            datos.forEach((campo, valor) -> {
                if (valor == null) campos.put(campo, "Obligatorio para crédito fiscal.");
            });
        }
        String codActividad = (String) datos.get("codActividad");
        if (codActividad != null && !catalogos.existeActividad(codActividad)) {
            campos.putIfAbsent("codActividad", "No está en el catálogo de Hacienda.");
        }
        if (dto.idDistrito() != null && !catalogos.existeDistrito(dto.idDistrito())) {
            campos.putIfAbsent("idDistrito", "No está en el catálogo de Hacienda.");
        }
    }

    private void exigirDocumentoLibre(String numDocumento, Integer idActual) {
        repo.findFirstByTipoDocumentoAndNumDocumento(TIPO_DUI, numDocumento)
                .filter(otro -> !otro.getIdCliente().equals(idActual))
                .ifPresent(otro -> {
                    throw new ConflictoException(
                            "Ese DUI ya es de " + nombreCompleto(otro) + (otro.isActivo() ? "." : " (inactivo)."),
                            "CLIENTE_DUPLICADO",
                            Map.of("numDocumento", "Ya existe un cliente con este DUI."),
                            Map.of("idClienteExistente", otro.getIdCliente()));
                });
    }

    private static void convertirAEntity(ClienteRequest dto, ClienteEntity c) {
        String numDocumento = Texto.limpio(dto.numDocumento());
        c.setNombre(Texto.limpio(dto.nombre()));
        c.setApellido(Texto.limpio(dto.apellido()));
        c.setTelefono(Texto.limpio(dto.telefono()));
        c.setCorreo(Texto.minusculas(dto.correo()));
        c.setDiaCumple(dto.diaCumple());
        c.setMesCumple(dto.mesCumple());
        c.setAlergias(Texto.limpio(dto.alergias()));
        c.setNumDocumento(numDocumento);
        c.setTipoDocumento(numDocumento == null ? null : TIPO_DUI);
        c.setRazonSocial(Texto.limpio(dto.razonSocial()));
        c.setNombreComercial(Texto.limpio(dto.nombreComercial()));
        c.setNit(Texto.limpio(dto.nit()));
        c.setNrc(Texto.limpio(dto.nrc()));
        c.setCodActividad(Texto.limpio(dto.codActividad()));
        c.setIdDistrito(dto.idDistrito());
        c.setComplemento(Texto.limpio(dto.complemento()));
    }

    private ClienteFichaDTO convertirADTO(ClienteEntity c) {
        int id = c.getIdCliente();
        Estadisticas s = consultas.estadisticas(id);
        return new ClienteFichaDTO(
                id, c.getNombre(), c.getApellido(), c.getTelefono(), c.getCorreo(),
                c.getDiaCumple(), c.getMesCumple(), c.getAlergias(),
                c.getTipoDocumento(), c.getNumDocumento(),
                c.getRazonSocial(), c.getNit(), c.getNrc(), c.getNombreComercial(),
                c.getCodActividad(), c.getIdDistrito(), c.getComplemento(),
                c.isActivo(), Fechas.local(c.getCreadoEn()),
                s.ultimaVisita(), s.visitas12m(), s.gastado12m(),
                consultas.empleadoFrecuente(id),
                consultas.ultimaFormula(id).orElse(null),
                consultas.ultimasVisitas(id),
                consultas.proximaCita(id).orElse(null));
    }

    private static Map<String, Object> datosBitacora(ClienteEntity c) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", c.getNombre());
        datos.put("apellido", c.getApellido());
        datos.put("telefono", c.getTelefono());
        datos.put("correo", c.getCorreo());
        datos.put("diaCumple", c.getDiaCumple());
        datos.put("mesCumple", c.getMesCumple());
        datos.put("alergias", c.getAlergias());
        datos.put("tipoDocumento", c.getTipoDocumento());
        datos.put("numDocumento", c.getNumDocumento());
        datos.put("razonSocial", c.getRazonSocial());
        datos.put("nombreComercial", c.getNombreComercial());
        datos.put("nit", c.getNit());
        datos.put("nrc", c.getNrc());
        datos.put("codActividad", c.getCodActividad());
        datos.put("idDistrito", c.getIdDistrito());
        datos.put("complemento", c.getComplemento());
        return datos;
    }

    private static String nombreCompleto(ClienteEntity c) {
        return c.getApellido() == null ? c.getNombre() : c.getNombre() + " " + c.getApellido();
    }
}