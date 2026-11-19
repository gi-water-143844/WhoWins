package com.adss.whowins.Controller;

import com.adss.whowins.DTO.CriarGrupoRequest;
import com.adss.whowins.DTO.GrupoResponse;
import com.adss.whowins.Model.GrupoDesafio;
import com.adss.whowins.Service.GrupoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/grupos")
public class GrupoController {

    private final GrupoService grupoService;

    public GrupoController(GrupoService grupoService) {
        this.grupoService = grupoService;
    }

    @PostMapping
    public ResponseEntity<GrupoResponse> criar(
            @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
            @RequestBody CriarGrupoRequest request) {
        GrupoDesafio grupo = grupoService.criar(usuarioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GrupoResponse.de(grupo));
    }
}
