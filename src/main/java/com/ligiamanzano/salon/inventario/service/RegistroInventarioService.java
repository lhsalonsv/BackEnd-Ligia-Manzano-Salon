package com.ligiamanzano.salon.inventario.service;

import com.ligiamanzano.salon.common.exception.RecursoNoEncontradoException;
import com.ligiamanzano.salon.common.util.Periodo;
import com.ligiamanzano.salon.configuracion.service.EmisorService;
import com.ligiamanzano.salon.inventario.dto.MovimientoRegistroDTO;
import com.ligiamanzano.salon.inventario.dto.ProductoRegistroDTO;
import com.ligiamanzano.salon.inventario.dto.RegistroInventarioDTO;
import com.ligiamanzano.salon.inventario.dto.SaldoDTO;
import com.ligiamanzano.salon.inventario.repository.ProductoRepository;
import com.ligiamanzano.salon.inventario.repository.RegistroInventarioRepository;
import com.ligiamanzano.salon.inventario.repository.RegistroInventarioRepository.Fila;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegistroInventarioService {

    private final RegistroInventarioRepository repo;
    private final ProductoRepository productos;
    private final EmisorService emisor;

    public RegistroInventarioDTO obtener(String periodo, Integer idProducto) {
        Periodo p = Periodo.desde(periodo);
        if (idProducto != null && !productos.existsById(idProducto)) {
            throw new RecursoNoEncontradoException("No se encontró el producto.");
        }
        LocalDate desde = p.primerDia();
        LocalDate hasta = p.primerDiaDelSiguiente();
        Map<Integer, List<MovimientoRegistroDTO>> movimientos = repo.movimientos(desde, hasta, idProducto).stream()
                .collect(Collectors.groupingBy(Fila::idProducto,
                        Collectors.mapping(Fila::movimiento, Collectors.toList())));
        List<ProductoRegistroDTO> lista = repo.productos(desde, hasta, idProducto).stream()
                .map(e -> new ProductoRegistroDTO(e.idProducto(), e.codigo(), e.nombreProducto(), e.marca(), e.unidadMedida(),
                        new SaldoDTO(e.unidadesIniciales(), e.valorInicial()),
                        movimientos.getOrDefault(e.idProducto(), List.of())))
                .toList();
        return new RegistroInventarioDTO(emisor.encabezado(), p.texto(), lista);
    }
}
