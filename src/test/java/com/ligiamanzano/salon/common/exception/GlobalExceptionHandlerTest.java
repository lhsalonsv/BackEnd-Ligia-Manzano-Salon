package com.ligiamanzano.salon.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @RestController
    @RequestMapping("/api/prueba")
    static class PruebaController {

        @GetMapping("/{id}")
        String porId(@PathVariable int id) {
            return "ok";
        }

        @GetMapping("/lista")
        String lista(@RequestParam int pagina) {
            return "ok";
        }

        @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        String json(@RequestBody String cuerpo) {
            return "ok";
        }

        @GetMapping("/falla")
        String falla() {
            throw new IllegalStateException("detalle interno");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new PruebaController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rutaQueNoExisteResponde404() throws Exception {
        mvc.perform(get("/api/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void metodoEquivocadoResponde405() throws Exception {
        mvc.perform(delete("/api/prueba/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("METODO_NO_PERMITIDO"));
    }

    @Test
    void idQueNoEsNumeroResponde400ConElCampo() throws Exception {
        mvc.perform(get("/api/prueba/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.campos.id").exists());
    }

    @Test
    void parametroFaltanteResponde400() throws Exception {
        mvc.perform(get("/api/prueba/lista"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PETICION_INVALIDA"));
    }

    @Test
    void cuerpoQueNoEsJsonResponde415() throws Exception {
        mvc.perform(post("/api/prueba/json").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value("TIPO_NO_SOPORTADO"));
    }

    @Test
    void errorInesperadoResponde500SinDetalles() throws Exception {
        mvc.perform(get("/api/prueba/falla"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error. Intenta de nuevo."));
    }
}
