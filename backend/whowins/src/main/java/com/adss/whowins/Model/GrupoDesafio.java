package com.adss.whowins.Model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class GrupoDesafio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String descricao; // editada depois, no UC-015

    @Column(length = 1000)
    private String atividade;

    private String categoria;
    private LocalDate dataInicio;
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    private Visibilidade visibilidade;

    @Enumerated(EnumType.STRING)
    private StatusGrupo status;

    private Integer limiteParticipantes; // null = sem limite (RN-012)

    @Column(length = 1000)
    private String premiacao;

    private String linkConvite; // gerado no UC-021

    @ManyToOne(optional = false)
    @JoinColumn(name = "criador_id")
    private Usuario criador;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "criterio_id")
    private CriterioPontuacao criterioPontuacao;

    protected GrupoDesafio() {}

    public GrupoDesafio(String nome, String atividade, String categoria,
                        LocalDate dataInicio, LocalDate dataFim,
                        Visibilidade visibilidade, Integer limiteParticipantes,
                        String premiacao, Usuario criador,
                        CriterioPontuacao criterioPontuacao) {
        this.nome = nome;
        this.atividade = atividade;
        this.categoria = categoria;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.visibilidade = visibilidade;
        this.limiteParticipantes = limiteParticipantes;
        this.premiacao = premiacao;
        this.criador = criador;
        this.criterioPontuacao = criterioPontuacao;
        this.status = StatusGrupo.ATIVO;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getAtividade() { return atividade; }
    public String getCategoria() { return categoria; }
    public LocalDate getDataInicio() { return dataInicio; }
    public LocalDate getDataFim() { return dataFim; }
    public Visibilidade getVisibilidade() { return visibilidade; }
    public StatusGrupo getStatus() { return status; }
    public Integer getLimiteParticipantes() { return limiteParticipantes; }
    public String getPremiacao() { return premiacao; }
    public Usuario getCriador() { return criador; }
    public CriterioPontuacao getCriterioPontuacao() { return criterioPontuacao; }
}
