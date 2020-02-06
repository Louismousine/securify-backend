package com.dto;

import com.entities.TrackEntity;

import lombok.Data;

@Data
public class GetSongDTO {

	private TrackEntity track;
	private int secondsPlayed;
}
