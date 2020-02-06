package com.dto;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class UserEmailDTO {
	@Email(message = "{email.invalid}")
	@NotNull(message = "{email.notempty}")
	@NotEmpty(message = "{email.notempty}")
	private String email;
}
