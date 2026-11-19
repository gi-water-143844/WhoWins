package com.adss.whowins.Model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Participacao {

    public static final String CRIADOR = "CRIADOR";
    public static final String MEMBRO = "MEMBRO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    private GrupoDesafio grupo;

    private LocalDate dataEntrada;
    private String papel;

    @Enumerated(EnumType.STRING)
    private StatusParticipacao status;

    protected Participacao() {}

    public Participacao(Usuario usuario, GrupoDesafio grupo, String papel) {
        this.usuario = usuario;
        this.grupo = grupo;
        this.papel = papel;
        this.dataEntrada = LocalDate.now();
        this.status = StatusParticipacao.ATIVA;
    }

    public Long getId() { return id; }
    public String getPapel() { return papel; }
}