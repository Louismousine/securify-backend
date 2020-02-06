package com.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "config")
public class PlaylistMetadataEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id = 0;
	private int positionInPlaylist = 0;	
	private int secondsPlayed;
	private int songsInPlaylist;
}
