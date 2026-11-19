package com.adss.whowins.Exception;

public class UsuarioNaoAutenticadoException extends RuntimeException {
    public UsuarioNaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
