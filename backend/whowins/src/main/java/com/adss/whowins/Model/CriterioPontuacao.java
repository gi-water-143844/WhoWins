package com.adss.whowins.Model;

import jakarta.persistence.*;

@Entity
public class CriterioPontuacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String descricao;

    private String unidadeMedida; // preenchida quando a categoria é "Outro" (RN-001)

    protected CriterioPontuacao() {}

    public CriterioPontuacao(String descricao, String unidadeMedida) {
        this.descricao = descricao;
        this.unidadeMedida = unidadeMedida;
    }

    public Long getId() { return id; }
    public String getDescricao() { return descricao; }
    public String getUnidadeMedida() { return unidadeMedida; }
}
