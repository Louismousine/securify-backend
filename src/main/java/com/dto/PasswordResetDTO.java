package com.dto;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class PasswordResetDTO {
	@ValidPassword(message = "{password.invalid}")
	private String password;
}
