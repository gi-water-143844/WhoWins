package com.adss.whowins.Model;

import jakarta.persistence.*;

@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeUsuario;
    private String email;

    protected Usuario() {}

    public Usuario(String nomeUsuario, String email) {
        this.nomeUsuario = nomeUsuario;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getNomeUsuario() { return nomeUsuario; }
    public String getEmail() { return email; }
}
