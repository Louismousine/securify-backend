package com.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class UserRegisterDTO {
	@Email(message = "{email.invalid}")
	private String email;
	@NotEmpty(message="{username.notempty}")
	private String username;
	@ValidPassword(message = "{password.invalid}")
	private String password;
}
