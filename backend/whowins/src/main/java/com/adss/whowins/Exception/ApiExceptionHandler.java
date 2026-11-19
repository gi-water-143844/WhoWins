package com.adss.whowins.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ErroResponse> tratarDadosInvalidos(DadosInvalidosException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(400, ex.getMessage(), ex.getErros()));
    }

    @ExceptionHandler(UsuarioNaoAutenticadoException.class)
    public ResponseEntity<ErroResponse> tratarNaoAutenticado(UsuarioNaoAutenticadoException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroResponse(401, ex.getMessage(), null));
    }
}
