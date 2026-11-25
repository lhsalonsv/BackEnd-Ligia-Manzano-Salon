package com.ligiamanzano.salon.inventario.service;

import com.ligiamanzano.salon.inventario.dto.CategoriaDTO;
import com.ligiamanzano.salon.inventario.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repo;

    public List<CategoriaDTO> listar() {
        return repo.findAllByOrderByNombreCategoriaAsc().stream()
                .map(c -> new CategoriaDTO(c.getIdCategoria(), c.getNombreCategoria()))
                .toList();
    }

    public boolean existe(Integer idCategoria) {
        return idCategoria != null && repo.existsById(idCategoria);
    }
}