package com.finwise.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String accountName, BigDecimal balance, BigDecimal requested) {
        super(String.format("Account '%s' has insufficient balance (Available: ₹%s, Requested: ₹%s)",
                accountName, balance.toPlainString(), requested.toPlainString()));
    }
}
