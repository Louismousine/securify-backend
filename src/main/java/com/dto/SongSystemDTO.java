package com.dto;

import javax.validation.constraints.Min;

import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class SongSystemDTO {

	@Min(value = 0, message = "{songsystem.positive}")
	private int positionInPlaylist;
	@Min(value = 0, message = "{songsystem.positive}")
	private Integer secondsPlayed;

}
