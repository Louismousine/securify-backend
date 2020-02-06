package com.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class KeywordDTO {
	@Autowired
	private MessageSource ms;
	
	@NotEmpty(message = "{keyword.notempty}")
	@NotNull(message = "{keyword.notempty}")
	private String keyword;
}
