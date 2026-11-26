package com.ligiamanzano.salon.compras.repository;

import com.ligiamanzano.salon.compras.entity.CompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompraRepository extends JpaRepository<CompraEntity, Integer> {

    Optional<CompraEntity> findFirstByIdProveedorAndNumeroDocumentoProveedorIgnoreCaseAndEstadoNot(
            Integer idProveedor, String numeroDocumentoProveedor, String estado);

    Optional<CompraEntity> findFirstByCodigoGeneracionProveedorAndEstadoNot(UUID codigoGeneracionProveedor, String estado);
}
