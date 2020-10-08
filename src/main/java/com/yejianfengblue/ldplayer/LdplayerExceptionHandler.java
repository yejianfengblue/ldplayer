package com.yejianfengblue.ldplayer;

import com.yejianfengblue.ldplayer.command.CommandExecutionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class LdplayerExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InterruptedException.class)
    protected ResponseEntity<Object> handleInterrupted(InterruptedException ex, WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(CommandExecutionFailureException.class)
    protected ResponseEntity<Object> handleCommandExecutionFailure(CommandExecutionFailureException ex, WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(LdplayerFailureException.class)
    protected ResponseEntity<Object> handleLdplayerFailure(LdplayerFailureException ex, WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
