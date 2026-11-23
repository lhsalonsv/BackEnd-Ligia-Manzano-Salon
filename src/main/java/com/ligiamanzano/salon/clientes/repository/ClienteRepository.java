package com.ligiamanzano.salon.clientes.repository;

import com.ligiamanzano.salon.clientes.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<ClienteEntity, Integer> {

    Optional<ClienteEntity> findFirstByTipoDocumentoAndNumDocumento(String tipoDocumento, String numDocumento);
}
