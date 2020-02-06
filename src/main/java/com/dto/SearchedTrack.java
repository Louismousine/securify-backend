package com.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

//a track that has been searched but not added to the playlist, so not persisted
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
public class SearchedTrack {

	private String name;
	private String imageURL;
	private String album;
	private String artistName;
	private String id;
	@Min(value=0, message = "{songsystem.positive")
	private int lengthSeconds;
}
