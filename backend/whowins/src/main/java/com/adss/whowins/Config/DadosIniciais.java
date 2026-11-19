package com.adss.whowins.Config;

import com.adss.whowins.Model.Usuario;
import com.adss.whowins.Repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DadosIniciais {
    @Bean
    CommandLineRunner criarUsuarioDeTeste(UsuarioRepository repo) {
        return args -> repo.save(new Usuario("usuario_teste", "teste@whowins.com"));
    }
}