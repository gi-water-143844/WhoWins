package com.adss.whowins.Exception;

import java.util.Map;

public class DadosInvalidosException extends RuntimeException {

    private final Map<String, String> erros;

    public DadosInvalidosException(Map<String, String> erros) {
        super("Revise os campos informados.");
        this.erros = erros;
    }

    public Map<String, String> getErros() { return erros; }
}