package com.ligiamanzano.salon.configuracion.repository;

import com.ligiamanzano.salon.configuracion.entity.FormaPagoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormaPagoRepository extends JpaRepository<FormaPagoEntity, Short> {

    List<FormaPagoEntity> findAllByOrderByIdFormaPagoAsc();
}
