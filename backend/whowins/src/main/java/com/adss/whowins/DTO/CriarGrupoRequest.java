package com.adss.whowins.DTO;

public record CriarGrupoRequest(
                String nome,
                String atividade,
                String categoria,
                DuracaoRequest duracao,
                String criteriosPontuacao,
                String formaQuantificacao,
                String visibilidade,
                Integer limiteParticipantes,
                String premiacao
                ) {}
