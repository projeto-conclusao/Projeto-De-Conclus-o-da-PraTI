package com.achadosedevolvidos.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(AppException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(HttpStatus.UNAUTHORIZED.value(), "Credenciais inválidas"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(new ApiError(HttpStatus.BAD_REQUEST.value(), message));
    }

    /**
     * Sem isso, uma requisição para uma rota inexistente (nenhum @Controller nem
     * recurso estático correspondente) cai no handler genérico de Exception logo
     * abaixo e vira 500 — quando o correto é 404, exatamente o status que o Spring
     * já sinaliza ao lançar este tipo específico de exceção.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(HttpStatus.NOT_FOUND.value(), "Recurso não encontrado"));
    }

    /**
     * Rede de segurança: qualquer exceção não mapeada explicitamente acima cai
     * aqui em vez de virar um HTML de erro do servlet container. O detalhe da
     * causa vai para o log, não para a resposta — evita vazar stack trace/detalhes
     * internos para o cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Erro não tratado", ex);
        return ResponseEntity.internalServerError()
                .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno inesperado"));
    }
}
