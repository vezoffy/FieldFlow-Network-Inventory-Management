package com.training.authservice.exception;

import com.training.authservice.dto.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<MessageResponse> handleRoleNotFoundException(RoleNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse("Error: " + ex.getMessage()));
    }
}
