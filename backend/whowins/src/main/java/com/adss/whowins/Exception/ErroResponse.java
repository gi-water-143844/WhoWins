package com.adss.whowins.Exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(int status, String mensagem, Map<String, String> erros) {}
