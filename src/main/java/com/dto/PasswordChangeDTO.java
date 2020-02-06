package com.dto;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class PasswordChangeDTO {
	private String oldPW;
	@ValidPassword(message = "{password.invalid}")
	private String newPW;	
}
