package com.maf.exchangeapiv1.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientLiquidityException extends RuntimeException {
    public InsufficientLiquidityException(String message) {
        super(message);
    }
}
