package com.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class SongIDDTO {

	@NotEmpty(message = "{songid.notempty")
	private String id;
}
