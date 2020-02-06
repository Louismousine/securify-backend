package com.dto;

import java.util.List;

import com.entities.TrackEntity;

import lombok.Data;

@Data
public class PlaylistDTO {

	private List<TrackEntity> trackEntities;
	private int secondsPlayed;
	private int position;
}
