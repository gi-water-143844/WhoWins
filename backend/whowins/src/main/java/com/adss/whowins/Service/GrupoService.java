package com.adss.whowins.Service;

import com.adss.whowins.DTO.CriarGrupoRequest;
import com.adss.whowins.DTO.DuracaoRequest;
import com.adss.whowins.Exception.DadosInvalidosException;
import com.adss.whowins.Exception.UsuarioNaoAutenticadoException;
import com.adss.whowins.Model.*;
import com.adss.whowins.Repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class GrupoService {

    // Lista provisória: deve ser a mesma do front até o time confirmar a oficial
    private static final Set<String> CATEGORIAS =
            Set.of("Exercício físico", "Estudos", "Alimentação", "Leitura", "Bem-estar", "Outro");
    private static final String CATEGORIA_OUTRO = "Outro";
    private static final Set<String> UNIDADES = Set.of("dias", "semanas", "meses");

    private final GrupoDesafioRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParticipacaoRepository participacaoRepository;

    public GrupoService(GrupoDesafioRepository grupoRepository,
                        UsuarioRepository usuarioRepository,
                        ParticipacaoRepository participacaoRepository) {
        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
        this.participacaoRepository = participacaoRepository;
    }

    @Transactional
    public GrupoDesafio criar(Long usuarioId, CriarGrupoRequest req) {
        // RN-010: usuário precisa estar autenticado
        Usuario criador = buscarUsuarioAutenticado(usuarioId);

        validar(req);

        LocalDate inicio = LocalDate.now();
        LocalDate fim = calcularDataFim(inicio, req.duracao());

        boolean categoriaOutro = CATEGORIA_OUTRO.equals(req.categoria());
        CriterioPontuacao criterio = new CriterioPontuacao(
                req.criteriosPontuacao().trim(),
                categoriaOutro ? req.formaQuantificacao().trim() : null);

        GrupoDesafio grupo = new GrupoDesafio(
                req.nome().trim(),
                req.atividade().trim(),
                req.categoria(),
                inicio,
                fim,
                converterVisibilidade(req.visibilidade()),
                req.limiteParticipantes(),
                vazio(req.premiacao()) ? null : req.premiacao().trim(),
                criador,
                criterio);

        grupo = grupoRepository.save(grupo);

        // Pós-condição do UC-002: o usuário vira criador e dono do grupo
        participacaoRepository.save(new Participacao(criador, grupo, Participacao.CRIADOR));

        return grupo;
    }

    private Usuario buscarUsuarioAutenticado(Long usuarioId) {
        if (usuarioId == null) {
            throw new UsuarioNaoAutenticadoException("É necessário estar autenticado para criar um grupo.");
        }
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNaoAutenticadoException("Usuário não autenticado."));
    }

    // Coleta TODOS os erros de uma vez para o front destacar cada campo
    private void validar(CriarGrupoRequest r) {
        Map<String, String> erros = new LinkedHashMap<>();

        if (vazio(r.nome())) {
            erros.put("nome", "Informe o nome do grupo.");
        }
        if (vazio(r.atividade())) {
            erros.put("atividade", "Descreva a atividade ou o hábito do desafio.");
        }
        if (vazio(r.categoria())) {
            erros.put("categoria", "Selecione uma categoria.");
        } else if (!CATEGORIAS.contains(r.categoria())) {
            erros.put("categoria", "Categoria inválida.");
        }

        DuracaoRequest d = r.duracao();
        if (d == null || d.quantidade() == null) {
            erros.put("duracao", "Informe a duração do desafio.");
        } else if (d.quantidade() < 1) {
            erros.put("duracao", "Use um número inteiro maior que zero.");
        } else if (d.unidade() == null || !UNIDADES.contains(d.unidade())) {
            erros.put("duracao", "Unidade de tempo inválida.");
        }

        if (vazio(r.criteriosPontuacao())) {
            erros.put("criteriosPontuacao", "Descreva os critérios de pontuação.");
        }
        // RN-001: forma de quantificação obrigatória se a categoria for "Outro"
        if (CATEGORIA_OUTRO.equals(r.categoria()) && vazio(r.formaQuantificacao())) {
            erros.put("formaQuantificacao", "Informe como o progresso será medido.");
        }
        if (converterVisibilidade(r.visibilidade()) == null) {
            erros.put("visibilidade", "Escolha entre público e privado.");
        }
        // RN-012: limite é opcional, mas se vier precisa ser >= 1
        if (r.limiteParticipantes() != null && r.limiteParticipantes() < 1) {
            erros.put("limiteParticipantes", "Use um número inteiro maior que zero ou deixe em branco.");
        }

        if (!erros.isEmpty()) {
            throw new DadosInvalidosException(erros);
        }
    }

    private LocalDate calcularDataFim(LocalDate inicio, DuracaoRequest d) {
        int q = d.quantidade();
        return switch (d.unidade()) {
            case "dias" -> inicio.plusDays(q);
            case "semanas" -> inicio.plusWeeks(q);
            case "meses" -> inicio.plusMonths(q);
            default -> throw new IllegalStateException("Unidade inesperada: " + d.unidade());
        };
    }

    private Visibilidade converterVisibilidade(String valor) {
        if (valor == null) return null;
        try {
            return Visibilidade.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}