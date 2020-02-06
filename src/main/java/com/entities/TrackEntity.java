package com.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(name = "tracks")
public class TrackEntity {
	private String album;
	private String artistName;
	private String mp3Link;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String name;
	private Integer position = 0;
	private String imageURL;
	private String songid;
	//id of user who added the song
	private Long userid;
	private int lengthSeconds;
}
