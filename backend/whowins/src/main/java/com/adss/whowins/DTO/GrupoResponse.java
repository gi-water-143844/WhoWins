package com.adss.whowins.DTO;

import com.adss.whowins.Model.*;
import java.time.LocalDate;

public record GrupoResponse(
        Long id,
        String nome,
        String atividade,
        String categoria,
        LocalDate dataInicio,
        LocalDate dataFim,
        String criteriosPontuacao,
        String formaQuantificacao,
        Visibilidade visibilidade,
        StatusGrupo status,
        Integer limiteParticipantes,
        String premiacao,
        Long criadorId
) {
    public static GrupoResponse de(GrupoDesafio g) {
        return new GrupoResponse(
                g.getId(), g.getNome(), g.getAtividade(), g.getCategoria(),
                g.getDataInicio(), g.getDataFim(),
                g.getCriterioPontuacao().getDescricao(),
                g.getCriterioPontuacao().getUnidadeMedida(),
                g.getVisibilidade(), g.getStatus(),
                g.getLimiteParticipantes(), g.getPremiacao(),
                g.getCriador().getId());
    }
}