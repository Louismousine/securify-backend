package com.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class UserLoginDTO {

	@NotEmpty(message = "{username.notempty}")
	private String username;
	@ValidPassword
	private String password;
}
