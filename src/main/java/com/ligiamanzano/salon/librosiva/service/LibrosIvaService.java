package com.ligiamanzano.salon.librosiva.service;

import com.ligiamanzano.salon.common.util.Periodo;
import com.ligiamanzano.salon.configuracion.service.EmisorService;
import com.ligiamanzano.salon.librosiva.dto.LibrosIvaDTO;
import com.ligiamanzano.salon.librosiva.dto.PendientesDTO;
import com.ligiamanzano.salon.librosiva.dto.ResumenF07DTO;
import com.ligiamanzano.salon.librosiva.repository.LibrosIvaRepository;
import com.ligiamanzano.salon.librosiva.repository.LibrosIvaRepository.Anexo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LibrosIvaService {

    private final LibrosIvaRepository repo;
    private final EmisorService emisor;

    public LibrosIvaDTO obtener(String periodo) {
        Periodo p = Periodo.desde(periodo);
        LocalDate mes = p.primerDia();
        return new LibrosIvaDTO(
                p.texto(),
                emisor.encabezado(),
                repo.filas(Anexo.VENTAS_CONTRIBUYENTES, mes),
                repo.filas(Anexo.CONSUMIDOR_FINAL, mes),
                repo.filas(Anexo.COMPRAS, mes),
                repo.filas(Anexo.ANULADOS, mes),
                new PendientesDTO(repo.sinSello(mes, p.primerDiaDelSiguiente())));
    }

    public ResumenF07DTO resumenF07(String periodo) {
        return repo.resumenF07(Periodo.desde(periodo).mes());
    }
}
