package com.services;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

//exceptions on registration: username/email in use
//maybe add constraints (length, email valid) later
@SuppressWarnings("serial")
@ResponseStatus(value = HttpStatus.CONFLICT)
public class RegisterException extends RuntimeException {
	public RegisterException(String msg) {
		super(msg);
	}
}
